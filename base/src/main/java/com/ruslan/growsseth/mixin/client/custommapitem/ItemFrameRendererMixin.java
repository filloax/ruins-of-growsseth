package com.ruslan.growsseth.mixin.client.custommapitem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.maps.CustomMapItems;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {
    // todo: fix
    // See CustomMapItems.checkCustomMapItem doc
//    @WrapOperation(
//            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
//            method = "Lnet/minecraft/client/renderer/entity/ItemFrameRenderer;getFrameModelResourceLoc(ZLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/client/resources/model/ModelIdentifier;"
//    )
//    private boolean recognizeMapsFromClass(ItemStack instance, Item item, Operation<Boolean> original) {
//        return CustomMapItems.checkMapItemWrapper(instance, item, original);
//    }
}
