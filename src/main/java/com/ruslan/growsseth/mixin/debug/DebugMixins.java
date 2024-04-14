package com.ruslan.growsseth.mixin.debug;

import com.mojang.serialization.Lifecycle;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class DebugMixins {
//    @Mixin(RegistryAccess.class)
    public static interface DebugMixin {
//            @Inject(
//        method = "method_41200",
//        at = @At("HEAD")
//    )
        // Result: TEST IS EXPERIMENTAL RegistryEntry[key=ResourceKey[minecraft:root / minecraft:dimension], value=Registry[ResourceKey[minecraft:root / minecraft:dimension] (Experimental)]]: Experimental
        private static void test(RegistryAccess.RegistryEntry registryEntry, CallbackInfoReturnable<Lifecycle> cir) {
            RuinsOfGrowsseth.getLOGGER().info("TEST IS EXPERIMENTAL " + registryEntry + ": " + registryEntry.value().registryLifecycle());
        }
    }

//    @Mixin(MappedRegistry.class)
    public static class MappedRegistryMixin {
//        @Inject(
//            method = "registerMapping(ILnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/Holder$Reference;",
//            at = @At("HEAD")
//        )
        private <T> void test(int id, ResourceKey<T> key, T value, Lifecycle lifecycle, CallbackInfoReturnable<Holder.Reference<T>> cir) {
            RuinsOfGrowsseth.getLOGGER().info("TEST IS EXPERIMENTAL " + key + ": " + lifecycle);
        }
    }
}
