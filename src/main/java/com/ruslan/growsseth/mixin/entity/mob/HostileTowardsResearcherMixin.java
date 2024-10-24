package com.ruslan.growsseth.mixin.entity.mob;

import com.ruslan.growsseth.config.GrowssethConfig;
import com.ruslan.growsseth.config.ResearcherConfig;
import com.ruslan.growsseth.entity.researcher.Researcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin (value = {
        AbstractSkeleton.class,
        Raider.class,
        Zombie.class
        // Vexes have a different priority and need their own mixin
})
public abstract class HostileTowardsResearcherMixin extends Mob {
    protected HostileTowardsResearcherMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }
    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void addResearcherTarget(CallbackInfo ci) {
        if (ResearcherConfig.researcherInteractsWithMobs)
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Researcher.class, true));
    }
}
