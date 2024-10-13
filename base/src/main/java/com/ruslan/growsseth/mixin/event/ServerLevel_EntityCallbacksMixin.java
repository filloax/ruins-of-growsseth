package com.ruslan.growsseth.mixin.event;

import com.ruslan.growsseth.events.ServerEntityLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.EntityCallbacks.class)
public abstract class ServerLevel_EntityCallbacksMixin {
    @Final
    @Shadow
    ServerLevel field_26936;

    @Inject(method = "onDestroyed(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void onDestroyed(Entity entity, CallbackInfo ci) {
        ServerEntityLifecycleEvents.ENTITY_DESTROYED.invoker().apply(entity, field_26936);
    }
}
