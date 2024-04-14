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
                output.accept(GrowssethItems.DISC_SEGA_DI_NIENTE)
                output.accept(GrowssethItems.DISC_GIORGIO_CUBETTI)
                output.accept(GrowssethItems.DISC_GIORGIO_LOFI)
                output.accept(GrowssethItems.DISC_GIORGIO_LOFI_INST)
                output.accept(GrowssethItems.DISC_GIORGIO_FINDING_HOME)
                output.accept(GrowssethItems.DISC_BINOBINOOO)
                output.accept(GrowssethItems.DISC_BINOBINOOO_INST)
                output.accept(GrowssethItems.DISC_PADRE_MAMMONK)
                output.accept(GrowssethItems.DISC_ABBANDONATI)
                output.accept(GrowssethItems.DISC_MISSIVA_NELL_OMBRA)
                output.accept(GrowssethItems.DISC_MICE_ON_VENUS)
                output.accept(GrowssethItems.DISC_INFINITE_AMETHYST)
                output.accept(GrowssethItems.DISC_LABYRINTHINE)
            }.build()
    )

    fun registerCreativeModeTabs() {}
}
