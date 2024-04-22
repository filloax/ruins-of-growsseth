package com.ruslan.growsseth.advancements

import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.ModEvents
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.criterion.JigsawPiecePredicate
import com.ruslan.growsseth.advancements.criterion.JigsawPieceTrigger
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.AdvancementRequirements
import net.minecraft.advancements.AdvancementRewards
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.advancements.critereon.PlayerTrigger
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.optionals.getOrNull


object StructureAdvancements {
    // Structures that are also considered "found" if a village house is found
    private val villageHouseStructures = mapOf(
        GrowssethStructures.GOLEM_HOUSE to getHousesOfVillageCategory(VillageBuildings.CATEGORY_GOLEM_HOUSE)
    )

    // To use with getStructTagOrKey (in utils)
    fun playerHasFoundStructure(player: ServerPlayer, structId: Either<TagKey<Structure>, ResourceKey<Structure>>, failHard: Boolean = false): Boolean {
        // if not failHard, if the structure isn't registered on the mod's system just always return false
        // We don't always need this information to be precise, so not erroring might be more priority
        return try {
            structId.map({ // tag version
                playerHasFoundStructure(player, it)
            }, { // id version
                playerHasFoundStructure(player, it)
            })
        } catch (e: IllegalArgumentException) {
            if (failHard) {
                throw e
            } else {
                false
            }
        }
    }


    fun playerHasFoundStructure(player: ServerPlayer, structKey: ResourceKey<Structure>): Boolean {
        val advancements = listOfNotNull(
            player.server.advancements.get(getStructureAdvancementId(structKey)),
        ) + villageHouseStructures.mapNotNull { player.server.advancements.get(getStructureJigsawAdvancementId(it.key, structKey)) }
        if (advancements.isEmpty()) {
            throw IllegalArgumentException("Unknown advancement key $structKey")
        }

        return advancements.any { player.advancements.getOrStartProgress(it).isDone }
    }

    fun playerHasFoundStructure(player: ServerPlayer, structTag: TagKey<Structure>): Boolean {
        val registries = player.serverLevel().registryAccess()
        val tagHolders = registries.registryOrThrow(Registries.STRUCTURE).getTag(structTag)?.getOrNull()
        if (tagHolders == null) {
            RuinsOfGrowsseth.LOGGER.warn("Structure tag $structTag doesn't exist")
            return false
        }
        return tagHolders.any {
            playerHasFoundStructure(player, it.unwrap().orThrow())
        }
    }

    fun getPlayerFoundStructures(player: ServerPlayer): Set<ResourceKey<Structure>> {
        return GrowssethStructures.all.filter {
            val advancement = player.server.advancements.get(getStructureAdvancementId(it))
            if (advancement != null) {
                player.advancements.getOrStartProgress(advancement).isDone
            } else {
                false
            }
        }.toSet()
    }

    fun getStructureAdvancementId(structKey: ResourceKey<Structure>): ResourceLocation {
        return resLoc("growsseth/found_${structKey.location().path}")
    }

    fun getStructureJigsawAdvancementId(structKey: ResourceKey<Structure>, target: ResourceKey<Structure>): ResourceLocation {
        return resLoc("growsseth/found_jigsaw/${structKey.location().namespace}/${structKey.location().path}/${target.location().path}")
    }

    private val structureKeyAdvancementRegex = Regex("^growsseth/found_(.+)$")
    private val structureJigsawPieceAdvancementRegex = Regex("^growsseth/found_jigsaw/([^/]+)/([^/]+)/(.+)$")

    fun getStructureKeyFromAdvancementId(id: ResourceLocation): ResourceKey<Structure>? {
        if (id.namespace != RuinsOfGrowsseth.MOD_ID) return null

        return structureKeyAdvancementRegex.matchEntire(id.path)?.groupValues?.get(1)?.let { ResourceKey.create(Registries.STRUCTURE, resLoc(it)) }
    }

