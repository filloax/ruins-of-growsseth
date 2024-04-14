package com.ruslan.growsseth.mixin.item;

import com.ruslan.growsseth.item.ResearcherDaggerItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.SweepingEdgeEnchantment;
import org.spongepowered.asm.mixin.Mixin;

@Mixin (SweepingEdgeEnchantment.class)
public abstract class SweepingEdgeEnchantmentMixin extends Enchantment {
    protected SweepingEdgeEnchantmentMixin(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
        super(rarity, category, applicableSlots);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.getItem() instanceof ResearcherDaggerItem)
            return false;
        else
            return super.canEnchant(stack);
    }
}
