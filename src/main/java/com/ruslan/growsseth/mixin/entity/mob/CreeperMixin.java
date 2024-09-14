package com.ruslan.growsseth.mixin.entity.mob;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.entity.researcher.Researcher;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {

    protected CreeperMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("HEAD"), method = "registerGoals")
    private void avoidResearchers(CallbackInfo ci) {
        Creeper th1s = (Creeper)(Object)this;
        goalSelector.addGoal(3, new AvoidEntityGoal<>(th1s, Researcher.class, Researcher.WALK_LIMIT_DISTANCE + 6, 1.0, 1.2));
    }

    @WrapOperation(
            method = "explodeCreeper",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;")
    )
    private Explosion preventExplosionInTent(Level instance, @Nullable Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction explosionInteraction, Operation<Explosion> original) {
        // already server side from vanilla code
        ServerLevel serverLevel = (ServerLevel) instance;
        StructureManager structureManager = serverLevel.structureManager();
        if (structureManager.getStructureAt(source.getOnPos(), MixinHelpers.researcherTent).isValid()) {
            return serverLevel.explode(source, x, y, z, 0, Level.ExplosionInteraction.MOB);    // 0 radius to avoid damaging the donkey or leash
        } else {
            return original.call(instance, source, x, y, z, radius, explosionInteraction);
        }
    }
}
