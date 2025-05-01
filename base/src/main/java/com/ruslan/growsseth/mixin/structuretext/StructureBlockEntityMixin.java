package com.ruslan.growsseth.mixin.structuretext;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntityMixin {
    @WrapOperation(
            method = "placeStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z")
    )
    private boolean placeStructure_structureTemplate_placeInWorld(StructureTemplate instance, ServerLevelAccessor blockentity1, BlockPos fluidstate, BlockPos blockstate, StructurePlaceSettings blockpos, RandomSource structuretemplate$structureblockinfo, int blockpos1, Operation<Boolean> original) {
        // Used to prevent template loading when loading structures from structure blocks
        MixinHelpers.loadingFromStructureBlock = true;
        boolean result = original.call(instance, blockentity1, fluidstate, blockstate, blockpos, structuretemplate$structureblockinfo, blockpos1);
        MixinHelpers.loadingFromStructureBlock = false;
        return result;      // (result isn't actually used)
    }
}
