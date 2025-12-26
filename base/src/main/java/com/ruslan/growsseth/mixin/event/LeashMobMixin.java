package com.ruslan.growsseth.mixin.event;

import com.ruslan.growsseth.events.Events;
import com.ruslan.growsseth.events.FenceLeashEvent;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Leashable.class)
public interface LeashMobMixin {
    @Inject(method = "dropLeash()V", at = @At("HEAD"))
    private void onUnleash(CallbackInfo ci) {
        Mob th1s = (Mob)(Object)this;
        if (
            th1s.getLeashHolder() != null
            && th1s.getLeashHolder() instanceof LeashFenceKnotEntity knot
            && !th1s.level().isClientSide()
        ) {
            Events.FENCE_UNLEASH.invoke(new FenceLeashEvent.Unleash(th1s, knot.getPos()));
        }
    }
}
