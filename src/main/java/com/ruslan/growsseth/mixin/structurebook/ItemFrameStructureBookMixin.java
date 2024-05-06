package com.ruslan.growsseth.mixin.structurebook;

import com.ruslan.growsseth.structure.StructureBooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrame.class)
public abstract class ItemFrameStructureBookMixin {
    @Unique
    ItemFrame thisItemFrame = (ItemFrame)(Object)this;

    @Inject(method = "dropItem(Lnet/minecraft/world/entity/Entity;Z)V", at=@At("HEAD"))
    private void convertDroppedBook(Entity entity, boolean dropSelf, CallbackInfo ci){
        ItemStack item = thisItemFrame.getItem();
        if (item.is(Items.WRITABLE_BOOK) || item.is(Items.WRITTEN_BOOK)){
            if (StructureBooks.bookIsTemplate(item)) {
                thisItemFrame.setItem(StructureBooks.loadTemplate(item));
            }
        }
    }
}
