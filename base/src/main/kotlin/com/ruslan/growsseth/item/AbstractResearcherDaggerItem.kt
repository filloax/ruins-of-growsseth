package com.ruslan.growsseth.item

import com.filloax.fxlib.api.platform.ServiceUtil
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.item.*
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import java.util.*

/**
 * To be implemented in loader-specific versions
 * that use their enchantment methods
 */
abstract class AbstractResearcherDaggerItem() : SwordItem(Tiers.DIAMOND, properties()){
    companion object {
        fun create(): AbstractResearcherDaggerItem {
            return ServiceUtil.findService(AbstractResearcherDaggerItem::class.java)
        }

        private fun properties() = Properties()
            .rarity(Rarity.EPIC)
            .attributes(SwordItem.createAttributes(Tiers.DIAMOND, 1, -1.5F))
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("${stack.descriptionId}.description")
            .withStyle(
                Style.EMPTY
                .applyFormat(ChatFormatting.GOLD)
            ))
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced)
    }

    protected fun allowEnchantment(
        stack: ItemStack,
        enchantment: Holder<Enchantment>,
    ): Optional<Boolean> {
        if (enchantment.unwrapKey().map { listOf(Enchantments.KNOCKBACK, Enchantments.SWEEPING_EDGE).contains(it) }.orElse(false))
            return Optional.of(false)
        if (enchantment == Enchantments.BREACH)
            return Optional.of(true)
        return Optional.empty()
    }
}