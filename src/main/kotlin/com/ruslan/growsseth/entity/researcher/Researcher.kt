package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.codec.mutableMapCodec
import com.filloax.fxlib.entity.delegate
import com.filloax.fxlib.entity.fixedChangeDimension
import com.filloax.fxlib.entity.getPersistData
import com.filloax.fxlib.getStructTagOrKey
import com.filloax.fxlib.nbt.getCompoundOrNull
import com.filloax.fxlib.nbt.loadField
import com.filloax.fxlib.nbt.saveField
import com.filloax.fxlib.secondsToTicks
import com.filloax.fxlib.structure.tracking.CustomPlacedStructureTracker
import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.dialogues.BasicDialogueEvents
import com.ruslan.growsseth.dialogues.DialoguesNpc
import com.ruslan.growsseth.effect.GrowssethEffects
import com.ruslan.growsseth.entity.RefreshableMerchant
import com.ruslan.growsseth.entity.SpawnTimeTracker
import com.ruslan.growsseth.entity.researcher.ResearcherCombatComponent.Companion.distanceForUnjustifiedAggression
import com.ruslan.growsseth.entity.researcher.ResearcherCombatComponent.ResearcherAttackGoal
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTradeMode
import com.ruslan.growsseth.entity.researcher.trades.ResearcherTradesData
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.item.GrowssethItems
import com.ruslan.growsseth.quests.QuestOwner
import com.ruslan.growsseth.structure.pieces.ResearcherTent
import com.ruslan.growsseth.structure.structure.ResearcherTentStructure
import com.ruslan.growsseth.utils.GrowssethCodecs
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.UUIDUtil
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.DamageTypeTags
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.*
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.*
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.AbstractIllager.IllagerArmPose
import net.minecraft.world.entity.monster.AbstractSkeleton
import net.minecraft.world.entity.monster.Vex
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.entity.npc.Npc
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.raid.Raider
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart
import net.minecraft.world.level.portal.PortalInfo
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.jvm.optionals.getOrNull


class Researcher(entityType: EntityType<Researcher>, level: Level) : PathfinderMob(entityType, level),
    Npc, RefreshableMerchant, InventoryCarrier, QuestOwner<Researcher>, DialoguesNpc, SpawnTimeTracker {

    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5)    // same of a villager
                .add(Attributes.MAX_HEALTH, 40.0)       // double of a villager
                .add(Attributes.ATTACK_DAMAGE, 13.0)    // (+ 5 of dagger = 18 total)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)     // goodbye range is 17, if lower he would say goodbye even when aggressive
        }
        const val RESEARCHER_XP = 25
        const val WALK_LIMIT_DISTANCE = 15
        const val WALK_LIMIT_DISTANCE_NIGHT = 3
        const val RESEARCHER_ATTACK_REACH = 0.7         // default attack reach is 0,828

        const val DATA_TAG = "ResearcherData"
        const val SPAWN_TIME_TAG = "ResearcherSpawnTime"
        const val STARTING_POS_TAG = "ResearcherStartingPos"
        const val STARTING_DIM_TAG = "ResearcherStartingDim"
        const val TELEPORT_COUNTER_TAG = "ResearcherTPCounter"

        val RENAME_BLACKLIST = mutableMapOf(
            // True if it should check word parts
            "ricercatore" to false,
            "franco" to false,
            "folgo" to false,
            "foldo" to false,
            "palle" to true,
            "sabaku" to true,
            "lucio" to true,
            "lionel" to false,
            "julius" to false,
            "nicolaos" to false,
            "wazo" to true,
            "efisio" to false,
            "ruslan" to true,
            "grumm" to false,
            "dinnerbone" to false
        )

        val SPEED_MODIFIER_DRINKING = AttributeModifier(UUID.randomUUID(), "Researcher drinking speed penalty", -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
        // Used when fighting someone that is not running away (using this instead of sprinting for control over amount):
        val SPEED_MODIFIER_FIGHTING = AttributeModifier(UUID.randomUUID(), "Researcher fighting speed boost", 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)

        // Used for drinking potions:
        private val DATA_USING_ITEM: EntityDataAccessor<Boolean> = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)

        private val DATA_UNHAPPY_COUNTER = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.INT)
        // Next three need to be synched due to trade offers being both on client and server:
        private val DATA_ANGRY_FOR_MESS = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_DONKEY_BORROWED = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_HEALED = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)

        private val DATA_ANGRY_PARTICLES = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_DEFLECT_ARROW_PARTICLES = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)
        private val DATA_TELEPORT_PARTICLES = SynchedEntityData.defineId(Researcher::class.java, EntityDataSerializers.BOOLEAN)

