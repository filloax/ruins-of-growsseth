package com.ruslan.growsseth.mixin.structurebook;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @WrapOperation(
        method="placeInWorld",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V")
    )
    private void placeInWorld_loadBlockEntity(BlockEntity instance, CompoundTag tag, HolderLookup.Provider registries, Operation<Void> original) {
        MixinHelpers.placingBlockEntityInStructure = true;
        try { // Potentially laggy? But want to be 100% sure
            original.call(instance, tag, registries);
        } finally {
            MixinHelpers.placingBlockEntityInStructure = false;
        }
    }
}
