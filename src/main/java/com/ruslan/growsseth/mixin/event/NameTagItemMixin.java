package com.ruslan.growsseth.mixin.event;

import com.ruslan.growsseth.events.NameTagRenameEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagItemMixin {
    @Inject(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setCustomName(Lnet/minecraft/network/chat/Component;)V", ordinal = 0),
            method = "interactLivingEntity", cancellable = true
    )
    private void checkBeforeUse(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResultHolder<ItemStack> result = NameTagRenameEvent.BEFORE.invoker().rename(interactionTarget, stack.getHoverName(), (ServerPlayer) player, stack, usedHand);

        if (result.getResult() != InteractionResult.PASS) {
            cir.setReturnValue(result.getResult());
            cir.cancel();
        }
    }

    // Invoke before stack is shrunk (aka name tag is consumed)
    @Inject(
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
        method = "interactLivingEntity"
    )
    private void checkAfterUse(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand, CallbackInfoReturnable<InteractionResult> cir) {
        NameTagRenameEvent.AFTER.invoker().rename(interactionTarget, stack.getHoverName(), (ServerPlayer) player, stack, usedHand);
    }
}
