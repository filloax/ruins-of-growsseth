package com.ruslan.growsseth.mixin.item;

import com.ruslan.growsseth.item.PotPatternRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DecoratedPotPatterns.class)
public abstract class DecoratedPotPatternsMixin {
    @Inject(method = "getResourceKey", at = @At("HEAD"), cancellable = true)
    private static void getCustomPotPattern(Item item, CallbackInfoReturnable<ResourceKey<String>> cir) {
        ResourceKey<String> customTextureKey = PotPatternRegistry.getForItem(item);
        if (customTextureKey != null) {
            cir.setReturnValue(customTextureKey);
        }
    }
}
