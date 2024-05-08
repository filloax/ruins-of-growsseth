package com.ruslan.growsseth.advancements.criterion

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.advancements.GrowssethAdvancements
import com.ruslan.growsseth.advancements.GrowssethCriterions
import net.minecraft.advancements.critereon.ContextAwarePredicate
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.SimpleCriterionTrigger
import net.minecraft.core.Position
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.ExtraCodecs
import java.util.*

class JigsawPieceTrigger : SimpleCriterionTrigger<JigsawPieceTrigger.Instance>() {

    override fun codec(): Codec<Instance> = Instance.CODEC

    fun trigger(player: ServerPlayer) {
        this.trigger(player) { triggerInstance ->
            triggerInstance.matches(player.serverLevel(), player.position())
        }
    }

    object Callbacks {
        fun onServerPlayerTick(player: ServerPlayer) {
            if (player.tickCount % 20 == 0) {
                GrowssethCriterions.JIGSAW_PIECE.trigger(player)
            }
        }
    }

    data class Instance(
        private val player_: Optional<ContextAwarePredicate>,
        val jigsawPiecePredicate: Optional<JigsawPiecePredicate>,
    ) : SimpleInstance {
        override fun player() = player_

        companion object {
            val CODEC: Codec<Instance> = RecordCodecBuilder.create { builder -> builder.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                JigsawPiecePredicate.CODEC.optionalFieldOf("jigsawPiecePredicate").forGetter(Instance::jigsawPiecePredicate),
            ).apply(builder, ::Instance) }
        }

        fun matches(level: ServerLevel, position: Position): Boolean {
            return jigsawPiecePredicate.map { it.matches(level, position.x(), position.y(), position.z()) }.orElse(false)
        }
    }
}