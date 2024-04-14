package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PlaceBlockEvent {
    public static Event<After> AFTER = EventFactory.createArrayBacked(After.class,
            (listeners) -> (player, world, pos, placeContext, blockState, item) -> {
                for (After event : listeners) {
                    event.place(player, world, pos, placeContext, blockState, item);
                }
            }
    );

    public interface After {
        void place(Player player, Level world, BlockPos pos, BlockPlaceContext placeContext, BlockState blockState, BlockItem item);
    }
}
