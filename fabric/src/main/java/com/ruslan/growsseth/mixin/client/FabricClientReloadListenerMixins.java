package com.ruslan.growsseth.mixin.client;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.client.resource.EncryptedMusicResources;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixins to add Fabric's resource interfaces to the various reload listeners (client side)
 */
public class FabricClientReloadListenerMixins {
    @Mixin(EncryptedMusicResources.KeyListener.class)
    public static abstract class EncryptedMusicListenerMixin implements IdentifiableResourceReloadListener {
        @Override
        public ResourceLocation getFabricId() {
            return ResourceLocation.fromNamespaceAndPath(RuinsOfGrowsseth.MOD_ID, "sounds_key_listener");
        }
    }
}
