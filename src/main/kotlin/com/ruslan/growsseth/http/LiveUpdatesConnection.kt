package com.ruslan.growsseth.http

import com.filloax.fxlib.api.json.ItemByNameSerializer
import com.filloax.fxlib.api.json.SimpleComponentSerializer
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.WebConfig
import com.ruslan.growsseth.dialogues.DialogueEntry
import com.ruslan.growsseth.entity.researcher.Researcher
import com.ruslan.growsseth.networking.CustomToastPacket
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketSendListener
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread


/**
 * Connect to an endpoint to be able to remotely trigger an
 * update to the EgoBalego API past the usual methods (which
 * in the livestreaming case, is at most every 10 minutes at
 * time of writing). This way, we can actively trigger an update
 * from remote.
 * Also allow additional remote commands:
 * - Playing researcher dialogues
 * - Remote command execution
 * Class lifetime should be same as the server, just in case.
 */
class LiveUpdatesConnection private constructor(val server: MinecraftServer) : ResponseSender {
    private var running = true
    private var socket: Socket? = null
    private var thread: Thread? = null
    // Socketio is non blocking, since this was initially
    // made for blocking sockets simulate that behavior
    private var asyncLock: Lock = ReentrantLock()

    companion object {
        const val RELOAD_EVENT = "reload"
        const val DIALOGUE_EVENT = "rdialogue"
        const val TOAST_EVENT = "toast"
        const val COMMAND_EVENT = RemoteCommandExec.PREFIX

        var retryTimeSeconds = 60
        val charset = Charsets.UTF_8
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private var activeConnection: LiveUpdatesConnection? = null

        fun serverStart(server: MinecraftServer) {
            if (activeConnection == null && WebConfig.liveUpdateService) {
                val conn = LiveUpdatesConnection(server)
                conn.start()
                activeConnection = conn
            }
            else if (!WebConfig.liveUpdateService)
                RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection was disabled from mod settings, will not start")
        }

        fun serverStop(server: MinecraftServer) {
            if (WebConfig.liveUpdateService) {
                activeConnection?.stop()
                activeConnection = null
            }
        }
    }

