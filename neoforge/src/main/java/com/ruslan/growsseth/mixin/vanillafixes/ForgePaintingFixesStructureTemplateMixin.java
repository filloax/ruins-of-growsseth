package com.ruslan.growsseth.mixin.vanillafixes;

import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Big thanks to Naz Ikhsan from https://bugs.mojang.com/browse/MC-102223 for the painting fix
@Mixin(StructureTemplate.class)
public abstract class ForgePaintingFixesStructureTemplateMixin {
    // Injects into StructureTemplate#placeEntities, inside the lambda of createEntityIgnoreException

    @Inject(
        // placeEntities in vanilla, addEntitiesToWorld in neoforge
        method = {"lambda$addEntitiesToWorld$5"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveTo(DDDFF)V", shift = At.Shift.AFTER)
    )
    private static void fixPaintingPlacementVanilla(StructurePlaceSettings placementIn, Vec3 pos, ServerLevelAccessor level, Entity entity, CallbackInfo ci) {
        MixinHelpers.fixPaintingPlacement(entity);
    }
}