package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;

public class LeashEvents {
    private LeashEvents() {}

    public static Event<BeforeFenceLeash> BEFORE_FENCE_LEASH = EventFactory.createArrayBacked(BeforeFenceLeash.class,
            (listeners) -> (mob, pos, player) -> {
                for (BeforeFenceLeash event : listeners) {
                    InteractionResult result = event.apply(mob, pos, player);

                    if (result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public static Event<FenceLeash> FENCE_LEASH = EventFactory.createArrayBacked(FenceLeash.class,
            (listeners) -> (mob, pos, player) -> {
                for (FenceLeash event : listeners) {
                    event.apply(mob, pos, player);
                }
            }
    );

    public static Event<FenceUnleash> FENCE_UNLEASH = EventFactory.createArrayBacked(FenceUnleash.class,
            (listeners) -> (mob, pos) -> {
                for (FenceUnleash event : listeners) {
                    event.apply(mob, pos);
                }
            }
    );

    public interface BeforeFenceLeash {
        InteractionResult apply(Mob mob, BlockPos pos, ServerPlayer player);
    }
    public interface FenceUnleash {
        void apply(Mob mob, BlockPos pos); //, ServerPlayer player);
    }
    public interface FenceLeash {
        void apply(Mob mob, BlockPos pos, ServerPlayer player);
    }
}
