package com.ruslan.growsseth.mixin.structurebook;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.structure.StructureBooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrame.class)
public abstract class ItemFrameStructureBookMixin {

    @WrapOperation(
        method = "readAdditionalSaveData",
        at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStack;of(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack convertDroppedBook(CompoundTag compoundTag, Operation<ItemStack> original){
        var item = original.call(compoundTag);
        if ((item.is(Items.WRITABLE_BOOK) || item.is(Items.WRITTEN_BOOK)) && StructureBooks.bookIsTemplate(item)) {
            return StructureBooks.loadTemplate(item);
        }
        return item;
    }
}