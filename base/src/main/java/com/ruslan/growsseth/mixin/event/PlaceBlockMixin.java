package com.ruslan.growsseth.mixin.event;

import com.ruslan.growsseth.events.Events;
import com.ruslan.growsseth.events.PlaceBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BlockItem.class)
public abstract class PlaceBlockMixin {
    @Inject(at = @At("TAIL"), method = "place", locals = LocalCapture.CAPTURE_FAILHARD)
    public void onPlaceItemTrigger(BlockPlaceContext __, CallbackInfoReturnable<InteractionResult> cir, BlockPlaceContext context, BlockState blockState, BlockPos blockPos, Level level) {
        Player player = context.getPlayer();
        if (player != null) {
            Events.PLACE_BLOCK.invoke(new PlaceBlockEvent.Post(player, level, blockPos, context, blockState, (BlockItem) (Object) this));
        }
    }
}
