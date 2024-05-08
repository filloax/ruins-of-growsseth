package com.ruslan.growsseth.mixin.client.custommapitem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
    @WrapOperation(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V")
    private boolean recognizeMapsFromClass(ItemStack instance, Item item, Operation<Boolean> original) {
        return CustomMapItems.checkMapItemWrapper(instance, item, original);
    }
}
