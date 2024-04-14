package com.ruslan.growsseth.mixin.event;

import com.llamalad7.mixinextras.sugar.Local;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.events.LeashEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeadItem.class)
public abstract class LeadItemMixin {
    @Inject(
            method = "bindPlayerMobs",
//            at = @At(value = "INVOKE", target="Lnet/minecraft/world/entity/Mob;setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V"),
            at = @At(value = "INVOKE", target="Lnet/minecraft/world/entity/Mob;getLeashHolder()Lnet/minecraft/world/entity/Entity;", shift = At.Shift.BY, by = 5),
            cancellable = true
    )
    private static void triggerEventOnLeash(Player player, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir, @Local Mob mob) {
        if (player instanceof ServerPlayer serverPlayer) {
            InteractionResult result = LeashEvents.BEFORE_FENCE_LEASH.invoker().apply(mob, pos, serverPlayer);
            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
                return;
            }

            LeashEvents.FENCE_LEASH.invoker().apply(mob, pos, serverPlayer);
        } else {
            RuinsOfGrowsseth.getLOGGER().error("Cannot run leash mixin, not on server side (shouldn't happen)");
        }
    }
}
