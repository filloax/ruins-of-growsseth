package com.ruslan.growsseth.mixin.custommapitem;

import com.ruslan.growsseth.maps.CustomMapItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Target: the Slot temp class in the constructor
@Mixin(targets = "net/minecraft/world/inventory/CartographyTableMenu$3")
public abstract class CartographyTableMenu_ConstructorSlot1Mixin {
    // See CustomMapItems.isCustomMapItem doc
    @Redirect(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            method = "mayPlace(Lnet/minecraft/world/item/ItemStack;)Z"
    )
    private boolean recognizeMapsFromClass(ItemStack instance, Item item) {
        return CustomMapItems.checkCustomMapItem(instance, item);
    }
}
