package com.ruslan.growsseth.mixin.client.custommapitem;

import com.ruslan.growsseth.maps.CustomMapItems;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin {
    // See CustomMapItems.isCustomMapItem doc
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V")
    private boolean recognizeMapsFromClass(ItemStack instance, Item item) {
        return CustomMapItems.checkCustomMapItem(instance, item);
    }
}
