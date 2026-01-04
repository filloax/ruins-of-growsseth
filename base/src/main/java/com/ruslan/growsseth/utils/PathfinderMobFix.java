package com.ruslan.growsseth.utils;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

// Fix for kotlin 2.3 bug with JSpecify and equals method override taken from Entity (todo: remove after kotlin 2.3.20)
public class PathfinderMobFix extends PathfinderMob {
    protected PathfinderMobFix(EntityType<? extends @NotNull PathfinderMob> p_21683_, Level p_21684_) {
        super(p_21683_, p_21684_);
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}
