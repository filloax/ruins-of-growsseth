package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.nbt.loadField
import com.filloax.fxlib.api.nbt.saveField
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.DebugConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.SpawnTimeTracker
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.sound.GrowssethSounds
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.Difficulty
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.monster.ZombieVillager
import net.minecraft.world.entity.monster.ZombifiedPiglin
import net.minecraft.world.entity.npc.AbstractVillager
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.LevelEvent
import java.time.LocalDateTime
import kotlin.math.min


class ZombieResearcher(entityType: EntityType<ZombieResearcher>, level: Level) :
    ZombieVillager(entityType, level), SpawnTimeTracker, ResearcherDataUser {
    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return ZombieVillager.createAttributes()
                // taken from non zombie researcher
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 10.0)
        }
        const val SPAWN_TIME_TAG = "ResearcherSpawnTime"
    }

    var researcherData: CompoundTag? = null
    var researcherOriginalPos: BlockPos? = null
    var shouldDespawn: Boolean = false
    override var lastWorldDataTime: LocalDateTime? = null

    // World time the researcher was spawned first at (used in rmresearcher remote command)
    override var spawnTime: Long
        set(value) { _spawnTime = value }
        get() {
            if (_spawnTime == null)
                _spawnTime = level().gameTime
            return _spawnTime!!
        }
    private var _spawnTime: Long? = null

    override fun finalizeSpawn(level: ServerLevelAccessor, difficulty: DifficultyInstance, reason: MobSpawnType, spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        villagerData = villagerData.setProfession(VillagerProfession.CARTOGRAPHER).setLevel(5)
        return super.finalizeSpawn(level, difficulty, reason, spawnData)
    }

    // same as zombie villagers, with some exceptions
    override fun registerGoals() {
        goalSelector.addGoal(2, RestrictSunGoal(this))      // taken from skeleton behavior (like the next one)
        // same priority as attacking villagers and after attacking player, useful for simple tent variant:
        goalSelector.addGoal(3, FleeSunGoal(this, 1.0))
        // no destroy turtle egg goal
        goalSelector.addGoal(8, LookAtPlayerGoal(this, Player::class.java, 8.0f))
        goalSelector.addGoal(8, RandomLookAroundGoal(this))
        this.addBehaviourGoals()
    }

    override fun addBehaviourGoals() {
        goalSelector.addGoal(2, ZombieResearcherAttackGoal(this, 1.0, false, level()))    // special goal to not despawn in peaceful
        // no move through village goal
        goalSelector.addGoal(7, WaterAvoidingRandomStrollGoal(this, 1.0))
        targetSelector.addGoal(1, HurtByTargetGoal(this, *arrayOfNulls(0)).setAlertOthers(*arrayOf<Class<*>>(ZombifiedPiglin::class.java)))
        targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Player::class.java, true))
        targetSelector.addGoal(3, NearestAttackableTargetGoal(this, AbstractVillager::class.java, false))
        targetSelector.addGoal(3, NearestAttackableTargetGoal(this, IronGolem::class.java, true))
        targetSelector.addGoal(3, NearestAttackableTargetGoal(this, Researcher::class.java, true))      // should never happen, but is funny
        // does not care about turtles
    }

    override fun shouldDespawnInPeaceful(): Boolean {
        return false        // we don't want the quest to break
    }

    private fun convertToResearcher(serverLevel: ServerLevel, alsoMove: Boolean = false) {
        val researcher = convertTo(GrowssethEntities.RESEARCHER, false)
        for (equipmentSlot in EquipmentSlot.entries) {
            val itemStack = getItemBySlot(equipmentSlot)
            if (itemStack.isEmpty) continue
            val d = getEquipmentDropChance(equipmentSlot).toDouble()
            if (d <= 1.0) continue
            this.spawnAtLocation(itemStack)
        }
        if (researcher == null) {
            RuinsOfGrowsseth.LOGGER.error("Zombie researcher to researcher for $this conversion failed!")
            return
        }
        researcher.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(researcher.blockPosition()), MobSpawnType.CONVERSION, null)

        val useResData = if (ResearcherConfig.singleResearcher) {
            ResearcherSavedData.getPersistent(serverLevel.server).data
        } else researcherData

        useResData?.let { researcher.readResearcherData(it) }
        researcher.healed = true
        researcher.dialogues?.resetNearbyPlayers()
        researcherOriginalPos?.let { researcher.resetStartingPos(it) }
        researcher.addEffect(MobEffectInstance(MobEffects.CONFUSION, 200, 0))

        if (alsoMove) {
            researcher.startingPos?.let { researcher.moveTo(it, researcher.yRot, researcher.xRot) }
        }
    }

    override fun finishConversion(serverLevel: ServerLevel) {
        convertToResearcher(serverLevel)
        if (!this.isSilent) {
            serverLevel.levelEvent(null, LevelEvent.SOUND_ZOMBIE_CONVERTED, blockPosition(), 0)
        }
    }

    override fun tick() {
        super.tick()
        // Make conversion quicker (more or less two times as fast, since we tick it a second time)
        if (!level().isClientSide && this.isAlive && this.isConverting) {
            if (DebugConfig.researcherSpeedup)
                villagerConversionTime = min(10, villagerConversionTime)
            villagerConversionTime--
        }
    }

    override fun customServerAiStep() {
        super.customServerAiStep()

        val server = server!!

        if (ResearcherConfig.singleResearcher) { // && this.tickCount % 5 == 0) {
            // make sure we are up to date in case more researcher entities are loaded
            // (edge case, but just in case)
            // Note that this does not load from nbt every time, but uses the single instance
            val savedData = ResearcherSavedData.getPersistent(server)
            if (savedData.isDead) {
                RuinsOfGrowsseth.LOGGER.info("Zombie researcher $this | should be dead from data, discarding...")
                this.discard()
                return
            }
            // Note: zombie doesn't directly use saved data, only keeps it for when converting later
            // additionally, it only uses the stored data in case singleresearcher is off
            // Meaning this is only useful to cases where a user disabled singleResearcher when the
            // researcher was a zombie
            if (!this.isUpToDateWithWorldData(savedData)) {
                RuinsOfGrowsseth.LOGGER.info("Zombie researcher $this | is not up to date with world data, updating...")
                this.researcherData = savedData.data
                this.lastWorldDataTime = savedData.lastChangeTimestamp
                RuinsOfGrowsseth.LOGGER.info("Zombie researcher $this | updated world data!")
            }
        }

        // If healed researcher from another tent, then instantly cure
        if (ResearcherQuestComponent.isHealed(server)) {
            convertToResearcher(this.level() as ServerLevel, true)
        }
        if (tickCount % 20 == 0) {
            if (GrowssethExtraEvents.shouldRunResearcherRemoveCheck) {
                GrowssethExtraEvents.researcherRemoveCheck(this, this)
            }
        }
        if (shouldDespawn) {
            this.remove(RemovalReason.DISCARDED)
            RuinsOfGrowsseth.LOGGER.info("Removed $this because another researcher was killed somewhere else")
        }
    }

    override fun die(damageSource: DamageSource) {
        super.die(damageSource)
        if (ResearcherConfig.singleResearcher) { server?.let { serv ->
            val savedData = ResearcherSavedData.getPersistent(serv)
            savedData.isDead = true
            savedData.setDirty()
        } }
    }

    override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false
    override fun requiresCustomPersistence(): Boolean = true

    override fun setBaby(baby: Boolean) { }     // do nothing

    override fun addAdditionalSaveData(compound: CompoundTag) {
        super.addAdditionalSaveData(compound)
        researcherData?.let { compound.put("ResearcherData", it) }
        compound.saveField("ResearcherPos", BlockPos.CODEC, ::researcherOriginalPos)
        compound.putLong(Researcher.SPAWN_TIME_TAG, spawnTime)
    }

    override fun readAdditionalSaveData(compound: CompoundTag) {
        super.readAdditionalSaveData(compound)
        compound.getCompound("ResearcherData")?.let { researcherData = it }
        researcherOriginalPos = compound.loadField("ResearcherPos", BlockPos.CODEC)
        if (compound.contains(SPAWN_TIME_TAG)) {
            spawnTime = compound.getLong(SPAWN_TIME_TAG)
        }
    }

    class ZombieResearcherAttackGoal(zombie: Zombie, speedModifier: Double, followingTargetEvenIfNotSeen: Boolean, val level: Level):
        ZombieAttackGoal(zombie, speedModifier, followingTargetEvenIfNotSeen)
    {
        override fun canUse(): Boolean {
            if (level.difficulty == Difficulty.PEACEFUL)
                return false        // to allow completing the quest even in peaceful
            return super.canUse()
        }
    }

    // No need to also override step sounds (I hope)
    override fun getAmbientSound(): SoundEvent? {
        return GrowssethSounds.ZOMBIE_RESEARCHER_AMBIENT
    }
    override fun getHurtSound(damageSource: DamageSource): SoundEvent? {
        return GrowssethSounds.ZOMBIE_RESEARCHER_HURT
    }
    override fun getDeathSound(): SoundEvent? {
        return GrowssethSounds.ZOMBIE_RESEARCHER_DEATH
    }
}