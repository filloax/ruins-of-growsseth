package com.ruslan.growsseth.structure

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.structure.templatesystem.*

object GrProcessorLists {
    val FORGE_DEGRADATION = key("forge_degradation")
    val TOWER_DEGRADATION = key("tower_degradation")


    private fun key(id: String) = ResourceKey.create(Registries.PROCESSOR_LIST, resLoc(id))

    fun bootstrap(context: BootstrapContext<StructureProcessorList>) {
        context.registerSimple(FORGE_DEGRADATION, RuleProcessor(listOf(
            ProcessorRule(
                RandomBlockMatchTest(Blocks.NETHER_BRICKS, 0.7f),
                AlwaysTrueTest.INSTANCE,
                Blocks.CRACKED_NETHER_BRICKS.defaultBlockState()
            ),
        )))
        context.registerSimple(TOWER_DEGRADATION, RuleProcessor(listOf(
            ProcessorRule(
                RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.5f),
                AlwaysTrueTest.INSTANCE,
                Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
            ),
            ProcessorRule(
                RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.2f),
                AlwaysTrueTest.INSTANCE,
                Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
            ),
        )))
    }

    private fun BootstrapContext<StructureProcessorList>.registerSimple(key: ResourceKey<StructureProcessorList>, processor: StructureProcessor) {
        register(key, StructureProcessorList(listOf(processor)))
    }
}