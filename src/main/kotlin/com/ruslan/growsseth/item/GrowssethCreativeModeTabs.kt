package com.ruslan.growsseth.item

import com.ruslan.growsseth.RuinsOfGrowsseth
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack


object GrowssethCreativeModeTabs {

    val GROWSSETH_ITEMS: CreativeModeTab = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        ResourceLocation(RuinsOfGrowsseth.MOD_ID, "growsseth"),
        FabricItemGroup.builder().title(Component.translatable("growsseth.creative_tab"))
            .icon{ItemStack(GrowssethItems.RESEARCHER_HORN)}
            .displayItems{ params, output ->
                output.accept(GrowssethItems.RESEARCHER_SPAWN_EGG)
                output.accept(GrowssethItems.ZOMBIE_RESEARCHER_SPAWN_EGG)
                output.accept(GrowssethItems.RESEARCHER_DAGGER)
                output.accept(GrowssethItems.RESEARCHER_HORN)
                output.accept(GrowssethItems.GROWSSETH_BANNER_PATTERN)
                output.accept(GrowssethItems.GROWSSETH_ARMOR_TRIM)
                output.accept(GrowssethItems.GROWSSETH_POTTERY_SHERD)
                for (disc in GrowssethItems.DISCS_ORDERED) {
                    output.accept(disc)
                }
            }.build()
    )

    fun registerCreativeModeTabs() {}
}
