package com.ruslan.growsseth.mixin;

import com.ruslan.growsseth.config.MiscConfig;
import com.ruslan.growsseth.interfaces.ZombieWithEasterEgg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class EasterEggMixin {
    @Mixin(Zombie.class)
    public static abstract class ZombieMixin extends Entity implements ZombieWithEasterEgg {

        @Unique
        private static final EntityDataAccessor<Boolean> DATA_IS_GUBER = SynchedEntityData.defineId(Zombie.class, EntityDataSerializers.BOOLEAN);
        @Unique
        private static final String DATA_TAG_GUBER = "growsseth_isGuber";

        // Needed for extends to work
        protected ZombieMixin(EntityType<?> entityType, Level level) {
            super(entityType, level);
        }

        @Override
        public boolean gr$isGuber() {
            return getIsGuber();
        }

        @Unique
        private void setIsGuber(boolean value) {
            this.getEntityData().set(DATA_IS_GUBER, value);
        }

        @Unique
        private boolean getIsGuber() {
            return this.getEntityData().get(DATA_IS_GUBER);
        }

        @Inject(
            method = "defineSynchedData",
            at = @At("RETURN")
        )
        private void onDefineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
            builder.define(DATA_IS_GUBER, false);
        }

        @Inject(
            method = "finalizeSpawn",
            at = @At("RETURN")
        )
        private void onFinalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData, CallbackInfoReturnable<SpawnGroupData> cir) {
            float value = random.nextFloat();
            if (value < MiscConfig.zombieGuberSpawnChance / 100) {
                setIsGuber(true);
            }
        }

        @Inject(
            method = "addAdditionalSaveData",
            at = @At("RETURN")
        )
        private void onAddAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
            output.putBoolean(DATA_TAG_GUBER, getIsGuber());
        }

        @Inject(
            method = "readAdditionalSaveData",
            at = @At("RETURN")
        )
        private void onReadAdditionalSaveData(ValueInput input, CallbackInfo ci) {
            setIsGuber(input.getBooleanOr(DATA_TAG_GUBER, false));
        }
    }
}
