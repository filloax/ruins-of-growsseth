package com.ruslan.growsseth.mixin.client;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.interfaces.ZombieWithEasterEgg;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class EasterEggMixinClient {
    @Mixin(AbstractZombieRenderer.class)
    public static class AbstractZombieRendererMixin {
        @Unique
        private static final ResourceLocation GUBER_ZOMBIE_PATH = new ResourceLocation(RuinsOfGrowsseth.MOD_ID, "textures/entity/zombie/guber_zombie.png");

        @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/monster/Zombie;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"),
            cancellable = true
        )
        private void onGetTexture(Zombie zombie, CallbackInfoReturnable<ResourceLocation> ci) {
            boolean isGuber = ((ZombieWithEasterEgg) zombie).gr$isGuber();

            if (isGuber) {
                ci.setReturnValue(GUBER_ZOMBIE_PATH);
            }
        }
    }
}
