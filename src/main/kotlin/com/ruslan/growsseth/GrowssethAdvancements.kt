package com.ruslan.growsseth

import com.mojang.datafixers.util.Either
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.structure.RemoteStructures
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
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.level.levelgen.structure.Structure
import java.lang.IllegalArgumentException
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashSet
import kotlin.jvm.optionals.getOrNull

object GrowssethAdvancements {
    @JvmStatic
    val all = HashSet<ResourceLocation>()

    @JvmField
    val TABS_WITH_SINGLE_BACKGROUND = mutableSetOf<String>(
        "growsseth",
    )

    // Defined via data or datagen

    val ROOT = make("root")
    val START = make("start")

    val FOR_STRUCTURES: Map<ResourceKey<Structure>, ResourceLocation> by lazy {
        GrowssethStructures.all.associateWith { StructureAdvancements.getStructureAdvancementId(it) }
    }

    fun make(name: String, folder: String = "growsseth"): ResourceLocation {
        val res = resLoc("$folder/" + name)
        all.add(res)
        return res
    }

    fun onServerTick(server: MinecraftServer) {
        // Check every 2 seconds for lag prevention
        if (server.tickCount % 40 == 0) {
            val spawningTent = RemoteStructures.STRUCTS_TO_SPAWN_BY_ID.values.any { it.structure == GrowssethStructures.RESEARCHER_TENT.location() }
            val advancement = server.advancements.get(START)
            if (advancement == null) {
                RuinsOfGrowsseth.LOGGER.warn("No $START advancement!")
                return
            }
            if (spawningTent)
                server.playerList.players.forEach { player ->
                    if (!player.advancements.getOrStartProgress(advancement).isDone)
                        player.advancements.award(advancement, "requirement")
                }
        }
    }
}

object StructureAdvancements {
    private val structToAdvancement = mutableMapOf<ResourceKey<Structure>, Advancement>()

    fun initServer(server: MinecraftServer) {

    }

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
        val advancement = player.server.advancements.get(getStructureAdvancementId(structKey)) ?:
            throw IllegalArgumentException("Unknown advancement key $structKey")
        return player.advancements.getOrStartProgress(advancement).isDone
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

    fun generateForStructureDetection(consumer: Consumer<AdvancementHolder>) {
        // Root dummy so that the one defined in datagen can be referred to
        val rootDummy = Advancement(
            Optional.of(GrowssethAdvancements.ROOT), Optional.empty(), AdvancementRewards.EMPTY,
            mapOf(), AdvancementRequirements.EMPTY, false
        )

        GrowssethStructures.all
            .minus(GrowssethStructures.RESEARCHER_TENT)
            .forEach { createStructureDetectionAdvancement(consumer, it, AdvancementHolder(GrowssethAdvancements.ROOT, rootDummy)) }
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

}