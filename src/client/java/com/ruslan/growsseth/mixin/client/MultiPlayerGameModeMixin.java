package com.ruslan.growsseth.mixin.client;

import com.ruslan.growsseth.item.ResearcherDaggerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Code taken from https://forum.mixmods.com.br/f294-tutorial/t8489-minecraft-java-increasing-the-item-range-item-reac, thanks a lot!
// (the second mixin is not needed since the range is under 3.0)
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method="getPickRange", at=@At("HEAD"), cancellable = true)
    public void getPickRange(CallbackInfoReturnable<Float> cir){
        Player player = Minecraft.getInstance().player;
        if(player != null && !player.isCreative() && player.getMainHandItem().getItem() instanceof ResearcherDaggerItem) {
            cir.setReturnValue(2.5f);   // half a block less than normal survival attack range
        }
    }
}