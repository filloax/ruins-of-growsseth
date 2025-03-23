package com.ruslan.growsseth.mixin.debug;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import net.minecraft.core.WritableRegistry;
import org.spongepowered.asm.mixin.Unique;

/**
 * Turn off if not in development environment (via the mixins config)
 */
//@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
//    @Inject(
//            method = "loadElementFromResource",
//            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;")
//    )
    private static void loadRegistryContents(
            Decoder<?> instance, DynamicOps<?> ops, Object jsonElement, Operation<DataResult<?>> original,
            @Local(argsOnly = true) WritableRegistry<?> registry
    ) {
        registryWrapper(instance, ops, jsonElement, original, registry);
    }

//    @Inject(
//            method = "loadContentsFromNetwork",
//            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;")
//    )
    private static void loadRegistryContentsFromNetwork(
            Decoder<?> instance, DynamicOps<?> ops, Object jsonElement, Operation<DataResult<?>> original,
            @Local(argsOnly = true) WritableRegistry<?> registry
    ) {
        registryWrapper(instance, ops, jsonElement, original, registry);
    }

    @Unique
    private static void registryWrapper(Decoder<?> instance, DynamicOps<?> ops, Object jsonElement, Operation<DataResult<?>> original, WritableRegistry<?> registry) {
        RuinsOfGrowsseth.getLOGGER().info("Loading registry " + registry.key() + " - " + jsonElement);
    }
}
