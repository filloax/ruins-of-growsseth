package com.ruslan.growsseth.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.ruslan.growsseth.entity.researcher.Researcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherRoseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherRoseBlock.class)
public abstract class WitherRoseBlockMixin {
    @Unique
    boolean researcherInside = false;

    @Inject(method = "entityInside", at = @At("HEAD"))
    private void checkIfResearcherInside(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!level.isClientSide && level.getDifficulty() != Difficulty.PEACEFUL)
            researcherInside = entity instanceof Researcher;
    }

    @ModifyExpressionValue(method = "entityInside",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isInvulnerableTo(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean researcherDoesNotStepOnRose (boolean original) {
        return original || researcherInside;
    }
}
