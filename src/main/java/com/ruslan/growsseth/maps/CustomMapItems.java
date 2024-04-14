package com.ruslan.growsseth.maps;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;

public class CustomMapItems {
    /**
     * Replace checks for Items.FILLED_MAP in various classes with any item that extends MapItem.
     * @param itemStack Item instance to check for
     * @param compareItem Item that was used in the check in the original call
     * @return True if the item should be treated like a map.
     */
    public static boolean checkCustomMapItem(ItemStack itemStack, Item compareItem) {
        if (compareItem == Items.FILLED_MAP) {
            return itemStack.getItem() instanceof MapItem;
        }
        return itemStack.is(compareItem);
    }
}
