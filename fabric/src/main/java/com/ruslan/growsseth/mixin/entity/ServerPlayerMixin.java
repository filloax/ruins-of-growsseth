package com.ruslan.growsseth.mixin.entity;

import com.ruslan.growsseth.entity.researcher.Researcher;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Unique
    ServerPlayer player = (ServerPlayer)(Object)this;

    @Inject(at = @At("HEAD"), method = "die")
    private void killedByResearcher(DamageSource damageSource, CallbackInfo ci){
        if (damageSource.getEntity() instanceof Researcher researcher)
            researcher.getCombat().onPlayerKilled(player);
    }
}
