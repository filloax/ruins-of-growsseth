package com.ruslan.growsseth.mixin.structurebook;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ruslan.growsseth.structure.StructureBooks;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(ItemStack.class)
public abstract class ItemStackStructureBookMixin {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ModifyReturnValue(method = "parse", at = @At("RETURN"))
    private static Optional<ItemStack> onCreateFromTag(Optional<ItemStack> original) {
        if (MixinHelpers.placingBlockEntityInStructure) {
            return original.map(item -> {
                if (
                    (item.is(Items.WRITABLE_BOOK) || item.is(Items.WRITTEN_BOOK))
                    && StructureBooks.bookIsTemplate(item)
                ) {
                    return StructureBooks.loadTemplate(item);
                }
                return item;
            });
        }
        return original;
    }
}
