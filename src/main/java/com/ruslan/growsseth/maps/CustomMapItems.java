package com.ruslan.growsseth.maps;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;

import java.util.Optional;

public class CustomMapItems {
    /**
     * Replace checks for Items.FILLED_MAP in various classes with any item that extends MapItem.
     * @param itemStack Item instance to check for
     * @param compareItem Item that was used in the check in the original call
     * @return True if the item should be treated like a map.
     */
    public static Optional<Boolean> checkCustomMapItem(ItemStack itemStack, Item compareItem) {
        if (compareItem == Items.FILLED_MAP) {
            return Optional.of(itemStack.getItem() instanceof MapItem);
        }
        return Optional.empty();
    }

    public static boolean checkMapItemWrapper(ItemStack instance, Item item, Operation<Boolean> original) {
        var opt = CustomMapItems.checkCustomMapItem(instance, item);
        if (opt.isPresent()) return opt.orElseThrow();
        return original.call(instance, item);
    }
}
