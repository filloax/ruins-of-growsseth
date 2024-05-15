package com.ruslan.growsseth.http

import com.filloax.fxlib.api.getStructTagOrKey
import com.filloax.fxlib.api.json.RotationSerializer
import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.Constants
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

object GrowssethApiV1 : AbstractGrowssethApi() {
    override val structureSpawns: List<ApiStructureSpawn>
        get() = structSpawnsList
    override val quests: List<ApiQuestData>
        get() = questsList
    override val events: List<ApiEvent>
        get() = eventsList

    private val structSpawnsList = mutableListOf<ApiStructureSpawn>()
    private val questsList = mutableListOf<ApiQuestData>()
    private val eventsList = mutableListOf<ApiEvent>()
    // Keep map separately, so there is still a fallback in case of duplicate ids
    private val structSpawnsByName = mutableMapOf<String, ApiStructureSpawn>()
    private val eventsByName = mutableMapOf<String, ApiEvent>()
    private var contentsHash: Int? = null

    override fun structByName(name: String): ApiStructureSpawn? = structSpawnsByName[name]

    override fun eventByName(name: String): ApiEvent? = eventsByName[name]
    override fun isEventActive(name: String): Boolean = eventsByName[name]?.active == true

    override fun init() {
        DataRemoteSync.endpointParams(WebConfig.dataSyncEndpoint).headers["apiKey"] = WebConfig.dataSyncApiKey

        DataRemoteSync.subscribe(WebConfig.dataSyncEndpoint, ListSerializer(Entry.serializer())) { list, server ->
            val (events, structSpawns) = list.partition { decomposeId(it.structureId).first == Constants.EVENT_NAMESPACE }

            structSpawnsList.clear()
            structSpawnsByName.clear()
            structSpawnsList.addAll(structSpawns)
            structSpawnsByName.putAll(structSpawns.associateBy(Entry::name))

            questsList.clear()
            questsList.addAll(list.mapNotNull { it.quest })

            eventsList.clear()
            eventsByName.clear()
            eventsList.addAll(events.map(Entry::toEvent))
            eventsByName.putAll(eventsList.associateBy(ApiEvent::name))

            val hash = Objects.hash(structSpawnsList, eventsList, questsList)
            val changed = hash != contentsHash

            if (changed) {
                RuinsOfGrowsseth.LOGGER.info("API v1 data updated!\n\tQuests: $questsList}"
                        + "\n\tStructures: $structSpawnsList"
                        + "\n\tEvents: $eventsList"
                )

                structSpawnsList.groupBy { it.name }.filter { it.value.size > 1 }.let {
                    if (it.isNotEmpty())
                        RuinsOfGrowsseth.LOGGER.warn("More than 1 structure spawn with the same name! (Check list above) $it")
                }

                triggerSubscriberUpdates(server)
                contentsHash = hash
            } else {
                RuinsOfGrowsseth.LOGGER.info("API data not changed, not triggering subscribers (hash = $hash)")
            }
        }
    }

    object Callbacks {
        // Remove data on server stop, no need to persist it
        // to achieve this behavior as it would need to be made serializable too then,
        // we only need for data to not be shared across different world loads etc
        fun onServerStop(server: MinecraftServer) {
            structSpawnsList.clear()
            questsList.clear()
            eventsList.clear()
            structSpawnsByName.clear()
            eventsByName.clear()
            contentsHash = null
        }
    }

    /**
     * Decompose a string into location id and path, like ResourceLocation but
     * without the format checking (to have more flexibility)
     */
    private fun decomposeId(location: String, separator: Char = ':'): Pair<String, String> {
        val strings = arrayOf("minecraft", location)
        val i = location.indexOf(separator)
        if (i >= 0) {
            strings[1] = location.substring(i + 1)
            if (i >= 1) {
                strings[0] = location.substring(0, i)
            }
        }
        return Pair(strings[0].trim(), strings[1].trim())
    }

    /**
    {
    "id":1,
    "structureID":"prova1",
    "name":"Struttura 1",
    "x":1,
    "y":2,
    "z":3,
    "active":false,
    "quest":{
    "id":2,
    "unlocked":false,
    "solved":false,
    "name":"La ricerca dell'Arcano",
    "questIMGUnlocked":"./assets/quests/enchanting.webp",
    "questIMGLocked":"./assets/quests/quest_enchanting.webp",
    "text":"Un potere grande questo mondo cela, \nmolti lo bramano, \nma nessuno lo anela.\n\nI figli piu' rari della terra dovrai trovare, \nattorno ad una piramide li dovrai posizionare.\n\nUna base comune sara' inefficace, \nma con la forza dell'altro mondo sarai meno fallace.\n\nInfine una piccola aggiunta e' richiesta,\nUn raccoglitore di sapere per una ricetta mesta.\n\nSolo infine l'arcano si rivelera',\nSarai in grado di usarlo, \n...o ti sopraffara'?",
    "difficulty":"HARD"
    }
    }
     */

    @Serializable
    data class Entry(
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

        fun toEvent(): ApiEvent = Event(
            decomposeId(structureId).second,
            active,
            name,
            startPos,
            rotation
        )
    }

    data class Event(
        override val name: String,
        override val active: Boolean = false,
        override val desc: String? = null,
        override val pos: BlockPos? = null,
        override val rotation: Rotation? = null,
    ) : ApiEvent {
        override fun toString(): String {
            val d = desc?.let { " $it" } ?: ""
            val p = pos?.let{ " @$it" } ?: ""
            return "$name(${if (active) "X" else ""}$d$p)"
        }
    }
}