package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.secondsToTicks
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.dialogues.BasicDialogueEvents
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.sound.GrowssethSounds
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.boss.wither.WitherBoss
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.AbstractArrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import org.apache.commons.lang3.mutable.MutableInt

/**
 * Split combat-related features to simplify main class
 * (and avoid it being 1.3k lines long)
 */
class ResearcherCombatComponent(
    val owner: Researcher,
) {
    val level = owner.level()
    val lastKilledPlayers: MutableList<Player> = mutableListOf()

    // For player aggro management
    var hitCounter: MutableMap<Player, MutableInt> = mutableMapOf()
    val timeToCalmDown: Int = 10F.secondsToTicks()

    companion object {
        // distance for attacking mobs that are not going after him (if option is active)
        val distanceForUnjustifiedAggression: Int = 10
    }

    private val dialogues by owner::dialogues

    // For player aggro management
    private var angerBuildupTimer: MutableMap<Player, MutableInt> = mutableMapOf()
    private val maxHitCounter = 3

    private val lowHealthCondition: Boolean
        get() = owner.health <= owner.maxHealth / 3

    fun createWeapon(): ItemStack = ItemStack(GrowssethItems.RESEARCHER_DAGGER).also { dagger ->
        dagger.enchant(Enchantments.UNBREAKING, 3)
        dagger.enchant(Enchantments.MENDING, 1)
    }

    fun hurt(source: DamageSource, amount: Float, superHurt: (DamageSource, Float) -> Boolean): Boolean? {
        val attacker = source.entity
        if (attacker is WitherBoss)
            return superHurt(source, amount * 2)

        if (attacker is Player && !(attacker.isCreative || level.difficulty == Difficulty.PEACEFUL || ResearcherConfig.immortalResearcher)) {
            if (lowHealthCondition)
                dialogues?.triggerDialogue(attacker as ServerPlayer, BasicDialogueEvents.LOW_HEALTH)
            else
                dialogues?.triggerDialogue(attacker as ServerPlayer, BasicDialogueEvents.HIT_BY_PLAYER)

            if (!hitCounter.contains(attacker))
                hitCounter[attacker] = MutableInt(0)
            if (!wantsToKillPlayer(attacker)) {
                hitCounter[attacker]?.increment()
                angerBuildupTimer[attacker] = MutableInt(timeToCalmDown)
            }
        }

        if (owner.isAggressive && source.directEntity is AbstractArrow)
            if (deflectArrow(source)) {
                owner.showArrowDeflectParticles = true
                owner.playSound(GrowssethSounds.DEFLECT_ARROW_SOUND)
                return false
            }

        return null
    }

    fun aggressiveAiStep() {
        if (owner.isAggressive) {
            if (this.owner.tickCount % 10 == 0) {
                if (owner.hasEffect(GrowssethEffects.SERENITY))
                    owner.removeEffect(GrowssethEffects.SERENITY)
                if (owner.target !is WitherBoss)
                    owner.addEffect(MobEffectInstance(GrowssethEffects.FIGHTING_SPIRIT, 15, 0, false, false))

                // Hardcoded regen when fighting mobs and being cheesed by the player:
                if ((!ResearcherConfig.immortalResearcher && ResearcherConfig.researcherAntiCheat &&
                            owner.hasEffect(MobEffects.DAMAGE_RESISTANCE) && owner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN))
                    || (owner.target !is Player && owner.target !is WitherBoss)) {
                    owner.addEffect(MobEffectInstance(GrowssethEffects.JUSTICE, 15, 0, false, false))
                }
            }

            if (owner.target != null && owner.target!!.y >= owner.y + 2 && owner.distanceTo(owner.target!!) < 4) {
                owner.needsJumpBoost = true
                if (owner.onGround())
                    owner.jumpFromGroundAccess()
            } else
                owner.needsJumpBoost = false

            // Used for cheese prevention:
            if (ResearcherConfig.researcherAntiCheat) {
                if (owner.stuckCounter < owner.maxStuckCounter &&
                    owner.tickCount - owner.lastHurtByMobTimestamp < 2.0f.secondsToTicks() &&
                    owner.tickCount - owner.lastHurtMobTimestamp > 5.0f.secondsToTicks() &&
                    owner.blockPosition().distSqr(owner.lastCheckStuckPosition!!) < 4
                )
                    owner.stuckCounter++
                else {
                    owner.lastCheckStuckPosition = owner.blockPosition()
                    owner.stuckCounter = 0
                    owner.isStuck = false
                }
            }
        }

        val attributeInstance = owner.getAttribute(Attributes.MOVEMENT_SPEED)
        if (owner.isAggressive && (owner.tickCount - owner.lastHurtByMobTimestamp) in 0..3.0f.secondsToTicks()) {
            // if enemy is fighting speed up (to avoid keeping him back by attacking constantly)
            attributeInstance!!.removeModifier(Researcher.SPEED_MODIFIER_FIGHTING.id)
            attributeInstance.addTransientModifier(Researcher.SPEED_MODIFIER_FIGHTING)
        }
        else
            attributeInstance!!.removeModifier(Researcher.SPEED_MODIFIER_FIGHTING.id)

        angerBuildupTimer.forEach { (key, value) ->
            if (value.toInt() > 0 && !owner.isAggressive)
            // Slowly forgive players when not aggressive:
                angerBuildupTimer[key]!!.decrement()
            if (value.toInt() == 0) {       // using if (and not else if) since the timer decreases in the tick after the attack
                hitCounter[key]!!.decrement()
                angerBuildupTimer[key] =
                    if (hitCounter[key]!!.toInt() > 0) MutableInt(timeToCalmDown)
                    else MutableInt(-1)
            }
        }
    }

    fun wantsToKillPlayer(player: Player): Boolean {
        return (hitCounter.getOrDefault(player, 0).toInt() == maxHitCounter)
    }

    fun onPlayerKilled(player: ServerPlayer) {
        hitCounter[player]!!.setValue(0)
        angerBuildupTimer[player]!!.setValue(-1)
        lastKilledPlayers.add(player)
        dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_KILL_PLAYER)
    }

    private fun deflectArrow(source: DamageSource) : Boolean {
        val arrow = source.directEntity as AbstractArrow
        val directionToArrow = arrow.position().subtract(owner.position()).normalize()
        val dotProduct = directionToArrow.dot(owner.lookAngle.normalize())
        return dotProduct > 0.5
    }

    class ResearcherAttackGoal(mob: Researcher, speedModifier: Double, private val followingTargetEvenIfNotSeen: Boolean) :
        MeleeAttackGoal(mob, speedModifier, followingTargetEvenIfNotSeen) {

        override fun canContinueToUse(): Boolean {
            val livingEntity = mob.target
            return if (livingEntity == null) {
                false
            } else if (!livingEntity.isAlive) {
                false
            } else if (!followingTargetEvenIfNotSeen) {
                !mob.navigation.isDone
                // Removed the check for being away from the restriction
            } else {
                livingEntity !is Player || !livingEntity.isSpectator() && !livingEntity.isCreative
            }
        }
    }
}