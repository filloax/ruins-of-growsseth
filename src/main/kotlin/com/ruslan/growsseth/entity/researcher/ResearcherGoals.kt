package com.ruslan.growsseth.entity.researcher

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.goal.BreathAirGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.pathfinder.PathComputationType


// Original goal used by dolphins, edited to work with researcher
class ResearcherBreatheAirGoal(private val researcher: Researcher) : BreathAirGoal(researcher) {
    override fun canUse(): Boolean {
        return researcher.isInWall
    }

    override fun start() {
        findAirPosition()
    }

    private fun findAirPosition() {
        // changed y checks from dolphin behavior
        val iterable = BlockPos.betweenClosed(
            // researcher is two blocks tall, we need to check air at head height
            Mth.floor(researcher.x - 1.0), Mth.floor(researcher.y + 1.0), Mth.floor(researcher.z - 1.0),
            Mth.floor(researcher.x + 1.0), Mth.floor(researcher.y + 1.0), Mth.floor(researcher.z + 1.0)
        )
        var blockPos: BlockPos? = null
        for (blockPos2 in iterable) {
            if (givesAir(researcher.level(), blockPos2)) {
                blockPos = blockPos2
                break
            }
        }
        if (blockPos != null) {
            // reduced speed to researcher walking speed, targeting block at foot level
            researcher.navigation.moveTo(blockPos.x.toDouble(), blockPos.y.toDouble() - 1.0, blockPos.z.toDouble(), 0.6)
        }
    }

    private fun givesAir(level: LevelReader, pos: BlockPos): Boolean {
        val blockState = level.getBlockState(pos)
        val blockStateLower = level.getBlockState(BlockPos(pos.x, pos.y - 1, pos.z))    // we want to check if the block at the feet of researcher is pathfindable
        return (level.getFluidState(pos).isEmpty || blockState.`is`(Blocks.BUBBLE_COLUMN)) && blockStateLower.isPathfindable(PathComputationType.LAND)
    }
}


class ResearcherRandomStrollGoal(private val researcher: Researcher, speedModifier: Double) :
    WaterAvoidingRandomStrollGoal(researcher, speedModifier) {
    override fun canUse(): Boolean {
        if (researcher.isTrading() || researcher.dialogues?.playersStillAround() == true) {
            return false
        }
        return super.canUse()
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


class ResearcherHurtByTargetGoal(private val researcher: PathfinderMob, vararg toIgnoreDamage: Class<*>?) :
    HurtByTargetGoal(researcher, *toIgnoreDamage) {
    override fun canUse(): Boolean {
        if (researcher.lastHurtByMob is Player)    // players are treated separately
            return false
        return super.canUse()
    }
}
