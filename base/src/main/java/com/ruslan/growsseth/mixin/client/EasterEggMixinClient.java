package com.ruslan.growsseth.mixin.client;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.interfaces.ZombieWithEasterEgg;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class EasterEggMixinClient {
    @Mixin(AbstractZombieRenderer.class)
    public static class AbstractZombieRendererMixin {
        @Unique
        private static final Identifier GUBER_ZOMBIE_PATH = Identifier.fromNamespaceAndPath(RuinsOfGrowsseth.MOD_ID, "textures/entity/zombie/guber_zombie.png");

        @Inject(
            method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true
        )
        private void onGetTexture(ZombieRenderState renderState, CallbackInfoReturnable<Identifier> cir) {
            /* todo: logic must be changed after the introduction of render states in 1.21.2
            boolean isGuber = ((ZombieWithEasterEgg) renderState).gr$isGuber();

            if (isGuber) {
                cir.setReturnValue(GUBER_ZOMBIE_PATH);
            }
             */
        }
    }
}