    fun getStructureJigsawPieceIdFromAdvancementId(id: ResourceLocation): Pair<ResourceKey<Structure>, ResourceLocation>? {
        if (id.namespace != RuinsOfGrowsseth.MOD_ID) return null

        val match = structureJigsawPieceAdvancementRegex.matchEntire(id.path)
        return match?.let { m -> Pair(
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation(m.groupValues[1], m.groupValues[2])),
            resLoc(m.groupValues[3]),
        ) }
    }


    fun generateForStructureDetection(consumer: Consumer<AdvancementHolder>) {
        // Root dummy so that the one defined in datagen can be referred to
        val rootDummy = Advancement(
            Optional.of(GrowssethAdvancements.ROOT), Optional.empty(), AdvancementRewards.EMPTY,
            mapOf(), AdvancementRequirements.EMPTY, false
        )
        val rootHolder = AdvancementHolder(GrowssethAdvancements.ROOT, rootDummy)

        GrowssethStructures.all
            .minus(GrowssethStructures.RESEARCHER_TENT)
            .forEach { createStructureDetectionAdvancement(consumer, it, rootHolder) }

        villageHouseStructures.forEach { (struct, villageHouses) ->
            villageHouses.forEach { (villageStructId, housePaths) ->
                val name = getStructureJigsawAdvancementId(villageStructId, struct)
                createJigsawDetectionAdvancement(consumer, villageStructId, housePaths, rootHolder, name.toString())
            }
        }
    }

    private fun createStructureDetectionAdvancement(consumer: Consumer<AdvancementHolder>, structKey: ResourceKey<Structure>, root: AdvancementHolder): AdvancementHolder {
        return Advancement.Builder.advancement()
            /* not needed if it's not going to be displayed
            .display(
                Items.FILLED_MAP,  // The display icon
                Component.literal(structKey.location().path),  // The title
                Component.literal("hidden adv for structure detection (${structKey.location()})"),  // The description
                null,  // Background image used
                FrameType.TASK,  // Options: TASK, CHALLENGE, GOAL
                false,  // Show toast top right
                false,  // Announce to chat
                true // Hidden in the advancement tab
            )
             */
            .parent(root)
            .addCriterion("in_structure", PlayerTrigger.TriggerInstance.located(LocationPredicate.Builder.inStructure(structKey)) )
            .save(consumer, getStructureAdvancementId(structKey).toString())
    }

    private fun createJigsawDetectionAdvancement(
        consumer: Consumer<AdvancementHolder>,
        structKey: ResourceKey<Structure>,
        pieceIds: List<ResourceLocation>,
        root: AdvancementHolder,
        name: String = pieceIds.joinToString("_") { it.path.replace("/", "_") },
    ): AdvancementHolder {
        return Advancement.Builder.advancement()
            .parent(root)
            .addCriterion("in_structure_piece", GrowssethCriterions.JIGSAW_PIECE.createCriterion(JigsawPieceTrigger.Instance(
                Optional.empty(),
                Optional.of(JigsawPiecePredicate(
                    structKey,
                    pieceIds,
                )),
            )))
            .save(consumer, name)
    }

    private fun getHousesOfVillageCategory(category: String): Map<ResourceKey<Structure>, List<ResourceLocation>> {
        val entries = VillageBuildings.houseEntries[category] ?: throw IllegalArgumentException("Village category $category not found")
        return entries
            .groupBy{ it.kind }
            .mapKeys { ResourceKey.create(Registries.STRUCTURE, ResourceLocation("minecraft", "village_${it.key}")) }
            .mapValues { e -> e.value.flatMap { listOf(it.normal, it.zombie) } }
    }

    object Callbacks {
        fun onAdvancement(player: ServerPlayer, advancement: AdvancementHolder, criterionString: String) {
            val (structureId, isJigsaw) = getStructureKeyFromAdvancementId(advancement.id)?.let {
                Pair(it, false)
            } ?: getStructureJigsawPieceIdFromAdvancementId(advancement.id)?.let { (structId, pieceId) ->
                for ((targetStructId, structureHouses) in villageHouseStructures.entries) {
                    if (structureHouses[structId]?.contains(pieceId) == true) {
                        return@let Pair(targetStructId, true)
                    }
                }
                return
            } ?: return

            ModEvents.get().triggerOnStructureFound(player, structureId, isJigsaw)
        }
    }
}