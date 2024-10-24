package com.ruslan.growsseth.mixin.item;

import com.ruslan.growsseth.item.GrowssethJukeboxSongs;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(JukeboxPlayable.class)
public class DiscCreditsMixin {
    // Inside addToTooltip
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
        method = { "lambda$addToTooltip$1", "method_60748" },
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V")
    )
    private static void addExtraCredits(Consumer<Component> tooltipAdder, Holder<JukeboxSong> holder, CallbackInfo ci) {
        if (holder.unwrapKey().map(GrowssethJukeboxSongs.credits::containsKey).orElse(false)) {
            var credits = GrowssethJukeboxSongs.credits.get(holder.unwrapKey().orElseThrow());
            Optional.ofNullable(credits.getTooltip()).ifPresent(x -> x.forEach(tooltipAdder));
        }
    }
}
