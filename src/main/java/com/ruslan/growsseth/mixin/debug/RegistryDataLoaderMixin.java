package com.ruslan.growsseth.mixin.debug;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Decoder;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Turn off if not in development environment (via the mixins config)
 */
//@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
//    @Inject(
//            method = "loadRegistryContents",
//            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;")
//    )
    private static void loadRegistryContents(
            RegistryOps.RegistryInfoLookup lookup, ResourceManager manager, ResourceKey<?> registryKey, WritableRegistry<?> registry,
            Decoder<?> decoder, Map<ResourceKey<?>, Exception> exceptions,
            CallbackInfo ci,
            @Local JsonElement jsonElement
    ) {
        RuinsOfGrowsseth.getLOGGER().info("Loading registry " + registryKey + " - " + jsonElement);
    }
}
