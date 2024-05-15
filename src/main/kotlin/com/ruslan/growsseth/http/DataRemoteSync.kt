package com.ruslan.growsseth.http

import com.filloax.fxlib.api.savedata.FxSavedData
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.WebConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

object DataRemoteSync {
    var lastSyncSuccessful = false
        private set

    private var tickUpdateRealTimeDistance: Duration =
        Duration.ofSeconds(((WebConfig.dataSyncReloadTime * 60).toLong()))   // how often the mod should query the server
        set(value) {
            if (value > Duration.ofSeconds(10)) {
                throw IllegalArgumentException("Duration too short, must be at least 10s: $value")
            }
            field = value
        }

    private val observersByEndpoint = mutableMapOf<String, MutableList<(String, MinecraftServer) -> Unit>>()
    //private val endpointLocks = mutableMapOf<String, Lock>() // Locks introduced risk of deadlocks and weren't necessary with a 1-threadpool
    private val endpointParams = mutableMapOf<String, EndpointParams>()
    private val gson = GsonBuilder().create()
    private val json = Json { ignoreUnknownKeys = true } // Kotlinx's serialization acts better with kotlin non-nullables etc
    private var requestsService: ExecutorService? = null
    private var didFirstLoad = mutableMapOf<String, Boolean>()
    private val doOnNextServerStart = LinkedBlockingQueue<(MinecraftServer) -> Unit>()
    private val logger = RuinsOfGrowsseth.LOGGER

    private var lastUpdateTime: LocalTime? = null

    fun <T: Any>subscribe(endpoint: String, serializer: DeserializationStrategy<T>, callback: (T, MinecraftServer) -> Unit) {
        subscribeRaw(endpoint) { response, server ->
            try {
                callback(json.decodeFromString(serializer, response), server)
            } catch (e: SerializationException) {
                logger.error("[$endpoint] JSON* PARSE FAILURE, IS $response", e)
            } catch (e: Exception) {
                logger.error("[$endpoint] OTHER FAILURE", e)
            }
        }
    }

    fun <T>subscribe(endpoint: String, type: Type, callback: (T, MinecraftServer) -> Unit) {
        subscribeRaw(endpoint) { response, server ->
            try {
                callback(gson.fromJson(response, type), server)
            } catch (e: JsonParseException) {
                logger.error("[$endpoint] JSON PARSE FAILURE, IS $response", e)
            } catch (e: Exception) {
                logger.error("[$endpoint] OTHER FAILURE", e)
            }
        }
    }

    fun subscribeRaw(endpoint: String, callback: (String, MinecraftServer) -> Unit) {
        val adjEndpoint = if (endpoint.startsWith("/")) {
            endpoint.replace(Regex("^/"), "")
        } else endpoint
        //endpointLocks.putIfAbsent(adjEndpoint, ReentrantLock())
        observersByEndpoint.computeIfAbsent(adjEndpoint) { mutableListOf() }.add(callback)
    }

    fun endpointParams(endpoint: String): EndpointParams {
        return endpointParams.computeIfAbsent(endpoint) {EndpointParams()}
    }

    /**
     * Run data sync on the specified endpoint
     * @return A completable future that completes when all endpoints do, and is true if all endpoints had a success
     */
    fun doSync(url: String, server: MinecraftServer): CompletableFuture<Boolean> {
        if (!WebConfig.webDataSync) {
            return CompletableFuture.completedFuture(false)
        }

        if (url.isBlank()) {
            RuinsOfGrowsseth.LOGGER.warn("Data sync url is empty, won't run")
            return CompletableFuture.completedFuture(false)
        }

        lastUpdateTime = LocalTime.now()
        val future = CompletableFuture<Boolean>()
        val successes = mutableMapOf<String, Boolean>()
        observersByEndpoint.forEach { (endpoint, callbacks) ->
            syncEndpoint(url, endpoint, callbacks, server).thenAccept ta@{ success ->
                successes[endpoint] = success
                if (successes.keys.size >= observersByEndpoint.keys.size) {
                    future.complete(successes.values.all{it})
                }
            }
        }
        return future
    }

