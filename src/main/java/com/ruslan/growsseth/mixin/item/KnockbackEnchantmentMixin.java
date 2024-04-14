package com.ruslan.growsseth.mixin.item;

import com.ruslan.growsseth.item.ResearcherDaggerItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.KnockbackEnchantment;
import org.spongepowered.asm.mixin.Mixin;

@Mixin (KnockbackEnchantment.class)
public abstract class KnockbackEnchantmentMixin extends Enchantment {
    protected KnockbackEnchantmentMixin(Rarity rarity, EnchantmentCategory category, EquipmentSlot[] applicableSlots) {
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
