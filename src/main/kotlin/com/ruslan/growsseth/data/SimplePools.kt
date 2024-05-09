package com.ruslan.growsseth.data

import com.mojang.datafixers.util.Either
import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.structure.GrProcessorLists
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.structure.VillageBuildings.CATEGORY_GOLEM_HOUSE
import com.ruslan.growsseth.structure.VillageBuildings.CATEGORY_GOLEM_STREET
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.data.worldgen.Pools
import net.minecraft.data.worldgen.ProcessorLists
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList

class SimplePools(private val context: BootstrapContext<StructureTemplatePool>) {
    private val processorGetter: HolderGetter<StructureProcessorList> = context.lookup(Registries.PROCESSOR_LIST)
    private val templatePoolGetter: HolderGetter<StructureTemplatePool> = context.lookup(Registries.TEMPLATE_POOL)

    private val emptyPool = templatePoolGetter.getOrThrow(Pools.EMPTY)
    private val emptyProcessor = processorGetter.getOrThrow(ProcessorLists.EMPTY)

    companion object {
        fun bootstrap(ctx: BootstrapContext<StructureTemplatePool>) = SimplePools(ctx).bootstrap()
    }

    fun bootstrap() {
        bootstrapBeekeperHouse()
        bootstrapConduitChurch()
        bootstrapEnchantTower()
        bootstrapNoteblockLab()
        bootstrapAbandonedForge()
        bootstrapMinorRuins()
        bootstrapMisc()
        bootstrapStandaloneVillageHouses()
    }

    private fun bootstrapBeekeperHouse() {
        registerSimplePoolElementsWithCydonia(
            "ruins/beekeeper_house/beenest",
            "ruins/beekeeper_house/house",
            "ruins/beekeeper_house/zombie",
        )
        registerSimplePoolElements(
            "ruins/beekeeper_house/beehive",
            "cydonia/ruins/beekeeper_house/beehive_off",
            "cydonia/ruins/beekeeper_house/beehive_on",
            "cydonia/ruins/beekeeper_house/beenest_campfire",
        )
    }

    private fun bootstrapConduitChurch() {
        registerSimplePoolElementsWithCydonia(
            "ruins/conduit_church/main",
            "ruins/conduit_church/maze_end",
            "ruins/conduit_church/maze",
            "ruins/conduit_church/secret_tunnel",
        )
        registerSimplePoolElements(
            "cydonia/ruins/conduit_church/follonichese",
            "cydonia/ruins/conduit_church/follonichese_shell",
        )
    }

    private fun bootstrapEnchantTower() {
        val processors = processorGetter.getOrThrow(GrProcessorLists.TOWER_DEGRADATION)
        registerSimplePoolElements(
            "ruins/enchant_tower/base",
            "ruins/enchant_tower/table",
            "ruins/enchant_tower/tower",
            processors = processors,
        )
        registerSimplePoolElements(
            "cydonia/ruins/enchant_tower/base",
            "cydonia/ruins/enchant_tower/table",
            "cydonia/ruins/enchant_tower/tower",
        )
        registerSimplePoolElements(
            "ruins/enchant_tower/armor_stand",
        )
    }

    private fun bootstrapNoteblockLab() {
        registerSimplePoolElementsWithCydonia(
            "ruins/noteblock_lab/basement",
            "ruins/noteblock_lab/house",
        )
    }

    private fun bootstrapAbandonedForge() {
        val processors = processorGetter.getOrThrow(GrProcessorLists.FORGE_DEGRADATION)
        registerSimplePoolElements(
            "ruins/abandoned_forge/base",
            "ruins/abandoned_forge/cave",
            "ruins/abandoned_forge/hole",
            processors = processors,
        )
    }

    private fun bootstrapMinorRuins() {
        registerSimplePoolElements(
            "ruins/conduit_ruins",
            "ruins/noteblock_ship",
        )
    }

    private fun bootstrapMisc() {
        registerSimplePoolElementsWithCydonia(
            "misc/cave_camp",
            "misc/marker",
        )
        registerSimplePoolElements(
            "cydonia/misc/golem_house",
            processors = processorGetter.getOrThrow(ProcessorLists.MOSSIFY_10_PERCENT),
        )
    }

    // IMPORTANT: this creates pools used only by the standalone versions
    // (used for /place and gamemaster mode), for natural village generation
    // we directly modify the vanilla pools
    private fun bootstrapStandaloneVillageHouses() {
        VillageBuildings.houseEntries[CATEGORY_GOLEM_HOUSE]!!.forEach { entry ->
            registerSimplePoolElements(
                entry.normal.path, entry.zombie.path,
            )
        }
    }


    private fun registerSimplePoolElementsWithCydonia(vararg paths: String, templatePaths: List<String> = paths.toList(), processors: Holder<StructureProcessorList> = emptyProcessor) {
        registerSimplePoolElements(paths.toList().flatMap { listOf(it, "cydonia/$it") }, templatePaths.flatMap { listOf(it, "cydonia/$it") }, processors)
    }

    private fun registerSimplePoolElements(vararg paths: String, templatePaths: List<String> = paths.toList(), processors: Holder<StructureProcessorList> = emptyProcessor) {
        registerSimplePoolElements(paths.toList(), templatePaths, processors)
    }

    private fun registerSimplePoolElements(paths: List<String>, templatePaths: List<String> = paths.toList(), processors: Holder<StructureProcessorList> = emptyProcessor) {
        for ((path, templatePath) in paths.zip(templatePaths)) {
            registerSimplePoolElement(path, templatePath, processors)
        }
    }

    private fun registerSimplePoolElement(path: String, templatePath: String = path, processors: Holder<StructureProcessorList> = emptyProcessor) {
        context.register(ResourceKey.create(Registries.TEMPLATE_POOL, resLoc(path)), StructureTemplatePool(
            emptyPool,
            listOf(
                // Why didn't Mojang expose a function to do this with any resloc goddammit
                Pair.of(singlePoolElement(resLoc(templatePath), processors), 1),
            ),
            Projection.RIGID
        ))
    }

    private fun singlePoolElement(id: ResourceLocation, processors: Holder<StructureProcessorList>) = java.util.function.Function<Projection, SinglePoolElement> { proj ->
        SinglePoolElement(Either.left(id), processors, proj)
    }
}