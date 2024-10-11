package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class ServerEntityLifecycleEvents {
    private ServerEntityLifecycleEvents() {}

    public static Event<Destroyed> ENTITY_DESTROYED = EventFactory.createArrayBacked(Destroyed.class,
        (listeners) -> (entity, level) -> {
            for (Destroyed listener : listeners)
                listener.apply(entity, level);
        }
    );

    public interface Destroyed {
        void apply(Entity entity, ServerLevel level);
    }
}
