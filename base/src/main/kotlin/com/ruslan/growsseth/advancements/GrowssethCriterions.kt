package com.ruslan.growsseth.advancements

import com.ruslan.growsseth.advancements.criterion.JigsawPieceTrigger
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.advancements.Criterion
import net.minecraft.advancements.CriterionTrigger
import net.minecraft.resources.Identifier

object GrowssethCriterions {
    val all = mutableMapOf<Identifier, CriterionTrigger<*>>()

    val JIGSAW_PIECE = make("jigsaw_piece", JigsawPieceTrigger())

    private fun <T : CriterionTrigger<*>> make(name: String, trigger: T): T {
        all[resLoc(name)] = trigger
        return trigger
    }

    fun registerCriterions(registrator: (Identifier, CriterionTrigger<*>) -> Unit) {
        all.forEach { (t, u) -> registrator(t, u) }
    }
}