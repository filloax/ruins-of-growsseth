package com.ruslan.growsseth.utils;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

// Fix for kotlin 2.3 bug with JSpecify and equals method override taken from Entity (todo: remove after kotlin 2.3.20)
public class ZombieVillagerFix extends ZombieVillager {
    public ZombieVillagerFix(EntityType<? extends @NotNull ZombieVillager> p_481402_, Level p_479747_) {
        super(p_481402_, p_479747_);
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }
}
