package com.ruslan.growsseth.item

import com.filloax.fxlib.api.platform.ServiceUtil
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.*
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import java.util.*

/**
 * To be implemented in loader-specific versions
 * that use their enchantment methods
 */
abstract class AbstractResearcherDaggerItem() : SwordItem(
    ToolMaterial.DIAMOND, 1f, -1.5F, getProperties()) {

    companion object {
        fun create(): AbstractResearcherDaggerItem {
            return ServiceUtil.findService(AbstractResearcherDaggerItem::class.java)
        }

        // todo: manage in GrowssethItems
        private fun getProperties() = Properties()
            .setId(ResourceKey.create<Item>(Registries.ITEM, resLoc("researcher_dagger")))   // todo: unify
            .rarity(Rarity.EPIC)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        isAdvanced: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("${stack.itemName}.description")
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