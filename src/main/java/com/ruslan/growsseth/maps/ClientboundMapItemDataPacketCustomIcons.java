package com.ruslan.growsseth.maps;

import com.google.common.collect.Lists;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.interfaces.WithCustomDecorations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Map data packet extension to include custom map icons
 * Note: this means we need to replace via mixins or other all calls to clientboundmapitemdatapacket with ones
 * that include our custom data
 */
public class ClientboundMapItemDataPacketCustomIcons extends ClientboundMapItemDataPacket {
    @Nullable
    private final List<CustomMapDecoration> customDecorations;

    public ClientboundMapItemDataPacketCustomIcons(
            int mapId, byte scale, boolean locked,
            @Nullable Collection<MapDecoration> decorations,
            @Nullable Collection<CustomMapDecoration> customDecorations,
            @Nullable MapItemSavedData.MapPatch colorPatch
    ) {
        super(mapId, scale, locked, decorations, colorPatch);

        this.customDecorations = customDecorations != null ? Lists.newArrayList(customDecorations) : null;
    }

    public ClientboundMapItemDataPacketCustomIcons(FriendlyByteBuf friendlyByteBuf) {
        super(friendlyByteBuf);
        this.customDecorations = null;
        RuinsOfGrowsseth.INSTANCE.log(Level.ERROR,
    "Constructed ClientboundMapItemDataPacketCustomIcons with FriendlyByteBuf constructor, " +
            "not implemented (as not used in base game), custom icons won't likely work"
        );
    }

    @Override
    public void applyToMap(MapItemSavedData mapItemSavedData) {
        super.applyToMap(mapItemSavedData);
        if (this.customDecorations != null) {
            WithCustomDecorations mapItemSavedDataWic = (WithCustomDecorations) mapItemSavedData;
            mapItemSavedDataWic.gr$addClientSideCustomDecorations(this.customDecorations);
//            RuinsOfGrowsseth.INSTANCE.log(Level.INFO, "Loaded decorations! " + this.customDecorations);
        }
    }
}
