package com.ruslan.growsseth.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class NameTagRenameEvent {
    /**
     * Triggers before a name tag is used, pass InteractionResult to override
     * the action (PASS makes it work as normal). Runs on server side only.
     */
    public static Event<Before> BEFORE = EventFactory.createArrayBacked(Before.class,
            (listeners) -> (target, name, player, stack, usedHand) -> {
                for (Before event : listeners) {
                    InteractionResultHolder<ItemStack> result = event.rename(target, name, player, stack, usedHand);

                    if (result.getResult() != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResultHolder.pass(ItemStack.EMPTY);
            }
    );

    /**
     * Triggers after a name tag is used. Runs on server side only.
     */
    public static Event<After> AFTER = EventFactory.createArrayBacked(After.class,
            (listeners) -> (target, name, player, stack, usedHand) -> {
                for (After event : listeners) {
                    event.rename(target, name, player, stack, usedHand);
                }
            }
    );

    public interface Before {
        InteractionResultHolder<ItemStack> rename(LivingEntity target, Component name, ServerPlayer player, ItemStack stack, InteractionHand usedHand);
    }

    public interface After {
        void rename(LivingEntity target, Component name, ServerPlayer player, ItemStack stack, InteractionHand usedHand);
    }
}
