package com.ruslan.growsseth.mixin.debug;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.ruslan.growsseth.RuinsOfGrowsseth;
import com.ruslan.growsseth.config.StructureConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class StructureDebugMixins {
    @Mixin(ChunkGenerator.class)
    public static class ChunkGeneratorMixin {
        @ModifyVariable(
            method = "tryGenerateStructure",
            at = @At("STORE")
        )
        private Predicate<Holder<Biome>> onSetBiomePredicate(Predicate<Holder<Biome>> value, @Local(argsOnly = true) StructureSet.StructureSelectionEntry structureSelectionEntry) {
            if (!StructureConfig.debugMode || !structureSelectionEntry.structure().unwrapKey().map(k -> k.location().getNamespace().equals(RuinsOfGrowsseth.MOD_ID)).orElse(false)) {
                return value;
            } else {
                return (h) -> true;
            }
        }
    }

    @Mixin(RegistryDataLoader.class)
    public static class RegistryDataLoaderMixin {
        @WrapOperation(
            method = "loadRegistryContents",
            at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;")
        )
        @SuppressWarnings("unchecked")
        private static DataResult<?> loadRegistryContents(
                Decoder<?> instance, DynamicOps<?> ops, Object jsonElement, Operation<DataResult<?>> original,
                @Local(argsOnly = true) ResourceKey<? extends Registry<?>> registryKey
        ) {
            var result = original.call(instance, ops, jsonElement);
            if (StructureConfig.debugMode && registryKey.equals(Registries.STRUCTURE_SET) && jsonElement.toString().contains("growsseth")) {
                RuinsOfGrowsseth.getLOGGER().info("(debug mode) Increasing spawn frequency for {}", jsonElement);
                StructureSet structureSet = (StructureSet) result.getOrThrow(false, string -> {});
                var placement = structureSet.placement();
                placement.frequency = 1;
                if (placement instanceof RandomSpreadStructurePlacement randomSpread) {
                    randomSpread.spacing = randomSpread.spacing / 4;
                    randomSpread.separation = randomSpread.separation / 4;
                }
                result = DataResult.success(structureSet);
            }
            return result;
        }
    }

    @Mixin(FlatLevelSource.class)
    public static abstract class FlatLevelSourceMixin extends ChunkGenerator {
        protected FlatLevelSourceMixin(BiomeSource biomeSource) {
            super(biomeSource);
        }

        protected FlatLevelSourceMixin(BiomeSource biomeSource, Function<Holder<Biome>, BiomeGenerationSettings> generationSettingsGetter) {
            super(biomeSource, generationSettingsGetter);
        }

        @Inject(
            method = "createState(Lnet/minecraft/core/HolderLookup;Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;",
            at = @At("HEAD"),
            cancellable = true
        )
        private void overrideStructuresInDebug(HolderLookup<StructureSet> structureSetLookup, RandomState randomState, long seed, CallbackInfoReturnable<ChunkGeneratorStructureState> cir) {
            if (StructureConfig.debugMode) {
                RuinsOfGrowsseth.getLOGGER().info("(debug mode) Replaced flat worldgen structure selection");
                cir.setReturnValue(super.createState(structureSetLookup, randomState, seed));
            }
        }
    }

    @Mixin(ChunkGeneratorStructureState.class)
    public static class ChunkGeneratorStructureStateMixin {
        @Inject(
            method = "hasBiomesForStructureSet",
            at = @At("HEAD"),
            cancellable = true
        )
        private static void hasBiomesForStructureSet(StructureSet structureSet, BiomeSource biomeSource, CallbackInfoReturnable<Boolean> cir) {
            if (StructureConfig.debugMode) {
                cir.setReturnValue(true);
            }
        }
    }
}
