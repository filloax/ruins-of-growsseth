package com.ruslan.growsseth.mixin.structuretext;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @WrapOperation(
        method="placeInWorld",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/world/level/storage/ValueInput;)V")
    )
    private void placeInWorld_loadBlockEntity(BlockEntity instance, ValueInput input, Operation<Void> original) {
        // We want to keep the templates strings when placing structures through structure blocks
        if (!MixinHelpers.loadingFromStructureBlock) {
            MixinHelpers.placingBlockEntityInStructure = true;
            try { // Potentially laggy? But want to be 100% sure
                original.call(instance, input);
            } finally {
                MixinHelpers.placingBlockEntityInStructure = false;
            }
        }
        else {
            original.call(instance, input);
        }
    }
}
