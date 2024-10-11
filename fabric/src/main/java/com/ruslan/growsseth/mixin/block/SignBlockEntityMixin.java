package com.ruslan.growsseth.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ruslan.growsseth.templates.SignTemplates;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SignBlockEntity.class)
abstract public class SignBlockEntityMixin {
    @ModifyReturnValue(at = @At("RETURN"), method = "loadLines")
    private SignText convertSign(SignText original) {
        if (MixinHelpers.placingBlockEntityInStructure) {   // comment the condition for testing in real time
            return SignTemplates.processSign(original);
        }
        return original;
    }
}
