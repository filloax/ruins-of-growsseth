package com.ruslan.growsseth.mixin.custommapitem;

import com.ruslan.growsseth.maps.CustomMapItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    // See CustomMapItems.isCustomMapItem doc
    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            method = "getTooltipLines(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"
    )
    private boolean recognizeMapsFromClass(ItemStack instance, Item item) {
        return CustomMapItems.checkCustomMapItem(instance, item);
    }
}