    fun checkTickSync(url: String, server: MinecraftServer) {
        if (WebConfig.webDataSync) {
            val time = LocalTime.now()
            // check real time to make pause not affect it
            if (lastUpdateTime != null && Duration.between(lastUpdateTime, time) >= tickUpdateRealTimeDistance) {
                logger.info("Data sync: started periodic sync")
                doSync(url, server)
            }
        }
    }

    fun handleServerAboutToStartEvent(server: MinecraftServer) {
        if (WebConfig.webDataSync)
            setupExecutorService()
        else
            logger.info("Web data synchronization was disabled from settings, will not start")
    }

    fun handleWorldLoaded(server: MinecraftServer, level: ServerLevel) {
        if (WebConfig.webDataSync && level.dimension() == Level.OVERWORLD) {
            while (doOnNextServerStart.isNotEmpty()) {
                doOnNextServerStart.poll()(server)
            }
        }
    }

    fun handleServerStoppingEvent() {
        shutdownExecutorService()
    }

    private fun syncEndpoint(url: String, endpoint: String, callbacks: List<(String, MinecraftServer) -> Unit>, server: MinecraftServer): CompletableFuture<Boolean> {
        val params = endpointParams[endpoint] ?: DEFAULT_PARAMS
        val fullUrl = "$url/$endpoint"
        val conn = makeConnection(fullUrl, params)
        //val lock = endpointLocks[endpoint]!!
        //lock.lock()
        val future = CompletableFuture<Boolean>()
        sendRequest(conn).whenComplete { conn2, exception ->
            try {
                // Exception happened
                val result = if (exception != null) {
                    if (endpoint !in didFirstLoad) {
                        logger.info("[$endpoint] Restoring from server memory after connection error as didn't load the first time yet")
                        restoreEndpointFromMemory(server, endpoint).thenAccept ta@{ savedData ->
                            if (savedData == null) {
                                logger.warn("[$endpoint] No data for sync in server memory!")
                                return@ta
                            }
                            callbacks.forEach { it(savedData, server) }
                            didFirstLoad[endpoint] = true
                            logger.info("[$endpoint] Restore successful")
                        }
                    }
                    logger.error("[$endpoint] ERROR: ${exception.message}")
                    false

                    // Everything went well
                } else {
                    val status = conn2.responseCode
                    if (status < 300 && server.isRunning) {
                        didFirstLoad[endpoint] = true
                        val content = getResponseContent(conn2)
                        saveEndpointToMemory(server, endpoint, content)
                        logger.info("[$endpoint] SUCCESS, STATUS: $status")
                        callbacks.forEach { it(content, server) }
                        true
                    } else if (!server.isRunning) {
                        logger.error("Data sync $endpoint: server not running, abort...")
                        false
                    } else {
                        logger.error("[$endpoint] ERROR, STATUS $status\n${getResponseContent(conn2)}")

                        if (endpoint !in didFirstLoad) {
                            logger.info("[$endpoint] Restoring from server memory after error as didn't load the first time yet")
                            restoreEndpointFromMemory(server, endpoint).thenAccept ta@{ savedData ->
                                if (savedData == null) {
                                    logger.warn("[$endpoint] No data for sync in server memory!")
                                    return@ta
                                }
                                callbacks.forEach { it(savedData, server) }
                                didFirstLoad[endpoint] = true
                                logger.info("[$endpoint] Restore successful")
                            }
                        }
                        false
                    }
                }

                future.complete(result)
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            } finally {
                //lock.unlock()
            }
        }
        return future
    }

    private fun makeConnection(url: String, params: EndpointParams = DEFAULT_PARAMS): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept-Charset", "UTF-8")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        params.headers.forEach {  conn.setRequestProperty(it.key, it.value) }