//        private var tentStructures: List<Structure>? = null

        fun findTent(level: ServerLevel, startingPos: BlockPos, currentPos: BlockPos? = null): StructureStart? {
            val structureManager = level.structureManager()
            var tentStart: StructureStart? = null
//
//            val validStructures = tentStructures ?: run {
//                val structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE).holders().filter {
//                    it.value() is ResearcherTentStructure
//                }.map{ it.value() }.toList()
//                tentStructures = structures
//                structures
//            }

            // First try in starting pos, then less likely current pos
            for (pos in listOfNotNull(startingPos, currentPos)) {
                if (tentStart != null) break

//                tentStart = validStructures.firstNotNullOfOrNull { structureManager.getStructureAt(pos, it) }
                tentStart = structureManager.getStructureWithPieceAt(pos, GrowssethTags.StructTags.RESEARCHER_TENT)

                // Error in the fixed structures mixin? Just incase, given usecase of mod (streaming)
                // we have to avoid all avoidable crashes
                if (tentStart?.isValid == true && tentStart.structure !is ResearcherTentStructure) {
                    RuinsOfGrowsseth.LOGGER.error("Found wrong structure when searching tent, is ${tentStart.structure} in $tentStart")
                    tentStart = StructureStart.INVALID_START
                }

                if (tentStart?.isValid != true) {
                    tentStart = null
                }

                // No luck with the mixin-ed base function, directly search in our fixed structure track
                // just in case (mainly for streaming version)
                if (tentStart == null) {
                    val tracker = CustomPlacedStructureTracker.get(level)
                    tentStart = tracker.getByPos(pos).find { it.structure is ResearcherTentStructure }?.structureStart
                    if (tentStart != null) {
                        RuinsOfGrowsseth.LOGGER.warn("Couldn't find tent via mixin, found with structure tracker (at $pos)")
                    }
                }
            }
            return tentStart
        }
    }


    /* VARIABLES SECTION */


    val armPose : IllagerArmPose
        get() = if (this.isAggressive || this.isUsingItem) IllagerArmPose.ATTACKING else IllagerArmPose.CROSSED

    // No lateinit for these three, too many points of failure with Minecraft
    var startingPos: BlockPos? = null
        private set
    var startingDimension: ResourceKey<Level> = Level.OVERWORLD
        private set
    var metPlayer: Boolean = false
        private set

    /* If the donkey was borrowed by any player, do not check for specific player
       as there is only one donkey anyway and atm the penalty is shared
       (Treat is as if in multiplayer the researcher treats players as a group) */
    var donkeyWasBorrowed: Boolean by entityData.delegate(DATA_DONKEY_BORROWED)
    var angryForMess: Boolean by entityData.delegate(DATA_ANGRY_FOR_MESS)
    var unhappyCounter: Int by entityData.delegate(DATA_UNHAPPY_COUNTER)
    var healed: Boolean by entityData.delegate(DATA_HEALED)
    var showAngryParticles: Boolean by entityData.delegate(DATA_ANGRY_PARTICLES)
    var showArrowDeflectParticles: Boolean by entityData.delegate(DATA_DEFLECT_ARROW_PARTICLES)
    var showTeleportParticles: Boolean by entityData.delegate(DATA_TELEPORT_PARTICLES)

    val combat = ResearcherCombatComponent(this)
    val storedMapLocations = mutableMapOf<String, MapMemory>()
    val diary = if (!this.level().isClientSide()) ResearcherDiaryComponent(this) else null
    override val dialogues = if (!this.level().isClientSide()) ResearcherDialoguesComponent(this, random, combat) else null
    override val quest = if (!this.level().isClientSide()) {
        // Set to not active, so it doesn't get loaded when the zombie villager loads this in
        // on conversion end, before it applies the data
        ResearcherQuestComponent(this).also { it.data.active = false }
    } else null

    // World time the researcher was spawned first at
    // (used in clearoldresearchers remote command)
    override var spawnTime: Long
        private set(value) { _spawnTime = value }
        get() {
            if (_spawnTime == null)
                _spawnTime = level().gameTime
            return _spawnTime!!
        }
    private var _spawnTime: Long? = null

    val tent: ResearcherTent?
        get() {
            // Only search once
            val start = tentCache?.getOrNull()
                ?: findTent()
            tentCache = Optional.ofNullable(start)

            val firstPiece = start?.pieces?.get(0)
            if (firstPiece is ResearcherTent?) {
                return  firstPiece
            } else {
                RuinsOfGrowsseth.LOGGER.error("Researcher $this detected tent but not correct type, got $firstPiece")
                return null
            }
        }

    private val inventory = SimpleContainer(8)
    private var tradingPlayer: Player? = null
    private val offersByPlayer = mutableMapOf<UUID, MerchantOffers>()
    private var tradesData = server?.let { ResearcherTradesData(ResearcherTradeMode.getFromSettings(it)) }
    private var tentCache: Optional<StructureStart>? = null
    private var lastRefusedTradeTimer: Int = 0

    private var itemUsingTime = 0

    // For teleporting back to tent
    private var secondsAwayFromTent = 0
    private val maxSecondsAwayFromTent = 60 * 5
    private val maxDistanceFromStartingPos = 20
    private var needsToTpBack = false

    // For cheese prevention
    internal var isStuck: Boolean = false
    internal var stuckCounter: Int = 0
    internal val maxStuckCounter: Int = secondsToTicks(2.0f)
    internal var lastCheckStuckPosition: BlockPos? = blockPosition()
    internal var needsJumpBoost = false


    /* METHODS SECTION */

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        super.defineSynchedData(builder)
        builder.define(DATA_UNHAPPY_COUNTER, 0)
        builder.define(DATA_ANGRY_FOR_MESS, false)
        builder.define(DATA_DONKEY_BORROWED, false)
        builder.define(DATA_HEALED, false)
        builder.define(DATA_USING_ITEM, false)
        builder.define(DATA_ANGRY_PARTICLES, false)
        builder.define(DATA_DEFLECT_ARROW_PARTICLES, false)
        builder.define(DATA_TELEPORT_PARTICLES, false)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, FloatGoal(this))
        goalSelector.addGoal(1, OpenDoorGoal(this, true))
        goalSelector.addGoal(2, ResearcherAttackGoal(this, 0.7, true))
        goalSelector.addGoal(3, MoveTowardsRestrictionGoal(this, 0.6))
        goalSelector.addGoal(4, ResearcherRandomStrollGoal(this, 0.6))
        goalSelector.addGoal(5, ResearcherLookAtPlayerGoal(this, 8f, 0.1f))


        targetSelector.addGoal(0, NearestAttackableTargetGoal(this, Player::class.java, 0, true, true)
            { player -> combat.wantsToKillPlayer((player as Player)) })
        if (ResearcherConfig.researcherInteractsWithMobs) {
            targetSelector.addGoal(1, NearestAttackableTargetGoal(this, Mob::class.java, 0, false, true)
                { livingEntity: LivingEntity? -> livingEntity is Mob && livingEntity.target == this })
            targetSelector.addGoal(2, ResearcherHurtByTargetGoal(this))
            if (ResearcherConfig.researcherStrikesFirst)
                targetSelector.addGoal(2, NearestAttackableTargetGoal(this, Mob::class.java, 0, true, true)
                    { livingEntity: LivingEntity? -> ( (livingEntity != null && this.distanceTo(livingEntity) < distanceForUnjustifiedAggression) &&
                            (livingEntity is Raider || livingEntity is Vex || livingEntity is Zombie || livingEntity is AbstractSkeleton) ) }
                )
        }
    }

    // NOTE: Ran only once at first spawn
    override fun finalizeSpawn(level: ServerLevelAccessor, difficulty: DifficultyInstance, mobSpawnType: MobSpawnType, spawnGroupData: SpawnGroupData?): SpawnGroupData? {
        val savedData = server?.let{ serv ->
            // Load data from previous researchers
            if (ResearcherConfig.singleResearcher) {
                ResearcherSavedData.getPersistent(serv)
            } else {
                ResearcherSavedData.create()
            }
        }

        quest?.data?.active = true
        this.startingPos = blockPosition()
        this.startingDimension = level.level.dimension()

        if (savedData != null && savedData.data.allKeys.isNotEmpty()) {
            readSavedData(savedData)
        }

        // Set savedData if it was just created (and so nbt empty)
        if (savedData != null) {
            if (savedData.data.allKeys.isEmpty())
                writeSavedData(savedData)
        }

        spawnTime = level().gameTime

        isLeftHanded = false
        val randomSource = level.random
        this.populateDefaultEquipmentSlots(randomSource, difficulty)    // for equipping dagger

        return spawnGroupData
    }

    override fun aiStep() {
        updateSwingTime()       // needed for the attack animation

        if (showAngryParticles) {
            addParticlesAroundSelf(ParticleTypes.ANGRY_VILLAGER, 3, 6, 0.7)
            showAngryParticles = false
        }
        if (showArrowDeflectParticles) {
            addParticlesInFrontOfSelf(ParticleTypes.CRIT, 2, 4)
            showArrowDeflectParticles = false
        }
        if (showTeleportParticles) {
            addParticlesAroundSelf(ParticleTypes.PORTAL, 14, 20, 0.0)
            showTeleportParticles = false
        }

        if (!level().isClientSide && isAlive) {
            handleItems()

            // witch stuff
//            if (random.nextFloat() < 7.5E-4f)
//                level().broadcastEntityEvent(this, 15.toByte())
        }
        super.aiStep()
    }

    // Server side only
    private fun handleItems() {
        if (isUsingItem) {
            if (!isSilent && itemUsingTime == (offhandItem.useDuration / 4)) {    // play drinking sound when item is halfway consumed
                val itemStack = offhandItem

                val sound = when (itemStack.item) {
                    Items.POTION -> SoundEvents.GENERIC_DRINK
                    Items.HONEY_BOTTLE -> SoundEvents.HONEY_DRINK
                    Items.ENDER_PEARL -> SoundEvents.ENDERMAN_TELEPORT
                    else -> null
                }

                if (sound != null) {
                    level().playSound(null, x, y, z, sound, soundSource, 1.0f, 0.8f + random.nextFloat() * 0.4f)
                }
            }

            itemUsingTime--
            if (itemUsingTime <= 0) {
                isUsingItem = false
                val itemStack = offhandItem
                setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY)

                if (itemStack.`is`(Items.POTION)) {
                    val list = itemStack[DataComponents.POTION_CONTENTS]?.potion
                        ?.map{ p -> p.value().effects }
                        ?.orElse(listOf())
                    if (list != null)
                        for (mobEffectInstance in list)
                            addEffect(MobEffectInstance(mobEffectInstance))
                    gameEvent(GameEvent.DRINK)
                }
                else if (itemStack.`is`(Items.HONEY_BOTTLE)) {
                    removeEffect(MobEffects.POISON)
                    gameEvent(GameEvent.DRINK)
                }
                else if (itemStack.`is`(Items.ENDER_PEARL)) {
                    stopRiding()
                    showTeleportParticles = true
                    needsToTpBack = false
                    secondsAwayFromTent = 0

                    val targetLevel = server?.getLevel(startingDimension) ?:
                        throw IllegalStateException("Unkown level when researcher teleporting to start dimension $startingDimension")

                    fixedChangeDimension(targetLevel, PortalInfo(
                        startingPos!!.center, Vec3.ZERO, yRot, xRot
                    ))
                    gameEvent(GameEvent.TELEPORT)
                }

                getAttribute(Attributes.MOVEMENT_SPEED)!!.removeModifier(SPEED_MODIFIER_DRINKING.id)
            }
        }
        else {
            var potion: Holder<Potion>? = null
            var item: Item? = null

            if (needsToTpBack)
                item = Items.ENDER_PEARL

            if (needsJumpBoost && !hasEffect(MobEffects.JUMP))
                potion = Potions.STRONG_LEAPING

            else if (       // trying to counter cheese attempts
                ResearcherConfig.researcherAntiCheat &&
                stuckCounter >= maxStuckCounter &&
                ((!hasEffect(MobEffects.DAMAGE_RESISTANCE) || !hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) ||
                        (hasEffect(MobEffects.DAMAGE_RESISTANCE) && hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                                && getEffect(MobEffects.DAMAGE_RESISTANCE)?.endsWithin(40) == true))
            ) {
                if (target is ServerPlayer) {
                    isStuck = true
                    showAngryParticles = true
                    dialogues?.triggerDialogue(target as ServerPlayer, ResearcherDialoguesComponent.PLAYER_CHEATS)
                }
                potion = Potions.STRONG_TURTLE_MASTER
            }

            else if (lastDamageSource != null && lastDamageSource!!.`is`(DamageTypeTags.IS_DROWNING) && !hasEffect(MobEffects.WATER_BREATHING))
                potion = Potions.WATER_BREATHING

            else if ((isOnFire || lastDamageSource != null && lastDamageSource!!.`is`(DamageTypeTags.IS_FIRE)) && !hasEffect(MobEffects.FIRE_RESISTANCE))
                potion = Potions.FIRE_RESISTANCE

            else if (health < maxHealth && !hasEffect(MobEffects.REGENERATION) && (isAggressive || hasEffect(MobEffects.POISON) || random.nextFloat() < 0.05f))
                potion = Potions.STRONG_REGENERATION        // if not aggressive random chance to drink to make it seem natural

            else if (hasEffect(MobEffects.POISON))
                item = Items.HONEY_BOTTLE

            if (potion != null || item != null) {
                if (item != null)
                    setItemSlot(EquipmentSlot.OFFHAND, ItemStack(item))
                if (potion != null)
                    setItemSlot(EquipmentSlot.OFFHAND, ItemStack(Items.POTION).also {
                        it[DataComponents.POTION_CONTENTS] = PotionContents(potion)
                    })

                itemUsingTime = if (offhandItem.`is`(Items.ENDER_PEARL))
                    secondsToTicks(1f)
                else
                    offhandItem.useDuration / 2
                isUsingItem = true

                val attributeInstance = getAttribute(Attributes.MOVEMENT_SPEED)
                attributeInstance!!.removeModifier(SPEED_MODIFIER_DRINKING.id)
                attributeInstance.addTransientModifier(SPEED_MODIFIER_DRINKING)
            }
        }
    }

    override fun customServerAiStep() {
        super.customServerAiStep()

        // Always passes as server code
        val serverLevel = level() as ServerLevel

        if (this.startingPos == null)
            this.startingPos = this.blockPosition()

        quest?.aiStep()
        // Run after quest to run at its same time
        GrowssethExtraEvents.queuedRemoveTentWithGiftEvent?.let {
            GrowssethExtraEvents.removeTentWithGift(this, serverLevel)
        }

        diary?.aiStep()
        dialogues?.dialoguesStep()

        if (!metPlayer && dialogues?.nearbyPlayers()?.isNotEmpty() == true)
            metPlayer = true

        if (level().isNight || !metPlayer)
            restrictTo(this.startingPos!!, WALK_LIMIT_DISTANCE_NIGHT)
        else
            restrictTo(this.startingPos!!, WALK_LIMIT_DISTANCE)

        // Every second for lag prevention, check researcher teleport. Wait for chunks to be finished
        // loading before running teleport to avoid a bug (possibly caused by wrong thread == currentThread)
        // check in ServerChunkCache?
        if (this.tickCount % 20 == 0) {
            GrowssethExtraEvents.queuedTpResearcherEvent?.let {
                GrowssethExtraEvents.teleportResearcher(this, serverLevel)
            }
            if (GrowssethExtraEvents.shouldRunResearcherRemoveCheck) {
                GrowssethExtraEvents.researcherRemoveCheck(this, this)
            }

            if (ResearcherConfig.researcherTeleports) {
                if (position().distanceTo(startingPos!!.center) > maxDistanceFromStartingPos) {
                    secondsAwayFromTent++
                    if (secondsAwayFromTent >= maxSecondsAwayFromTent)
                        needsToTpBack = true
                } else
                    secondsAwayFromTent = 0
            }
        }

        if (this.tickCount % 10 == 0) {
            if (ResearcherConfig.immortalResearcher) {
                addEffect(MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 15, 4, false, false))
                addEffect(MobEffectInstance(MobEffects.REGENERATION, 15, 4, false, false))
            }
            if (!isAggressive) {
                if (hasEffect(GrowssethEffects.FIGHTING_SPIRIT))
                    removeEffect(GrowssethEffects.FIGHTING_SPIRIT)
                if (hasEffect(GrowssethEffects.JUSTICE))
                    removeEffect(GrowssethEffects.JUSTICE)
                addEffect(MobEffectInstance(GrowssethEffects.SERENITY, 15, 0, false, false))
            }
        }

        combat.aggressiveAiStep()

        if (lastRefusedTradeTimer > 0)
            lastRefusedTradeTimer--
    }

    override fun tick() {
        super.tick()
        if (unhappyCounter > 0)
            unhappyCounter--
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val attacker = source.entity

        if (attacker is Player && ResearcherConfig.immortalResearcher && !attacker.isCreative)
            dialogues?.triggerDialogue(attacker as ServerPlayer, ResearcherDialoguesComponent.HIT_BY_PLAYER_IMMORTAL)

        val combatRet = combat.hurt(source, amount) { s, a -> super.hurt(s, a) }

        return combatRet ?: super.hurt(source, amount)
    }

    override fun mobInteract(player: Player, interactionHand: InteractionHand): InteractionResult? {
        if (combat.wantsToKillPlayer(player))      // to avoid interaction while fighting
            return InteractionResult.FAIL

        if (this.isAlive && !this.isTrading() && !this.isAggressive) {
            RuinsOfGrowsseth.LOGGER.info("Start interaction with researcher $this")
            player.getPersistData().putBoolean(Constants.DATA_PLAYER_MET_RESEARCHER, true)

            if (player is ServerPlayer) {
                val offers = getOffers(player)
                val blockTrades = angryForMess && !healed
                if (offers.isEmpty() || blockTrades) {
                    if (lastRefusedTradeTimer == 0) {
                        lastRefusedTradeTimer = 40
                        setUnhappy()
                        val reason = if (blockTrades) "angry" else "noTrades"
                        dialogues?.triggerDialogue(player, ResearcherDialoguesComponent.EV_REFUSE_TRADE, eventParam = reason)
                        return InteractionResult.sidedSuccess(level().isClientSide)
                    } else
                        return InteractionResult.FAIL
                }

                setTradingPlayer(player)
                openTradingScreen(player, this.displayName ?: this.name, 1)
            }

            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        return super.mobInteract(player, interactionHand)
    }

    override fun die(damageSource: DamageSource) {
        if (damageSource.entity is ServerPlayer)
            dialogues?.triggerDialogue(damageSource.entity as ServerPlayer, BasicDialogueEvents.DEATH)
        super.die(damageSource)
    }


    /* Researcher data methods */

    // Only NBT stuff of this class
    fun makeResearcherData(): CompoundTag {
        val researcherData = CompoundTag()

        val persistMapMemory = if (!researcherData.contains("ResearcherMapMemory", Tag.TAG_COMPOUND.toInt())) {
            val newTag = CompoundTag()
            researcherData.put("ResearcherMapMemory", newTag)
            newTag
        } else {
            researcherData.getCompound("ResearcherMapMemory")
        }

        synchronized(storedMapLocations) { storedMapLocations.forEach { mapMemory ->
            val structData = CompoundTag()
            structData.putInt("x", mapMemory.value.pos.x)
            structData.putInt("y", mapMemory.value.pos.y)
            structData.putInt("z", mapMemory.value.pos.z)
            structData.putString("id", mapMemory.value.struct.map({"#${it.location()}"}, {"${it.location()}"}))
            structData.putInt("mapId", mapMemory.value.mapId)

            persistMapMemory.put(mapMemory.key, structData)
        } }

        researcherData.putBoolean("Healed", healed)
        researcherData.putBoolean("AngryForMess", angryForMess)
        researcherData.putBoolean("DonkeyBorrowed", donkeyWasBorrowed)
        researcherData.putBoolean("MetPlayer", metPlayer)
        researcherData.saveField("PlayerOffers", mutableMapCodec(UUIDUtil.STRING_CODEC, GrowssethCodecs.MERCHANT_OFFERS_CODEC), ::offersByPlayer)
        researcherData.saveField("TradesData", ResearcherTradesData.CODEC) { tradesData() }

        dialogues?.writeNbt(researcherData)
        diary?.writeNbt(researcherData)
        quest?.writeNbt(researcherData)
        return researcherData
    }

    // Only NBT stuff of this class
    fun readResearcherData(researcherData: CompoundTag) {
        if (researcherData.contains("ResearcherMapMemory")) {
            val mapMemory = researcherData.getCompound("ResearcherMapMemory")
            mapMemory.allKeys.forEach {
                val tag = mapMemory.getCompound(it)
                storedMapLocations[it] = MapMemory(
                    BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    getStructTagOrKey(tag.getString("id")),
                    tag.getInt("mapId"),
                )
            }
        } else {
            storedMapLocations.clear()
        }
        if (researcherData.contains("Healed")) {
            healed = researcherData.getBoolean("Healed")
        } else {
            healed = false
        }
        if (researcherData.contains("AngryForMess")) {
            angryForMess = researcherData.getBoolean("AngryForMess")
        } else {
            angryForMess = false
        }
        if (researcherData.contains("DonkeyBorrowed")) {
            donkeyWasBorrowed = researcherData.getBoolean("DonkeyBorrowed")
        } else {
            donkeyWasBorrowed = false
        }
        if (researcherData.contains("MetPlayer")) {
            metPlayer = researcherData.getBoolean("MetPlayer")
        } else {
            metPlayer = false
        }

        offersByPlayer.clear()
        researcherData.loadField("PlayerOffers", mutableMapCodec(UUIDUtil.STRING_CODEC, GrowssethCodecs.MERCHANT_OFFERS_CODEC)) { offersByPlayer.putAll(it) }
        server?.let { tradesData = ResearcherTradesData(ResearcherTradeMode.getFromSettings(it)) }
        researcherData.loadField("TradesData", ResearcherTradesData.CODEC) {tradesData = it}

        dialogues?.readNbt(researcherData)
        diary?.readNbt(researcherData)
        quest?.readNbt(researcherData)
    }

    // Also vanilla things like name
    fun writeSavedData(savedData: ResearcherSavedData, existingDataTag: CompoundTag? = null) {
        savedData.data = existingDataTag ?: makeResearcherData()
        savedData.name = customName
        savedData.setDirty()
    }

    fun readSavedData(savedData: ResearcherSavedData) {
        readResearcherData(savedData.data)
        customName = savedData.name
    }

    override fun addAdditionalSaveData(compoundTag: CompoundTag) {
        super.addAdditionalSaveData(compoundTag)
        val data = makeResearcherData()

        if (ResearcherConfig.singleResearcher) {
            server?.let { serv ->
                val savedData = ResearcherSavedData.getPersistent(serv)
                writeSavedData(savedData, data)
            }
        }

        compoundTag.put(DATA_TAG, data)
        compoundTag.putLong(SPAWN_TIME_TAG, spawnTime)
        // Save separately from data as specific to single researcher
        compoundTag.saveField(TELEPORT_COUNTER_TAG, Codec.INT, ::secondsAwayFromTent)
        compoundTag.saveField(STARTING_POS_TAG, BlockPos.CODEC, ::startingPos)
        compoundTag.saveField(STARTING_DIM_TAG, ResourceKey.codec(Registries.DIMENSION), ::startingDimension)
    }

    override fun readAdditionalSaveData(compoundTag: CompoundTag) {
        super.readAdditionalSaveData(compoundTag)
        var read = false

        if (ResearcherConfig.singleResearcher) {
            server?.let { serv ->
                val savedData = ResearcherSavedData.getPersistent(serv)
                readSavedData(savedData)
                read = true
            }
        }
        if (!read) {
            compoundTag.getCompoundOrNull(DATA_TAG)?.let { readResearcherData(it) }
        }
        if (compoundTag.contains(SPAWN_TIME_TAG)) {
            spawnTime = compoundTag.getLong(SPAWN_TIME_TAG)
        }

        compoundTag.loadField(TELEPORT_COUNTER_TAG, Codec.INT) { secondsAwayFromTent = it }
        compoundTag.loadField(STARTING_POS_TAG, BlockPos.CODEC) { startingPos = it }
        compoundTag.loadField(STARTING_DIM_TAG, ResourceKey.codec(Registries.DIMENSION)) { startingDimension = it }
    }

    fun saveWorldData() {
        if (!level().isClientSide()) {
            val data = makeResearcherData()
            server?.let { serv ->
                val savedData = ResearcherSavedData.getPersistent(serv)
                writeSavedData(savedData, data)
            }
        }
    }


    /* Trading methods */

    fun isTrading(): Boolean { return tradingPlayer != null }

    private fun tradesData() = tradesData ?: throw IllegalStateException("Accessed tradesData in client!")

    override fun getOffers(): MerchantOffers {
        return server?.let { serv ->
            val provider = ResearcherTradeMode.providerFromSettings(serv)
            provider.getOffers(this, tradesData())
        } ?: MerchantOffers()
    }

    fun getOffers(player: ServerPlayer): MerchantOffers {
        val server = player.server
        val currentProvider = ResearcherTradeMode.providerFromSettings(server)
        val tradesData = tradesData()
        val time = level().gameTime

        if (currentProvider.mode != tradesData.mode) {
            tradesData.resetRandomTrades()
        }

        var offers = offersByPlayer[player.uuid]
        val updatedOffers = currentProvider.getOffers(this, tradesData, player)

        if (
            currentProvider.mode != tradesData.mode
            || offers == null
            || ResearcherConfig.tradesRestockTime > 0 && time - tradesData.lastTradeRefreshTime > ResearcherConfig.tradesRestockTime * Constants.DAY_TICKS_DURATION
            || !offersMatch(offers, updatedOffers)
        ) {
            // Refresh offers
            offers = updatedOffers
            tradesData.mode = currentProvider.mode
            tradesData.lastTradeRefreshTime = time
        }
        offersByPlayer[player.uuid] = offers

        return offers
    }

    private fun offersMatch(offersA: MerchantOffers, offersB: MerchantOffers): Boolean {
        if (offersA.size != offersB.size) return false

        for (i in offersA.indices) {
            if (!offersA[i].equals(offersB[i])) return false
        }

        return true
    }

    override fun refreshCurrentTrades() {
        assert(!this.level().isClientSide) { "Refreshing trades from client side" }
        val offersCurrent = this.offers ?: throw IllegalStateException("Refreshing offers when not set yet on server")
        tradingPlayer?.let {
            it.sendMerchantOffers(
                it.containerMenu.containerId, offersCurrent,1,0,false,true,
            )
        }
    }

    override fun notifyTrade(merchantOffer: MerchantOffer) {
        ambientSoundTime = -this.ambientSoundInterval
        rewardTradeXp(merchantOffer)
    }

    override fun notifyTradeUpdated(itemStack: ItemStack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.ambientSoundInterval + 20) {
            val sound = if (itemStack.isEmpty) SoundEvents.WANDERING_TRADER_NO else SoundEvents.WANDERING_TRADER_YES
            this.playSound(sound, this.soundVolume, this.voicePitch)
            ambientSoundTime = -this.ambientSoundInterval
        }
    }

    private fun rewardTradeXp(offer: MerchantOffer) {
        val xpValue = random.nextIntBetweenInclusive(2,4)   // (villagers have 3-6)
        if (offer.shouldRewardExp()) {
            level().addFreshEntity(ExperienceOrb(level(), this.x, this.y + 0.5, this.z, xpValue))
        }
    }


    /* Helper methods */

    fun setUsingItem(usingItem: Boolean) { getEntityData().set(DATA_USING_ITEM, usingItem) }

    override fun isUsingItem(): Boolean { return getEntityData().get(DATA_USING_ITEM) }

    fun resetStartingPos(pos: BlockPos) { startingPos = pos }

    private fun findTent(): StructureStart? {
        val tent = startingPos?.let {
            findTent(level() as ServerLevel, it, blockPosition())
        } ?: findTent(level() as ServerLevel, blockPosition())
        tentCache = Optional.ofNullable(tent)
        return tent
    }

    fun renameCheck(newName: String, player: ServerPlayer): InteractionResult {
        if (isAggressive) return InteractionResult.FAIL

        val newNameToCheck = newName.trim().lowercase()
        val newNameNoWhitespace = newNameToCheck.replace(Regex("\\s"), "")
        for ((blocklisted, containsCheck) in RENAME_BLACKLIST) {
            val blockLower = blocklisted.lowercase()
            if (
                containsCheck && blockLower in newNameToCheck
                || !containsCheck && newNameNoWhitespace == blockLower
            ) {
                dialogues?.triggerDialogue(player, BasicDialogueEvents.RENAME, eventParam = blockLower)
                setUnhappy()
                // prevent rename, but also prevent further interaction (ie trade screen opening)
                return InteractionResult.SUCCESS
            }
        }
        dialogues?.triggerDialogue(player, BasicDialogueEvents.RENAME, eventParam = newNameNoWhitespace)
        return InteractionResult.PASS
    }

    private fun addParticlesAroundSelf(particleOption: ParticleOptions, minCount: Int, maxCount: Int, yOffset: Double) {
        for (i in 1..random.nextIntBetweenInclusive(minCount, maxCount)) {
            val d = random.nextGaussian() * 0.02
            val e = random.nextGaussian() * 0.02
            val f = random.nextGaussian() * 0.02
            level().addParticle(particleOption, getRandomX(1.0), randomY + yOffset, getRandomZ(1.0), d, e, f)
        }
    }

    private fun addParticlesInFrontOfSelf(particleOption: ParticleOptions, minCount: Int, maxCount: Int) {
        for (i in 1..random.nextIntBetweenInclusive(minCount, maxCount)) {
            level().addParticle(particleOption,
                getRandomX(0.2) + lookAngle.x / 2 , getY(0.2) + 1, getRandomZ(0.2) + (lookAngle.z * 0.7), 0.0, 0.0, 0.0)
        }
    }

    fun setUnhappy() {
        unhappyCounter = 40
        if (!level().isClientSide()) {
            this.playSound(SoundEvents.WANDERING_TRADER_NO, this.soundVolume, this.voicePitch)
        }
    }


    /* Methods that override various vanilla behaviors */

    override fun isClientSide(): Boolean = level().isClientSide

    override fun removeWhenFarAway(d: Double): Boolean = false
    override fun requiresCustomPersistence(): Boolean = true

    override fun getInventory(): SimpleContainer = inventory
    override fun overrideOffers(merchantOffers: MerchantOffers) { }
    override fun setTradingPlayer(player: Player?) { tradingPlayer = player }
    override fun getTradingPlayer(): Player? = tradingPlayer

    override fun getNotifyTradeSound(): SoundEvent = SoundEvents.WANDERING_TRADER_YES
    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.WANDERING_TRADER_HURT
    override fun getDeathSound(): SoundEvent = SoundEvents.WANDERING_TRADER_DEATH
    override fun getAmbientSoundInterval(): Int = super.getAmbientSoundInterval() * 3
    override fun getAmbientSound(): SoundEvent? {
        return if (isTrading()) {
            SoundEvents.WANDERING_TRADER_TRADE
        } else SoundEvents.WANDERING_TRADER_AMBIENT
    }

    override fun canBeLeashed(player: Player): Boolean = false
    override fun getVillagerXp(): Int = 0
    override fun overrideXp(i: Int) { }
    override fun showProgressBar(): Boolean = false
    override fun getExperienceReward(): Int { return RESEARCHER_XP }
    override fun canDisableShield(): Boolean { return true }

    override fun getAttackBoundingBox(): AABB {
        val aABB3: AABB = super.getAttackBoundingBox()
        aABB3.deflate(0.828, 0.0, 0.828)        // reverting vanilla inflation (approximation)
        return aABB3.inflate(RESEARCHER_ATTACK_REACH, 0.0, RESEARCHER_ATTACK_REACH)
    }

    override fun createNavigation(level: Level): PathNavigation {
        // He won't climb walls, but will try to jump over two blocks heights
        val wallClimberNavigation = WallClimberNavigation(this, level)
        wallClimberNavigation.setCanOpenDoors(true)
        return wallClimberNavigation
    }

    override fun populateDefaultEquipmentSlots(random: RandomSource, difficulty: DifficultyInstance) {
        this.setItemSlot(EquipmentSlot.MAINHAND, combat.createWeapon())
    }

    override fun dropCustomDeathLoot(damageSource: DamageSource, looting: Int, hitByPlayer: Boolean) {
        val itemEntity = ItemEntity(level(), position().x, position().y, position().z, ItemStack(GrowssethItems.RESEARCHER_DAGGER).also { dagger ->
            dagger.enchant(Enchantments.SMITE, 5)       // smite only on drop to prevent exploits
        })
        level().addFreshEntity(itemEntity)
    }

    override fun handleNetherPortal() {
        if (ResearcherConfig.researcherTeleports) {
            return // prevent nether portal interaction
        }
    }

    internal fun jumpFromGroundAccess() {
        jumpFromGround()
    }

    /* OBJECTS */

    object Callbacks {
        fun nameTagRename(target: LivingEntity, name: Component, player: ServerPlayer, stack: ItemStack, usedHand: InteractionHand): InteractionResultHolder<ItemStack> {
            if (target is Researcher) {
                return InteractionResultHolder(target.renameCheck(name.string, player), stack)
            }
            return InteractionResultHolder.pass(stack)
        }
    }

    data class MapMemory(
        val pos: BlockPos,
        val struct: Either<TagKey<Structure>, ResourceKey<Structure>>,
        val mapId: Int,
    )
}
