package com.ruslan.growsseth.mixin.structurebook;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ruslan.growsseth.structure.StructureBooks;
import com.ruslan.growsseth.utils.MixinHelpers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ItemStackStructureBookMixin {
    @ModifyReturnValue(method = "of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private static ItemStack onCreateFromTag(ItemStack item) {
        if (
            MixinHelpers.placingBlockEntityInStructure
            && (item.is(Items.WRITABLE_BOOK) || item.is(Items.WRITTEN_BOOK))
        ) {
            if (StructureBooks.bookIsTemplate(item)) {
                 return StructureBooks.loadTemplate(item);
            }
        }
        return item;
    }
}