        return conn
    }

    private fun sendRequest(conn: HttpURLConnection): CompletableFuture<HttpURLConnection> {
        val completableFuture = CompletableFuture<HttpURLConnection>()
        requestsService?.submit {
            logger.info("DATA SYNC: CONNECTING VIA ${conn.url}")
            try {
                conn.connect()
                completableFuture.complete(conn)
                conn.disconnect()
                logger.info("DATA SYNC: DISCONNECTED FROM ${conn.url}")
            } catch(e: Exception) {
                logger.error("DATA SYNC: FAILURE WITH ${conn.url}", e.message)
                completableFuture.completeExceptionally(e)
            }
        } ?: {
            completableFuture.completeExceptionally(IllegalStateException("Executor service not setup!"))
        }
        return completableFuture
    }

    private fun getResponseContent(conn: HttpURLConnection): String {
        val respReader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        var inputLine: String?
        val contentBuffer = StringBuffer()
        while (respReader.readLine().also { inputLine = it } != null) {
            contentBuffer.append(inputLine)
        }
        respReader.close()
        return contentBuffer.toString()
    }

    private fun setupExecutorService() {
        shutdownExecutorService()
        val threads = 1
        requestsService = Executors.newFixedThreadPool(
            threads,
            object : ThreadFactory {
                private val poolNum = AtomicInteger(1)
                private val threadNum = AtomicInteger(1)
                private val namePrefix = "growsseth-" + poolNum.getAndIncrement() + "-thread-requests"
                override fun newThread(r: Runnable): Thread {
                    return Thread(null, r, namePrefix + threadNum.getAndIncrement())
                }
            }
        )
    }

    private fun shutdownExecutorService() {
        requestsService?.shutdown()
    }

    private fun saveEndpointToMemory(server: MinecraftServer, endpoint: String, response: String) {
        val overworld = getOverworldOrNull(server)
        if (overworld != null) {
            val savedData = DataSyncMemorySavedData.get(overworld)
            savedData.lastEndpointOutputs[endpoint] = response
            savedData.setDirty()
            logger.info("Updated data sync save data")
        } else if (server.isRunning) {
            doOnNextServerStart.offer {
                saveEndpointToMemory(server, endpoint, response)
            }
        }
    }

    private fun restoreEndpointFromMemory(server: MinecraftServer, endpoint: String, existingFuture: CompletableFuture<String?>? = null): CompletableFuture<String?> {
        val future = existingFuture ?: CompletableFuture<String?>()

        if (!server.isRunning) {
            // abort
            future.completeExceptionally(IllegalStateException("Restore endpoint abort: Server not running anymore"))
            return future
        }

        val overworld = getOverworldOrNull(server)
        if (overworld != null) {
            future.complete(DataSyncMemorySavedData.get(overworld).lastEndpointOutputs[endpoint])
        } else {
            doOnNextServerStart.offer {
                restoreEndpointFromMemory(server, endpoint, future)
            }
        }

        return future
    }

    data class EndpointParams(
        val headers: MutableMap<String, String> = mutableMapOf(),
    )

    private fun getOverworldOrNull(server: MinecraftServer): ServerLevel? {
        return try {
            // Java checks for this to not be null, for some reason?
            server.overworld()
        } catch (e: NullPointerException) {
            null
        }
    }

    val DEFAULT_PARAMS = EndpointParams()
}

inline fun <reified T> genericType(): Type = object: TypeToken<T>() {}.type

class DataSyncMemorySavedData private constructor (
    lastEndpointOutputs: Map<String, String> = mapOf()
) : FxSavedData<DataSyncMemorySavedData>(CODEC) {
    val lastEndpointOutputs: MutableMap<String, String> = lastEndpointOutputs.toMutableMap()

    companion object {
        val CODEC: Codec<DataSyncMemorySavedData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("lastEndpointOutputs").forGetter(DataSyncMemorySavedData::lastEndpointOutputs)
        ).apply(builder, ::DataSyncMemorySavedData) }
        private val DEF = define("growsseth_datasync_memory", ::DataSyncMemorySavedData, CODEC)

        @JvmStatic
        fun get(level: ServerLevel): DataSyncMemorySavedData {
            return level.loadData(DEF)
        }
    }
}