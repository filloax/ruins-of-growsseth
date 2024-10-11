package com.ruslan.growsseth.mixin.structurebook;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ruslan.growsseth.structure.StructureBooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ItemFrame.class)
public abstract class ItemFrameStructureBookMixin {
    @Unique
    ItemFrame thisItemFrame = (ItemFrame)(Object)this;

    @WrapOperation(
        method = "readAdditionalSaveData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;parse(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Ljava/util/Optional;"
        )
    )
    private Optional<ItemStack> convertDroppedBook(HolderLookup.Provider lookupProvider, Tag tag, Operation<Optional<ItemStack>> original){
        var itemOpt = original.call(lookupProvider, tag);
        return itemOpt.map(item -> {
            if (
                (item.is(Items.WRITABLE_BOOK) || item.is(Items.WRITTEN_BOOK))
                && StructureBooks.bookIsTemplate(item)
            ) {
                return StructureBooks.loadTemplate(item);
            }
            return item;
        });
    }
}
