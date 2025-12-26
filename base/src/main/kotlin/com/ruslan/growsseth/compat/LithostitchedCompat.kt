//package com.ruslan.growsseth.compat
//
//import com.mojang.datafixers.util.Pair
//import dev.worldgen.lithostitched.worldgen.structure.DelegatingStructure
//import dev.worldgen.lithostitched.worldgen.modifier.AddTemplatePoolElementsModifier
//import net.minecraft.core.HolderSet
//import net.minecraft.core.Registry
//import net.minecraft.core.registries.Registries
//import net.minecraft.resources.ResourceKey
//import net.minecraft.resources.ResourceLocation
//import net.minecraft.world.level.levelgen.structure.Structure
//import net.minecraft.world.level.levelgen.structure.StructureType
//import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement
//import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool
//import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList
//
//
///**
// * Only use if Lithostitched mod is enabled
// */
//object LithostitchedCompat {
//    /**
//     * Some mods that use Lithostitched library replaced e.g. villages with DelegatingStructure
//     * (a type added by the mod), which would break some of our checks.
//     */
//    fun isValidJigsawCheckStructure(struct: Structure): Boolean {
//        if (struct is DelegatingStructure) {
//            return struct.delegate().type() == StructureType.JIGSAW
//        }
//        return false
//    }
//    /**
//     * The Lithostitched library replaces vanilla template pools with their optimized format,
//     * if it's loaded we add the village houses to the pools using their implementation
//     */
//    fun addBuildingToPool(
//        templatePoolRegistry: Registry<StructureTemplatePool>,
//        processorListRegistry: Registry<StructureProcessorList>,
//        poolId: ResourceLocation,
//        poolPieceId: ResourceLocation,
//        weight: Int
//    ) {
//        // Get the pool and create the piece
//        val pool = templatePoolRegistry.getOrThrow(ResourceKey.create(Registries.TEMPLATE_POOL, poolId))
//        val newPiece = LegacySinglePoolElement.legacy(poolPieceId.toString())
//            .apply(StructureTemplatePool.Projection.RIGID)
//        // Create a HolderSet with just this pool
//        val poolSet = HolderSet.direct(pool)
//        // Create and apply the modifier to the pool set
//        val modifier = AddTemplatePoolElementsModifier(
//            0,
//            poolSet,
//            listOf(Pair.of(newPiece, weight))
//        )
//        modifier.applyModifier()
//    }
//}