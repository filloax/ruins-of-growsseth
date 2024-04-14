package com.ruslan.growsseth.mixin.entity;

import com.ruslan.growsseth.effect.GrowssethEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    LivingEntity entity = (LivingEntity)(Object)this;

    @ModifyVariable(method = "hurt", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private float applyGrowssethEffects(float amount, DamageSource source) {
        // The effects are applied before any other computation, they do not affect void and /kill damage but affect starve damage
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (entity.hasEffect(GrowssethEffects.INSTANCE.getSERENITY()) &&        // attacks from creative players bypass the serenity effect
                    !(source.getEntity() instanceof Player && ((Player) source.getEntity()).isCreative()))
                return 1f;
            else if (entity.hasEffect(GrowssethEffects.INSTANCE.getFIGHTING_SPIRIT()))
                return amount - (amount * 0.35f);
        }
        return amount;
    }
}
