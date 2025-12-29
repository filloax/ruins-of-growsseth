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
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments
import java.util.*
import java.util.function.Consumer

/**
 * To be implemented in loader-specific versions
 * that use their enchantment methods
 */
abstract class AbstractResearcherDaggerItem() : Item(
    getProperties()) {

    companion object {
        fun create(): AbstractResearcherDaggerItem {
            return ServiceUtil.findService(AbstractResearcherDaggerItem::class.java)
        }

        // todo: manage in GrowssethItems
        private fun getProperties() = Properties()
            .setId(ResourceKey.create<Item>(Registries.ITEM, resLoc("researcher_dagger")))   // todo: unify
            .rarity(Rarity.EPIC)
            .sword(ToolMaterial.DIAMOND, 1f, -1.5F)
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipDisplay: TooltipDisplay,
        tooltipAdder: Consumer<Component>,
        isAdvanced: TooltipFlag
    ) {
        tooltipAdder.accept(
            Component.translatable("${stack.itemName}.description")
            .withStyle(
                Style.EMPTY
                .applyFormat(ChatFormatting.GOLD)
            ))
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, isAdvanced)
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