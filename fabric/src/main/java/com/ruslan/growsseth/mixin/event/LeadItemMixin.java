package com.ruslan.growsseth.mixin.event;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.events.LeashEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(LeadItem.class)
public abstract class LeadItemMixin {
    @Inject(
        method = "bindPlayerMobs",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Leashable;setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V",
            shift = At.Shift.AFTER
        )
    )
    private static void triggerEventOnLeash(
            Player player, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir,
            @Local(ordinal = 0) Leashable mob
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            LeashEvents.FENCE_LEASH.invoker().apply(mob, pos, serverPlayer);
        } else {
            RuinsOfGrowsseth.getLOGGER().error("Cannot run leash mixin, not on server side (shouldn't happen)");
        }
    }

    @WrapOperation(
        method = "bindPlayerMobs",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/LeadItem;leashableInArea(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Ljava/util/function/Predicate;)Ljava/util/List;"
        )
    )
    private static List<Leashable> beforeLeadMob(
            Level level, BlockPos pos, Predicate<Leashable> predicate, Operation<List<Leashable>> original,
            @Local(argsOnly = true) Player player
    ) {
        if (player instanceof ServerPlayer serverPlayer) {
            return original.call(level, pos, predicate.and(mob -> {
                InteractionResult result = LeashEvents.BEFORE_FENCE_LEASH.invoker().apply(mob, pos, serverPlayer);
                return result != InteractionResult.FAIL;
            }));
        } else {
            return original.call(level, pos, predicate);
        }
    }
}
