package com.ruslan.growsseth.entity.researcher

import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.player.Player

class ResearcherRandomStrollGoal(private val researcher: Researcher, speedModifier: Double)  :
    WaterAvoidingRandomStrollGoal(researcher, speedModifier) {
    override fun canUse(): Boolean {
        if (!researcher.metPlayer || researcher.isTrading() || researcher.dialogues?.nearbyPlayers()?.isNotEmpty() != false) {
            return false
        }
        return super.canUse()
    }

    override fun start() {
        // 0 accuracy to reduce chance of spinning
        mob.navigation.moveTo(mob.navigation.createPath(wantedX, wantedY, wantedZ, 0), speedModifier)
    }
}

class ResearcherLookAtPlayerGoal(private val researcher: Researcher, lookDistance: Float, lookChance: Float) :
    LookAtPlayerGoal(researcher, Player::class.java, lookDistance, lookChance) {
    override fun canUse(): Boolean {
        if (researcher.isTrading()) {
            lookAt = researcher.tradingPlayer
            return true
        }
        return super.canUse()
    }
}

class ResearcherHurtByTargetGoal(mob: PathfinderMob, vararg toIgnoreDamage: Class<*>?):
    HurtByTargetGoal(mob, *toIgnoreDamage) {
    override fun canUse(): Boolean {
        if (mob.lastHurtByMob is Player)    // players are treated separately
            return false
        return super.canUse()
    }
}