package com.ruslan.growsseth.mixin;

import com.ruslan.growsseth.event.ServerPlayerTickCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerFabricMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void postServerTick(CallbackInfo ci) {
        Player th1s = (Player)(Object)this;
        if (th1s instanceof ServerPlayer sp) {
            ServerPlayerTickCallback.EVENT.invoker().postPlayerTick(sp);
        }
    }
}
