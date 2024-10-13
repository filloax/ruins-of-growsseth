package com.ruslan.growsseth.mixin.growssethworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.worldgen.GrowssethModBiomeSources;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldDimensions.class)
public class WorldDimensionsMixin {
    @WrapOperation(
        method = "isStableOverworld",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/MultiNoiseBiomeSource;stable(Lnet/minecraft/resources/ResourceKey;)Z"
        )
    )
    private static boolean stableOverworldForGrowsseth(MultiNoiseBiomeSource instance, ResourceKey<MultiNoiseBiomeSourceParameterList> resourceKey, Operation<Boolean> original) {
        return original.call(instance, resourceKey)
            || original.call(instance, GrowssethModBiomeSources.GROWSSETH_OVERWORLD_SETTINGS);
    }
}
