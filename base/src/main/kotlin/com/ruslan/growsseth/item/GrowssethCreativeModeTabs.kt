package com.ruslan.growsseth.item

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.utils.resLoc
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType


object GrowssethCreativeModeTabs {
    private val all = mutableMapOf<ResourceLocation, CreativeModeTab>()

    val GROWSSETH_ITEMS = make("growsseth", CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("growsseth.creative_tab"))
        .icon{ItemStack(GrowssethItems.RESEARCHER_HORN)}
        .displayItems{ _, output ->
            output.accept(GrowssethItems.RESEARCHER_SPAWN_EGG)
            output.accept(GrowssethItems.ZOMBIE_RESEARCHER_SPAWN_EGG)
            output.accept(GrowssethItems.RESEARCHER_DAGGER)
            output.accept(GrowssethItems.RESEARCHER_HORN)
            output.accept(GrowssethItems.GROWSSETH_BANNER_PATTERN)
            output.accept(GrowssethItems.GROWSSETH_ARMOR_TRIM)
            output.accept(GrowssethItems.GROWSSETH_POTTERY_SHERD)
            output.accept(GrowssethItems.FRAGMENT_BALLATA_DEL_RESPAWN)
            for (disc in GrowssethItems.DISCS_ORDERED) {
                output.accept(disc)
            }
        }.build()
    )

    fun registerCreativeModeTabs(registrator: (ResourceLocation, CreativeModeTab) -> Unit) {
        all.forEach { t, u -> registrator(t, u) }
    }

    private fun make(id: String, creativeModeTab: CreativeModeTab): CreativeModeTab {
        all[resLoc(id)] = creativeModeTab
        return creativeModeTab
    }
}
