package com.ruslan.growsseth.http

import com.filloax.fxlib.api.FxUtils
import com.filloax.fxlib.api.getStructTagOrKey
import com.filloax.fxlib.api.json.RotationSerializer
import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.WebConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.*
import java.util.concurrent.CompletableFuture


object GrowssethApiV2 : AbstractGrowssethApi() {
    override val structureSpawns: List<ApiStructureSpawn>
        get() = structSpawnsList
    override val events: List<ApiEvent>
        get() = eventsList
    override val quests: List<ApiQuestData>
        get() = questsList

    private val structSpawnsList = mutableListOf<ApiStructureSpawn>()
    private val eventsList = mutableListOf<ApiEvent>()
    private val questsList = mutableListOf<ApiQuestData>()
    // Keep map separately, so there is still a fallback in case of duplicate ids
    private val structSpawnsByName = mutableMapOf<String, ApiStructureSpawn>()
    private val eventsByName = mutableMapOf<String, ApiEvent>()
    private var contentsHash: Int? = null

    override fun structByName(name: String): ApiStructureSpawn? = structSpawnsByName[name]
    override fun eventByName(name: String): ApiEvent? = eventsByName[name]
    override fun isEventActive(name: String): Boolean = eventsByName[name]?.active == true

    private val EVENT_TYPES = listOf(
        "questStep",
        "tradePreset",
        "tradeCustom",
        "dialogue",
        "toast",
        "researcherDiary",
        "structureBook",
        "command",
        "operation",
    )

    override fun init() {
        DataRemoteSync.endpointParams(WebConfig.dataSyncEndpoint).headers["apiKey"] = WebConfig.dataSyncApiKey

        DataRemoteSync.subscribe(WebConfig.dataSyncEndpoint, ListSerializer(Entry.serializer())) { list, server ->
            val validEntries = list.filter(Entry::checkValid)
            val (events, structSpawns) = validEntries.partition { it.type in EVENT_TYPES }

            structSpawnsList.clear()
            structSpawnsByName.clear()
            structSpawnsList.addAll(structSpawns.map(Entry::toStructureSpawn))
            structSpawnsByName.putAll(structSpawns.map(Entry::toStructureSpawn).associateBy(ApiStructureSpawn::name))

            eventsList.clear()
            eventsByName.clear()
            eventsList.addAll(events.map(Entry::toEvent))
            eventsByName.putAll(eventsList.associateBy(ApiEvent::name))

            questsList.clear()
            questsList.addAll(list.mapNotNull { it.quest })

            val hash = Objects.hash(structSpawnsList, eventsList, questsList)
            val changed = hash != contentsHash

            if (changed) {
                RuinsOfGrowsseth.LOGGER.info("API v2 data updated!"
                        + "\n\tStructures: $structSpawnsList"
                        + "\n\tEvents: $eventsList"
                )

                structSpawnsList.groupBy { it.name }.filter { it.value.size > 1 }.let {
                    if (it.isNotEmpty())
                        RuinsOfGrowsseth.LOGGER.warn("More than 1 structure spawn with the same name! (Check list above) $it")
                }

                triggerSubscriberUpdates(server)
                contentsHash = hash
            }
            else {
                RuinsOfGrowsseth.LOGGER.info("API data not changed, not triggering subscribers (hash = $hash)")
            }
        }
    }

    override fun reload(): CompletableFuture<Boolean> {
        val server = FxUtils.getServer()
        if (server == null) {
            RuinsOfGrowsseth.LOGGER.error("Server is not running, cannot reload!")
            return CompletableFuture.completedFuture(false)
        }
        return DataRemoteSync.doSync(WebConfig.dataSyncUrl, server)
    }

    object Callbacks {
        // Remove data on server stop, no need to persist it to achieve this behavior
        // as it would need to be made serializable too then, we only need for data
        // to not be shared across different world loads etc
        fun onServerStop(server: MinecraftServer) {
            structSpawnsList.clear()
            questsList.clear()
            eventsList.clear()
            structSpawnsByName.clear()
            eventsByName.clear()
            contentsHash = null
        }
    }


