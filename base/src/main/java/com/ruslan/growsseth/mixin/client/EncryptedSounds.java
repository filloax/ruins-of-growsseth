package com.ruslan.growsseth.mixin.client;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.ruslan.growsseth.client.resource.EncryptableSound;
import com.ruslan.growsseth.client.resource.EncryptedMusicResources;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.io.InputStream;
import java.util.Map;

/**
 * See [EncryptedMusicResources].
 */
public class EncryptedSounds {
    @Mixin(targets = "net.minecraft.client.sounds.SoundManager$Preparations")
    public static class SoundManager_PreparationsMixin {
        @ModifyExpressionValue(
            method = "listResources",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resources/FileToIdConverter;listMatchingResources(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/Map;"
            )
        )
        private Map<ResourceLocation, Resource> listResourcesWithExtras(Map<ResourceLocation, Resource> original, @Local(argsOnly = true) ResourceManager resourceManager) {
            var additionalResources = EncryptedMusicResources.LISTER.listMatchingResources(resourceManager);
            original.putAll(additionalResources);
            return original;
        }
    }

    @Mixin(SoundEventRegistrationSerializer.class)
    public static class SoundEventRegistrationSerializerMixin {
        @ModifyReturnValue(
            method = "getSound",
            at = @At("RETURN")
        )
        private Sound onGetSound(Sound sound, @Local(argsOnly = true)JsonObject jsonObject) {
            boolean isEncrypted = GsonHelper.getAsBoolean(jsonObject, "encrypted", false);
            ((EncryptableSound) sound).ruins_of_growsseth$setEncrypted(isEncrypted);
            return sound;
        }
    }

    @Mixin(Sound.class)
    public static class SoundMixin implements EncryptableSound {
        @Unique
        private boolean encrypted = false;
        @Shadow
        private @Final ResourceLocation location;

        @Override
        public boolean ruins_of_growsseth$isEncrypted() {
            return encrypted;
        }

        @Override
        public void ruins_of_growsseth$setEncrypted(boolean value) {
            encrypted = value;
        }

        @ModifyReturnValue(
            method = "getPath",
            at = @At("RETURN")
        )
        private ResourceLocation onGetPath(ResourceLocation original) {
            if (encrypted) {
                return EncryptedMusicResources.LISTER.idToFile(this.location);
            }
            return original;
        }
    }

    @Mixin(SoundBufferLibrary.class)
    public static class SoundBufferLibraryMixin {
        @WrapOperation(
                method = "method_19745",
                at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceProvider;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;"
                )
        )
        private InputStream wrapSoundReadingStream(ResourceProvider instance, ResourceLocation resourceLocation, Operation<InputStream> original) {
            return checkEncryptedSoundStream(resourceLocation, original.call(instance, resourceLocation));
        }

        @WrapOperation(
                method = "method_19747",
                at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/ResourceProvider;open(Lnet/minecraft/resources/ResourceLocation;)Ljava/io/InputStream;"
                )
        )
        private InputStream wrapSoundReadingStreamBuffer(ResourceProvider instance, ResourceLocation resourceLocation, Operation<InputStream> original) {
            return checkEncryptedSoundStream(resourceLocation, original.call(instance, resourceLocation));
        }

        @Unique
        private InputStream checkEncryptedSoundStream(ResourceLocation resourceLocation, InputStream inputStream) {
            return EncryptedMusicResources.checkEncryptedSoundStream(resourceLocation, inputStream);
        }
    }
}
