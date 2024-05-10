package com.ruslan.growsseth.mixin.block;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.ruslan.growsseth.config.ResearcherConfig;
import com.ruslan.growsseth.entity.researcher.Researcher;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @WrapWithCondition(method = "entityInside", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"))
    private boolean researcherDoesNotTeleport(Entity instance, ServerLevel destination){
        return !(instance instanceof Researcher) || !ResearcherConfig.researcherTeleports;
    }
}
