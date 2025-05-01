package com.ruslan.growsseth.mixin.entity.mob;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ruslan.growsseth.entity.researcher.Researcher;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public abstract class CreeperMixin {

    @Mixin(Creeper.class)
    public abstract static class CreeperMobMixin extends Monster {
        protected CreeperMobMixin(EntityType<? extends Monster> entityType, Level level) {
            super(entityType, level);
        }

        @Inject(at = @At("HEAD"), method = "registerGoals")
        private void avoidResearchers(CallbackInfo ci) {
            Creeper th1s = (Creeper)(Object)this;
            goalSelector.addGoal(3, new AvoidEntityGoal<>(th1s, Researcher.class, Researcher.WALK_LIMIT_DISTANCE + 6, 1.0, 1.2));
        }
    }

    @Mixin(SwellGoal.class)
    public abstract static class SwellGoalMixin {
        @Shadow @Final private Creeper creeper;
        @Unique private boolean creeperWasInTent;
        @Unique private BlockPos lastCreeperPos;

        @ModifyReturnValue(at = @At("TAIL"), method = "canUse")
        private boolean preventExplosionInTent(boolean original) {
            if (original == false) {
                // Skip check if already false
                return original;
            }
            BlockPos creeperPos = creeper.blockPosition();
            boolean coordsChanged = creeperCoordsChanged(creeperPos);
            if (creeperWasInTent && !coordsChanged) {
                // Creeper was in tent at last check and did not change position: skip tent check
                return false;
            }
            else if (coordsChanged) {
                // Creeper changed its position: we check if it's inside the Researcher tent
                ServerLevel serverLevel = (ServerLevel) creeper.level();        // Already server side from vanilla code
                StructureManager structureManager = serverLevel.structureManager();
                boolean creeperInTent = structureManager.getStructureAt(creeperPos, MixinHelpers.researcherTent).isValid();
                creeperWasInTent = creeperInTent;
                if (creeperInTent)
                    return false;
            }
            return original;
        }

        @Unique
        private boolean creeperCoordsChanged(BlockPos currentCreeperPos) {
            // We keep track of the last block pos for optimization purposes, we don't want to check
            // if the creeper is inside the Researcher tent every two ticks
            if (lastCreeperPos == null) {
                lastCreeperPos = currentCreeperPos;
                return true;
            }
            else if (currentCreeperPos.getX() == lastCreeperPos.getX() && currentCreeperPos.getY() == lastCreeperPos.getY() && currentCreeperPos.getZ() == lastCreeperPos.getZ()) {
                return false;
            }
            else {
                lastCreeperPos = currentCreeperPos;
                return true;
            }
        }
    }
}
