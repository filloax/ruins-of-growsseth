package com.ruslan.growsseth.mixin.item.mapitem;

import com.google.common.collect.Maps;
import com.ruslan.growsseth.interfaces.WithCustomDecorations;
import com.ruslan.growsseth.maps.CustomMapData;
import com.ruslan.growsseth.maps.CustomMapDecoration;
import com.ruslan.growsseth.maps.CustomMapDecorationType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(MapItemSavedData.class)
public abstract class MapItemSavedDataMixin
implements WithCustomDecorations {
    @Unique
    public final Map<String, CustomMapDecoration> customDecorations = Maps.newLinkedHashMap();

    @Shadow
    protected abstract void setDecorationsDirty();

    @Shadow @Final
    Map<String, MapDecoration> decorations;

    @Inject(at = @At("TAIL"), method = "tickCarriedBy(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V")
    private void loadCustomDataTags(Player player, ItemStack itemStack, CallbackInfo ci) {
        CustomMapData.INSTANCE.loadCustomDecorationFromDataJava(
            customDecorations, itemStack, player, this::addCustomDecoration
        );
    }

    @Inject(at = @At("RETURN"), method = "locked()Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;")
    private void addCustomDecorationsToLocked(CallbackInfoReturnable<MapItemSavedData> cir) {
        MapItemSavedData newData = cir.getReturnValue();
        WithCustomDecorations newDataWcd = (WithCustomDecorations)(Object) newData;
        newDataWcd.getCustomDecorationsMap().putAll(this.customDecorations);
    }

    @Inject(at = @At("HEAD"), method = "isExplorationMap()Z", cancellable = true)
    private void checkisExplorationMap(CallbackInfoReturnable<Boolean> cir) {
        for (CustomMapDecoration decoration: this.customDecorations.values()) {
            if (decoration.getType().getStructure() != null) {
                cir.setReturnValue(true);
                break;
            }
        }
    }

    @Override
    public void gr$addClientSideCustomDecorations(List<CustomMapDecoration> list) {
        this.customDecorations.clear();

        for(int i = 0; i < list.size(); ++i) {
            CustomMapDecoration customMapDecoration = list.get(i);
            this.customDecorations.put("icon-" + i, customMapDecoration);
        }
    }

    @Unique
    public void addCustomDecoration(CustomMapDecorationType type, @Nullable LevelAccessor levelAccessor, String id, double x, double z, double rot, @Nullable Component name) {
        MapItemSavedData th1s = (MapItemSavedData) (Object) this;
        int scaleMult = 1 << th1s.scale;
        float mapOffsetX = (float) (x - (double) th1s.centerX) / (float) scaleMult;
        float mapOffsetY = (float) (z - (double) th1s.centerZ) / (float) scaleMult;
        byte mapOffsetAbsX = (byte) ((int) ((double) (mapOffsetX * 2.0F) + 0.5));
        byte mapOffsetAbsY = (byte) ((int) ((double) (mapOffsetY * 2.0F) + 0.5));
        byte rotBytes;
        if (mapOffsetX >= -63.0F && mapOffsetY >= -63.0F && mapOffsetX <= 63.0F && mapOffsetY <= 63.0F) {
            rot += rot < 0.0 ? -8.0 : 8.0;
            rotBytes = (byte) ((int) (rot * 16.0 / 360.0));
            if (th1s.dimension == Level.NETHER && levelAccessor != null) {
                int l = (int) (levelAccessor.getLevelData().getDayTime() / 10L);
                rotBytes = (byte) (l * l * 34187121 + l * 121 >> 15 & 15);
            }
        } else {
//            if (type != MapDecoration.Type.PLAYER) { // There isn't an equivalent for custom decorations
            this.removeCustomDecoration(id);
            return;
//            }
        }

        CustomMapDecoration customMapDecoration = new CustomMapDecoration(type, mapOffsetAbsX, mapOffsetAbsY, rotBytes, name);
        CustomMapDecoration prevDecoration = customDecorations.put(id, customMapDecoration);
        if (!customMapDecoration.equals(prevDecoration)) {
            this.setDecorationsDirty();
        }
    }

    @Unique
    public void removeCustomDecoration(String id) {
        this.customDecorations.remove(id);
        this.setDecorationsDirty();
    }

    @Override
    public Iterable<CustomMapDecoration> getCustomDecorations() {
        return customDecorations.values();
    }
    @Override
    public Map<String, CustomMapDecoration> getCustomDecorationsMap() {
        return customDecorations;
    }
}
