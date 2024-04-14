package com.ruslan.growsseth.mixin.item.mapitem;

import com.ruslan.growsseth.interfaces.WithCustomDecorations;
import com.ruslan.growsseth.maps.ClientboundMapItemDataPacketCustomIcons;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(MapItemSavedData.HoldingPlayer.class)
public abstract class MapItemSavedData_HoldingPlayerMixin {

    @Final @Shadow MapItemSavedData field_132; // MapItemSavedData.this

    // Hijack map packet to add custom decorations data
    @Inject(at = @At("RETURN"), method = "nextUpdatePacket(I)Lnet/minecraft/network/protocol/Packet;", cancellable = true)
    void replaceBaseClientboundPacketWithOurExtension(int i, CallbackInfoReturnable<Packet<?>> cir) {
        Packet<?> packet = cir.getReturnValue();
        // packet null if decorations aren't dirty (= needing update), see source code for MapItemSaveData
        if (packet != null) {
            ClientboundMapItemDataPacket cbPacket = (ClientboundMapItemDataPacket) packet;
            ClientboundMapItemDataPacketAccessor cbPacket_Acc = (ClientboundMapItemDataPacketAccessor) cbPacket;
            MapItemSavedData mapItemSavedDataThis = field_132;
            WithCustomDecorations mapItemSavedDataWic = (WithCustomDecorations) mapItemSavedDataThis;

            ClientboundMapItemDataPacketCustomIcons customCbPacket = new ClientboundMapItemDataPacketCustomIcons(
                    cbPacket.getMapId(), cbPacket.getScale(), cbPacket.isLocked(),
                    cbPacket_Acc.getDecorations(), mapItemSavedDataWic.getCustomDecorationsMap().values(), cbPacket_Acc.getColorPatch()
            );

            // Wrap customCbPacket in vanilla bounded packet as the game knows how to handle it, otherwise
            // ConnectionProtocol.getProtocolForPacket will crash as it gets packets by class (and so doesn't
            // know how to handle it even if it is a subclass of a known packet), this allows it to "get past the gate"
            cir.setReturnValue(new ClientboundBundlePacket(List.of(customCbPacket)));
        }
    }
}
