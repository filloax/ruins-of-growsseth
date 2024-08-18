package com.ruslan.growsseth.mixin.block;

import com.ruslan.growsseth.templates.SignTemplates;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
abstract public class SignBlockEntityMixin {
    @Unique
    SignBlockEntity thisSign = (SignBlockEntity)(Object)this;

    @Inject(at = @At("RETURN"), method = "loadAdditional")
    private void convertSign(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        // comment the if condition for testing in real time, but be aware that template signs will break at chunks reload
        if (MixinHelpers.placingBlockEntityInStructure) {
            SignTemplates.processSign(thisSign);
        }
    }
}