    @Serializable
    data class Entry (
        val id: String = "",
        val type: String = "",
        val active: Boolean = false,
        val structure: String? = null,
        @Serializable(with = RotationSerializer::class)
        val rotation: Rotation? = null,
        val x: Int? = null,
        val y: Int? = null,
        val z: Int? = null,
        var name: String? = null,
        val content: String? = null,
        val icon: String? = null,
        val title: String? = null,
        val quest: Quest? = null,
    ) {
        fun checkValid(): Boolean {
            if (id == "") {
                RuinsOfGrowsseth.LOGGER.error("There are some entries with no id in web sync data!")
                return false
            }
            if (type != "structure" && type !in EVENT_TYPES) {
                RuinsOfGrowsseth.LOGGER.error("Web sync entry with id $id has type $type, which is not supported!")
                return false
            }
            val missingFields = mutableListOf<String>()
            when (type) {
                "structure" -> {
                    if (structure == null)
                        missingFields.add(::structure.name)
                    if (x == null)
                        missingFields.add(::x.name)
                    if (y == null)
                        missingFields.add(::y.name)
                    if (z == null)
                        missingFields.add(::z.name)
                }
                "questStep", "tradePreset", "operation" -> {
                    if (name == null)
                        missingFields.add(::name.name)
                }
                "tradeCustom", "dialogue", "command" -> {
                    if (content == null)
                        missingFields.add(::content.name)
                }
                "toast", "researcherDiary" -> {
                    if (title == null)
                        missingFields.add(::title.name)
                    if (content == null)
                        missingFields.add(::content.name)
                }
                "structureBook" -> {
                    if (structure == null)
                        missingFields.add(::structure.name)
                    if (content == null)
                        missingFields.add(::content.name)
                }
            }
            if (missingFields.size > 0) {
                RuinsOfGrowsseth.LOGGER.error(
                    "Web sync entry with id '$id' has type '$type', but is missing the following keys: $missingFields"
                )
                return false
            }
            return true
        }

        fun toStructureSpawn(): StructureSpawn = StructureSpawn (
            structureID = structure!!,
            name = id,
            x = x!!,
            y = y!!,
            z = z!!,
            active = active,
            rotation = rotation,
            quest = quest
        )

        fun toEvent(): Event {
            var eventName = ""
            var eventDesc = ""
            var eventPos = BlockPos(0,0,0)
            when (type) {
                "questStep" ->
                    eventName = name!!
                "tradePreset" ->
                    eventName = name!!
                "tradeCustom" -> {
                    eventName = "customTrade/$id"
                    eventDesc = content!!
                }
                "dialogue" -> {
                    eventName = "rdialogue/$id"
                    eventDesc = content!!
                }
                "toast" -> {
                    eventName = if (icon == null)
                        "toast/$title"
                    else
                        "toast/" + icon.replace(":", "/") + "/" + title
                    eventDesc = content!!
                }
                "researcherDiary" -> {
                    eventName = if (structure == null) {
                        if (title!!.endsWith("/endDiary"))
                            "enddiary/$title!!"
                        else
                            "rdiary/$title!!"
                    }
                    else
                        "rdiary/$structure/$title!!"
                    eventDesc = content!!
                }
                "structureBook" -> {
                    eventName = "structbook/$structure!!"
                    eventDesc = content!!
                }
                "command" -> {
                    eventName = "cmd/$id"
                    eventDesc = content!!
                }
                "operation" -> {
                    eventName = name!!
                    eventDesc = id
                    if (x != null && y != null && z != null)
                        eventPos = BlockPos(x, y, z)
                }
            }
            return Event (
                name = eventName,
                active = active,
                desc = eventDesc,
                pos = eventPos,
                rotation = rotation
            )
        }
    }

    @Serializable
    data class StructureSpawn (
        private val structureID: String = "",
        override val name: String = "",
        private val x: Int = Int.MAX_VALUE,
        private val y: Int = Int.MAX_VALUE,
        private val z: Int = Int.MAX_VALUE,
        override val active: Boolean = false,
        @Serializable(with = RotationSerializer::class)
        override val rotation: Rotation? = null,
        val quest: Quest? = null,
    ) : ApiStructureSpawn {
        init {
            checkValid()
        }

        override val structureId: String
            get() = structureID
        override val startPos: BlockPos
            get() = BlockPos(x, y, z)

        override fun structureKey(): Either<TagKey<Structure>, ResourceKey<Structure>> = getStructTagOrKey(structureID)

        override fun toString(): String {
            val rotString = rotation?.let {
                " " + when(it) {
                    Rotation.NONE -> 0
                    Rotation.CLOCKWISE_90 -> 90
                    Rotation.COUNTERCLOCKWISE_90 -> -90
                    Rotation.CLOCKWISE_180 -> 180
                } + "°"
            }
            return "$name(${if (active) "X" else ""}($structureID @$startPos$rotString)"
        }

        private fun checkValid() {
        }
    }


    data class Event(
        override val name: String,
        override val active: Boolean = false,
        override val desc: String? = null,
        override val pos: BlockPos? = BlockPos(Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE),
        override val rotation: Rotation? = null,
    ) : ApiEvent {
        override fun toString(): String {
            val d = desc?.let { " $it" } ?: ""
            val p = pos.let{ " @$it" } ?: ""
            return "$name(${if (active) "X" else ""}$d$p)"
        }
    }


    // unused (for now)
    @Serializable
    data class Quest (
        override val unlocked: Boolean = false,
        override val solved: Boolean = false,
        override val name: String = "",
        val questIMGUnlocked: String = "",
        val questIMGLocked: String = "",
        override val text: String = "",
        override val difficulty: String = "",
    ) : ApiQuestData {
        override val imgUnlocked: String
            get() = questIMGUnlocked
        override val imgLocked: String
            get() = questIMGLocked

        override fun toString(): String {
            return "$name(${if (unlocked) "U" else ""}${if (solved) "S" else ""})"
        }
    }
}