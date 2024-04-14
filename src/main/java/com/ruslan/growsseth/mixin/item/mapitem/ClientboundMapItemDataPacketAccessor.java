package com.ruslan.growsseth.mixin.item.mapitem;

import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientboundMapItemDataPacket.class)
public interface ClientboundMapItemDataPacketAccessor {
    @Accessor
    List<MapDecoration> getDecorations();
    @Accessor
    MapItemSavedData.MapPatch getColorPatch();
}
