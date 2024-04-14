package com.ruslan.growsseth.mixin.item.mapitem;

import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
    @Accessor
    boolean getUnlimitedTracking();
    @Accessor
    void setUnlimitedTracking(boolean value);
}
