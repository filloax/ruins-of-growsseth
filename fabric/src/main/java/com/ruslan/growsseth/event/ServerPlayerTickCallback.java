package com.ruslan.growsseth.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public interface ServerPlayerTickCallback {
    Event<ServerPlayerTickCallback> EVENT = EventFactory.createArrayBacked(ServerPlayerTickCallback.class,
        (listeners) -> (player) -> {
            for (ServerPlayerTickCallback listener : listeners) {
                listener.postPlayerTick(player);
            }
        }
    );

    void postPlayerTick(ServerPlayer serverPlayer);
}
