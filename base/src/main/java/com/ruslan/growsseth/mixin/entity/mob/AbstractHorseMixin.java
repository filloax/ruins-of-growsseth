package com.ruslan.growsseth.mixin.entity.mob;

import com.ruslan.growsseth.Constants;
import com.ruslan.growsseth.entity.researcher.ResearcherDonkey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends Animal {
    protected AbstractHorseMixin(EntityType<? extends Animal> entityType, Level level) { super(entityType, level); }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float injected(float amount, DamageSource source) {
        if(this.getTags().contains(Constants.TAG_RESEARCHER_DONKEY) &&
                !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) &&      // to prevent stopping the kill command
                ResearcherDonkey.shouldProtectDonkey(level(), this))
            return 0f;
        else
            return amount;
    }
}