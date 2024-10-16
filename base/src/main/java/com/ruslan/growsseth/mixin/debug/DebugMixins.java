package com.ruslan.growsseth.mixin.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Lifecycle;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.utils.debug.LoggingList;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DebugMixins {
//    @Mixin(RegistryAccess.class)
    public static interface DebugMixin {
//            @Inject(
//        method = "method_41200",
//        at = @At("HEAD")
//    )
        // Result: TEST IS EXPERIMENTAL RegistryEntry[key=ResourceKey[minecraft:root / minecraft:dimension], value=Registry[ResourceKey[minecraft:root / minecraft:dimension] (Experimental)]]: Experimental
        private static void test(RegistryAccess.RegistryEntry<?> registryEntry, CallbackInfoReturnable<Lifecycle> cir) {
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

    @Mixin(ServerLevel.class)
    public static class ServerLevelMixin {
        @Inject(
            method = "onStructureStartsAvailable",
            at = @At("HEAD")
        )
        private void onLoadStruct(ChunkAccess chunk, CallbackInfo ci) {
            if (chunk.getPos().toLong() == -1 && !(chunk instanceof ProtoChunk)) {
                RuinsOfGrowsseth.getLOGGER().error("Loaded chunk with wrong index, server will probably error and crash soon."
                + "Happens occasionally after purchasing the golem house map, restarting the game should fix this."
                + "We're looking into a fix!\n"
                + Arrays.toString(Thread.currentThread().getStackTrace())
                );
            }
        }
    }

    /*
    RecurrentModificationException Debugging
    Enable to debug ConcurrentModificationExceptions that happen during entity load,
    as they normally are thrown after the loop (and so with the cause not in the call stack,
    making it harder to debug by itself)
     */
    /*
    @Mixin(PersistentEntitySectionManager.class)
    public static class PersistentEntitySectionManagerMixin {
        @WrapOperation(
            method = "method_31825",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/entity/EntitySection;getEntities()Ljava/util/stream/Stream;",
                ordinal = 2
            )
        )
        private Stream<?> onStopTrackingEnts(EntitySection<?> instance, Operation<Stream<?>> original) {
            MixinHelpers.savingPersistentEntities = true;
            RuinsOfGrowsseth.getLOGGER().info("BEGIN STOP TRACKING ENTS");
            return original.call(instance);
        }

        @WrapOperation(
            method = "method_31825",
            at = @At(
                value = "INVOKE",
                target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V",
                ordinal = 2
            )
        )
        private void afterForEach(Stream<?> instance, Consumer<?> consumer, Operation<Void> original) {
            RuinsOfGrowsseth.getLOGGER().info("BEGIN STOP TRACKING ENTS FOREACH");
            original.call(instance, consumer);
            RuinsOfGrowsseth.getLOGGER().info("END STOP TRACKING ENTS");
            MixinHelpers.savingPersistentEntities = false;
        }
    }

    @Mixin(ClassInstanceMultiMap.class)
    public static class ClassInstanceMultimapMixin<T> {
        @Shadow
        private List<T> allInstances;

        @Inject(
            method = "<init>",
            at = @At("RETURN")
        )
        private void onInit(Class baseClass, CallbackInfo ci) {
            if (this.allInstances instanceof ArrayList<T>)
                this.allInstances = new LoggingList<>(allInstances, () -> MixinHelpers.savingPersistentEntities);
        }

        @Inject(
                method = "add",
                at = @At("HEAD")
        )
        public void add(T object, CallbackInfoReturnable<Boolean> cir) {
            log("add", object, null);
        }

        @Inject(
                method = "remove",
                at = @At("HEAD")
        )
        public void remove(Object object, CallbackInfoReturnable<Boolean> cir) {
            log("remove", object, null);
        }

        @Unique
        private void log(String action, @Nullable Object value, @Nullable Object pos) {
            if (MixinHelpers.savingPersistentEntities) {
                if (value == null) {
                    RuinsOfGrowsseth.getLOGGER().info("DEBUG CLASS MULTIMAP | {}", action);
                } else if (pos == null) {
                    RuinsOfGrowsseth.getLOGGER().info("DEBUG CLASS MULTIMAP | {} | {}", action, value);
                } else {
                    RuinsOfGrowsseth.getLOGGER().info("DEBUG CLASS MULTIMAP | {} | {} at {}", action, value, pos);
                }
            }
        }
    }
    */
}
