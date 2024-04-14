package com.ruslan.growsseth.structure

import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import com.ruslan.growsseth.structure.pieces.SimpleStructurePiece
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType

object GrowssethStructurePieceTypes {
    @JvmStatic
    val all = mutableMapOf<ResourceLocation, StructurePieceType>()

    val RESEARCHER_TENT = registerPieceType("gr_res_tent") { tag, ctx -> ResearcherTent(ctx, tag) }
    val SIMPLE = registerPieceType("gr_simple") { tag, ctx -> SimpleStructurePiece(ctx, tag) }

    private fun registerPieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType {
        all[resLoc(name)] = structurePieceType
        return structurePieceType
    }

    fun registerAll(registry: Registry<StructurePieceType>) {
        all.forEach{
            Registry.register(registry, it.key, it.value)
        }
    }
}