    /**
     * RUN ONLY INSIDE SECONDARY THREAD
     *
     * Attempts connecting every [retryTimeSeconds] seconds to the ip:port specified in config
     */
    private fun connect() {
        var interrupted = false
        while (running && socket == null && !interrupted) {
            val host = WebConfig.liveUpdateUrl
            val port = WebConfig.liveUpdatePort
            val uri = if (port > 0) URI("$host:$port") else URI(host)
            var success = false

            val connectCondition = asyncLock.newCondition()
            fun fulfillCondition() {
                asyncLock.lock();
                try {
                    connectCondition.signal();
                } finally {
                    asyncLock.unlock();
                }
            }

            try {
                RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Attempting connection to $uri...")

                val options = IO.Options.builder()
                    .setExtraHeaders(mapOf(
                        "apiKey" to listOf(WebConfig.dataSyncApiKey),
                    ))
                    .build()
                val newSocket = IO.socket(uri, options)

                newSocket.on(Socket.EVENT_CONNECT) {
                    RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Connected to $uri")
                    success = true
                    fulfillCondition()
                }
                // Set up an error listener
                newSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                    System.err.println("LiveUpdatesConnection | Connection error: " + args[0])
                    fulfillCondition()
                }

                newSocket.connect()

                asyncLock.lockInterruptibly();
                try {
                    connectCondition.await(60, TimeUnit.SECONDS);
                } catch (e: InterruptedException) {
                    RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | interrupted while connecting [A]")
                    interrupted = true
                } finally {
                    asyncLock.unlock();
                }

                if (success) {
                    socket = newSocket
                }
            } catch (e: InterruptedException) {
                RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | interrupted while connecting [B]")
                interrupted = true
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | failed in connection: " + e.message)
            }

            if (!success) {
                socket = null
                RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Retrying in ${retryTimeSeconds}s...")
                try {
                    Thread.sleep(retryTimeSeconds * 1000L)
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        }
        if (!running) {
            RuinsOfGrowsseth.LOGGER.info("Stopped LiveUpdatesConnection while connecting to remote")
        }
    }

    private fun sendOnSocket(message: String) {
        try {
            RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Sending message on socket: $message")
            socket?.emit(message) ?: run {
                RuinsOfGrowsseth.LOGGER.error("Couldn't send message $message: socket null")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun sendSuccess(extraItems: Map<String, Any>) {
        var msg = """{"status": "success""""
        extraItems.forEach { key, value ->
            msg += """, "$key": "$value" """
        }
        msg += "}"
        sendOnSocket(msg)
    }

    override fun sendFailure(reason: String?) {
        var msg = """{"status": "failure""""
        reason?.let {
            msg += """, "reason": "$it""""
        }
        msg += "}"
        sendOnSocket(msg)
    }

    // Type must have a json serializer!
    private inline fun <reified T> sendOnSocket(message: T) {
        sendOnSocket(json.encodeToString(message))
    }

    private fun onRefreshMessage() {
        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Refresh command received, updating data sync...")
        DataRemoteSync.doSync(WebConfig.dataSyncUrl, server).thenAccept { success ->
            if (success) {
                RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Data sync update success")
                sendSuccess()
            } else {
                RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Data sync update failure")
                sendFailure()
            }
        }
    }

    private fun onDialogueMessage(message: String) {
        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Received dialogue message $message")
        val dialogueEntry: DialogueEntry = try {
            json.decodeFromString(DialogueEntry.serializer(), message)
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Wrong dialogue format: ${e.message}")
            e.printStackTrace()
            sendFailure(e.message)
            return
        }

        val r = 100.0
        var c = 0
        server.allLevels.forEach { level ->
            level.players().forEach { player ->
                level.getEntitiesOfClass(Researcher::class.java, AABB.ofSize(player.position(), r, r, r)).forEach { researcher ->
                    if (researcher.dialogues?.nearbyPlayers()?.contains(player) == true) {
                        researcher.dialogues.triggerDialogueEntry(player, dialogueEntry)
                        c++
                    }
                }
            }
        }

        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Sent the dialogue successfully to $c players")
        sendSuccess(mapOf("amount" to c))
    }

    private fun onCommandMessage(message: String) {
        RemoteCommandExec.handleCommandMessage(message, server, this)
    }

    @Serializable
    private data class ToastData(
        @Serializable(with = SimpleComponentSerializer::class)
        val title: Component,
        @Serializable(with = SimpleComponentSerializer::class)
        val message: Component? = null,
        @Serializable(with = ItemByNameSerializer::class)
        val item: Item? = null,
    )

    private fun onNotificationMessage(message: String) {
        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Received toast message $message")
        val toastData: ToastData = try {
            json.decodeFromString(message)
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Wrong toast format: ${e.message}")
            e.printStackTrace()
            sendFailure(e.message)
            return
        }

        val packet = CustomToastPacket(toastData.title, toastData.message, toastData.item?.defaultInstance ?: ItemStack.EMPTY)
        server.playerList.players.forEach { player ->
            ServerPlayNetworking.getSender(player).sendPacket(packet, object : PacketSendListener {
                override fun onSuccess() = sendSuccess()
                override fun onFailure(): Packet<*>? {
                    sendFailure()
                    return null
                }
            })
        }
    }

    /**
     * RUN ONLY INSIDE SECONDARY THREAD
     *
     * The main loop, listen for triggers to re-run the data sync check.
     */
    private fun listen() {
        try {
            socket?.let { socket ->
                socket.on(RELOAD_EVENT) {
                    onRefreshMessage()
                }
                socket.on(DIALOGUE_EVENT) { args ->
                    onDialogueMessage(args[0].toString())
                }
                socket.on(TOAST_EVENT) { args ->
                    onNotificationMessage(args[0].toString())
                }
                socket.on(COMMAND_EVENT) { args ->
                    onCommandMessage(args[0].toString())
                }
            } ?: run {
                RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Socket is null!")
            }
            while (running && socket?.isActive == true) {
                if (!running) break // in case not running but socket returned

                // Probably not best way? When using normal sockets the main logic was here,
                // but since socketio is async just do this I guess
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
            RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | Interrupted")
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | Other error in listening: " + e.stackTraceToString())
        }

        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("LiveUpdatesConnection | error when closing connection: " + e.stackTraceToString())
        }
    }

    fun start() {
        thread = thread(start=true, name="LiveCheckEgobalego") {
            while (running) {
                connect()
                listen()
            }
            RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | stopped")
        }
    }

    fun stop() {
        RuinsOfGrowsseth.LOGGER.info("LiveUpdatesConnection | stopping thread...")
        // Interrupt thread if sleeping
        running = false
        try {
            thread?.interrupt() ?: RuinsOfGrowsseth.LOGGER.error("Thread is null!")
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("Error in interrupting the LiveUpdatesConnection thread: ${e.stackTraceToString()}")
        }
    }
}