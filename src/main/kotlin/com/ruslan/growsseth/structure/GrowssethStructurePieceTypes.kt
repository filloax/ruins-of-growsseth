package com.ruslan.growsseth.structure

import com.ruslan.growsseth.utils.resLoc
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType

object GrowssethStructurePieceTypes {
    @JvmStatic
    val all = mutableMapOf<ResourceLocation, StructurePieceType>()

    val RESEARCHER_TENT = registerPieceType("gr_res_tent") { tag, ctx -> ResearcherTent(ctx, tag) }

    private fun registerPieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType {
        all[resLoc(name)] = structurePieceType
        return structurePieceType
    }

    fun registerStructurePieces(registrator: (ResourceLocation, StructurePieceType) -> Unit) {
        all.forEach{
            registrator(it.key, it.value)
        }
    }
}