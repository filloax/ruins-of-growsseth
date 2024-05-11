package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.nbt.loadField
import com.filloax.fxlib.nbt.saveField
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.entity.GrowssethEntities
import com.ruslan.growsseth.entity.SpawnTimeTracker
import com.ruslan.growsseth.http.GrowssethExtraEvents
import com.ruslan.growsseth.item.GrowssethItems
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
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
import net.minecraft.world.entity.ai.goal.FleeSunGoal
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.ZombieVillager
import net.minecraft.world.entity.npc.VillagerProfession
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.LevelEvent

class ZombieResearcher(entityType: EntityType<ZombieResearcher>, level: Level) :
    ZombieVillager(entityType, level), SpawnTimeTracker {
    companion object {
        fun createAttributes(): AttributeSupplier.Builder {
            return ZombieVillager.createAttributes()
                // taken from non zombie researcher
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ARMOR, 10.0)
        }

        /*
        fun getLootTable(): LootTable.Builder = LootTable.lootTable()
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    /*.add(
                        LootItem.lootTableItem(Items.ROTTEN_FLESH)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 2.0F)))
                            .apply(LootingEnchantFunction.lootingMultiplier(UniformGenerator.between(0.0F, 1.0F)))
                    )*/
            )
            /* // Do not add horn to pool as need to create itemstack ourselves to init instrument
            .withPool(
                LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(GrowssethItems.RESEARCHER_HORN))
                    .`when`(LootItemKilledByPlayerCondition.killedByPlayer())
            ) */
         */

        const val SPAWN_TIME_TAG = "ResearcherSpawnTime"
    }

    override fun finalizeSpawn(
        level: ServerLevelAccessor, difficulty: DifficultyInstance,
        reason: MobSpawnType, spawnData: SpawnGroupData?
    ): SpawnGroupData? {
        villagerData = villagerData.setProfession(VillagerProfession.CARTOGRAPHER).setLevel(5)
        return super.finalizeSpawn(level, difficulty, reason, spawnData)
    }

    override fun registerGoals() {
        super.registerGoals()
        // same priority as attacking villagers, after attacking player, useful for simple tent variant
        goalSelector.addGoal(3, FleeSunGoal(this, 1.0))
    }

    var researcherData: CompoundTag? = null
    var researcherOriginalPos: BlockPos? = null

    // World time the researcher was spawned first at
    // (used in clearoldresearchers remote command)
    override var spawnTime: Long
        set(value) { _spawnTime = value }
        get() {
            if (_spawnTime == null) {
                _spawnTime = level().gameTime
            }
            return _spawnTime!!
        }
    private var _spawnTime: Long? = null

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
        researcherData?.let { researcher.readResearcherData(it) }
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
            villagerConversionTime--
        }
    }

    override fun customServerAiStep() {
        super.customServerAiStep()

        // If healed researcher from another tent, then instantly cure
        if (ResearcherQuestComponent.isHealed(server!!)) {
            convertToResearcher(this.level() as ServerLevel, true)
        }

        if (tickCount % 20 == 0) {
            if (GrowssethExtraEvents.shouldRunResearcherRemoveCheck) {
                GrowssethExtraEvents.researcherRemoveCheck(this, this)
            }
        }
    }

    override fun dropCustomDeathLoot(damageSource: DamageSource, looting: Int, hitByPlayer: Boolean) {
        super.dropCustomDeathLoot(damageSource, looting, hitByPlayer)
        val daggerItem = ItemStack(GrowssethItems.RESEARCHER_DAGGER)
        daggerItem.enchant(Enchantments.SMITE, 5)
        daggerItem.enchant(Enchantments.UNBREAKING, 3)
        daggerItem.enchant(Enchantments.MENDING, 1)
        val itemEntity = ItemEntity(level(), position().x, position().y, position().z, daggerItem)
        level().addFreshEntity(itemEntity)
    }

    override fun removeWhenFarAway(distanceToClosestPlayer: Double): Boolean = false
    override fun requiresCustomPersistence(): Boolean = true

    override fun setBaby(baby: Boolean) {
        // do nothing
    }

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
}