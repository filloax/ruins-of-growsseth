package com.ruslan.growsseth.data

import com.mojang.datafixers.util.Either
import com.mojang.datafixers.util.Pair
import com.ruslan.growsseth.structure.GrProcessorLists
import com.ruslan.growsseth.structure.VillageBuildings
import com.ruslan.growsseth.structure.VillageBuildings.CATEGORY_GOLEM_HOUSE
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
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
import java.util.*

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
        bootstrapVillageHouses()
    }

    private fun bootstrapBeekeperHouse() {
        registerPoolElementsWithCydonia(
            "ruins/beekeeper_house/beenest",
            "ruins/beekeeper_house/house",
            "ruins/beekeeper_house/zombie",
        )
        registerPoolElements(
            "ruins/beekeeper_house/beehive",
            "cydonia/ruins/beekeeper_house/beehive_off",
            "cydonia/ruins/beekeeper_house/beehive_on",
            "cydonia/ruins/beekeeper_house/beenest_campfire",
        )
    }

    private fun bootstrapConduitChurch() {
        registerPoolElementsWithCydonia("ruins/conduit_church/main", overrideLiquidSettings = LiquidSettings.APPLY_WATERLOGGING)
        registerPoolElementsWithCydonia(
            "ruins/conduit_church/maze_end",
            "ruins/conduit_church/maze",
            "ruins/conduit_church/secret_tunnel",
        )
        registerPoolElements(
            "cydonia/ruins/conduit_church/follonichese",
            "cydonia/ruins/conduit_church/follonichese_shell",
            overrideLiquidSettings = LiquidSettings.APPLY_WATERLOGGING,
        )
    }

    private fun bootstrapEnchantTower() {
        val processors = processorGetter.getOrThrow(GrProcessorLists.TOWER_DEGRADATION)
        registerPoolElements(
            "ruins/enchant_tower/base",
            "ruins/enchant_tower/table",
            "ruins/enchant_tower/tower",
            processors = processors,
        )
        registerPoolElements(
            "cydonia/ruins/enchant_tower/base",
            "cydonia/ruins/enchant_tower/table",
            "cydonia/ruins/enchant_tower/tower",
        )
        registerPoolElements(
            "ruins/enchant_tower/armor_stand",
        )
    }

    private fun bootstrapNoteblockLab() {
        registerPoolElementsWithCydonia(
            "ruins/noteblock_lab/basement",
            "ruins/noteblock_lab/house",
        )
    }

    private fun bootstrapAbandonedForge() {
        val processors = processorGetter.getOrThrow(GrProcessorLists.FORGE_DEGRADATION)
        registerPoolElements(
            "ruins/abandoned_forge/base",
            "ruins/abandoned_forge/cave",
            "ruins/abandoned_forge/hole",
            processors = processors,
        )
    }

    private fun bootstrapMinorRuins() {
        registerPoolElements(
            "ruins/conduit_ruins",
            overrideLiquidSettings = LiquidSettings.APPLY_WATERLOGGING,
        )
        registerPoolElements(
            "ruins/noteblock_ship",
        )
    }

    private fun bootstrapMisc() {
        registerPoolElementsWithCydonia(
            "misc/cave_camp",
            "misc/marker",
        )
        registerPoolElements(
            "cydonia/misc/golem_house",
            processors = processorGetter.getOrThrow(ProcessorLists.MOSSIFY_10_PERCENT),
        )
    }

    /**
     * Used by the street nbts that get added to village pools manually via code
     * (see {@link VillageBuildings} and by standalone houses
     */
    private fun bootstrapVillageHouses() {
        VillageBuildings.houseEntries[CATEGORY_GOLEM_HOUSE]!!.forEach { entry ->
            registerPoolElements(
                entry.normalPool.path, entry.zombiePool.path,
                templatePaths = listOf(entry.normalTemplate.path, entry.zombieTemplate.path)
            )
        }
    }


    private fun registerPoolElementsWithCydonia(
        vararg paths: String,
        templatePaths: List<String> = paths.toList(),
        processors: Holder<StructureProcessorList> = emptyProcessor,
        overrideLiquidSettings: LiquidSettings? = null,
    ) {
        registerPoolElements(
            paths.toList().flatMap { listOf(it, "cydonia/$it") }, templatePaths.flatMap { listOf(it, "cydonia/$it") },
            processors, overrideLiquidSettings
        )
    }

    private fun registerPoolElements(
        vararg paths: String,
        templatePaths: List<String> = paths.toList(),
        processors: Holder<StructureProcessorList> = emptyProcessor,
        overrideLiquidSettings: LiquidSettings? = null,
    ) {
        registerPoolElements(paths.toList(), templatePaths, processors, overrideLiquidSettings)
    }

    private fun registerPoolElements(
        paths: List<String>,
        templatePaths: List<String> = paths.toList(),
        processors: Holder<StructureProcessorList> = emptyProcessor,
        overrideLiquidSettings: LiquidSettings? = null,
    ) {
        for ((path, templatePath) in paths.zip(templatePaths)) {
            registerSimplePoolElement(path, templatePath, processors, overrideLiquidSettings)
        }
    }

    private fun registerSimplePoolElement(
        path: String,
        templatePath: String = path,
        processors: Holder<StructureProcessorList> = emptyProcessor,
        overrideLiquidSettings: LiquidSettings? = null,
    ) {
        context.register(
            ResourceKey.create(Registries.TEMPLATE_POOL, resLoc(path)), StructureTemplatePool(
                emptyPool,
                listOf(
                    // Why didn't Mojang expose a function to do this with any resloc goddammit
                    Pair(singlePoolElement(resLoc(templatePath), processors, overrideLiquidSettings), 1, ),
                ),
                Projection.RIGID
            )
        )
    }

    private fun singlePoolElement(
        id: ResourceLocation,
        processors: Holder<StructureProcessorList>,
        overrideLiquidSettings: LiquidSettings? = null,
    ) = java.util.function.Function<Projection, SinglePoolElement> { proj ->
        SinglePoolElement(Either.left(id), processors, proj, Optional.ofNullable(overrideLiquidSettings))
    }
}