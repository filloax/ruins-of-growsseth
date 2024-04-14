package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

public interface PlayerAdvancementEvent {
    Event<PlayerAdvancementEvent> EVENT = EventFactory.createArrayBacked(PlayerAdvancementEvent.class,
        listeners -> (player, advancement, criterionKey) -> {
            for (PlayerAdvancementEvent listener : listeners) {
                listener.apply(player, advancement, criterionKey);
            }
        }
    );

    void apply(ServerPlayer player, AdvancementHolder advancement, String criterionKey);
}
