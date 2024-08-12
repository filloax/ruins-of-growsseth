package com.ruslan.growsseth.mixin.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(MapItem.class)
public class MapItemInfoMixin {
    @Unique
    private static Set<Holder<MapDecorationType>> IGNORE_TYPES = Set.of(
            MapDecorationTypes.FRAME,
            MapDecorationTypes.PLAYER,
            MapDecorationTypes.PLAYER_OFF_MAP,
            MapDecorationTypes.PLAYER_OFF_LIMITS
        );

    // Adds in detailed mode
    @Inject(method = "appendHoverText", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 3, shift = At.Shift.AFTER))
    private void appendTargetHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag, CallbackInfo ci) {
        var firstDecorationOpt = Optional.ofNullable(stack.get(DataComponents.MAP_DECORATIONS))
                .map(MapDecorations::decorations)
                .flatMap(d -> d.values().stream()
                        .filter(x -> !IGNORE_TYPES.contains(x.type()))
                        .findFirst()
                );
        if (firstDecorationOpt.isPresent()) {
            var firstDecoration = firstDecorationOpt.get();
            tooltipComponents.add(Component.translatable("growsseth.filled_map.target", firstDecoration.x(), firstDecoration.z()).withStyle(ChatFormatting.GRAY));
        }
    }
}
