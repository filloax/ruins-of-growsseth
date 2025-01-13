package com.ruslan.growsseth.mixin.vanillafixes;

import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Big thanks to Naz Ikhsan from https://bugs.mojang.com/browse/MC-102223 for the painting fix
@Mixin(StructureTemplate.class)
public abstract class PaintingFixesStructureTemplateMixin {
    // Injects into StructureTemplate#placeEntities, inside the lambda of createEntityIgnoreException

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(
        // placeEntities in vanilla, addEntitiesToWorld in neoforge
        method = {"method_17917", "lambda$placeEntities$5"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;moveTo(DDDFF)V", shift = At.Shift.AFTER)
    )
    private static void fixPaintingPlacementVanilla(Rotation rotation, Mirror mirror, Vec3 vec3, boolean bl, ServerLevelAccessor serverLevelAccessor, Entity entity, CallbackInfo ci) {
        MixinHelpers.fixPaintingPlacement(entity);
    }
}