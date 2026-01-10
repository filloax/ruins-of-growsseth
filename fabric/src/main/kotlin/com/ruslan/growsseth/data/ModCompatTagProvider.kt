package com.ruslan.growsseth.data

import com.cobblemon.mod.common.CobblemonItems
import com.ruslan.growsseth.GrowssethTags
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider.ItemTagProvider
import net.fabricmc.fabric.impl.datagen.FabricTagBuilder
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import java.util.concurrent.CompletableFuture

class ModCompatTagProviderItems(output: FabricDataOutput, val registries: CompletableFuture<HolderLookup.Provider>) :
    ItemTagProvider(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        // everything except enigma and starf
        valueLookupBuilder(GrowssethTags.COBBLEMON_BERRIES_EXCEPT_RARE)
            .addOptionalTag(cobbleTag("berries/damage_reduction"))
            .addOptionalTag(cobbleTag("berries/damaging"))
            .addOptionalTag(cobbleTag("berries/friendship"))
            // hp recovery contains enigma
            /* disabled because of compile errors
            .addOptional(CobblemonItems.ORAN_BERRY)
            .addOptional(CobblemonItems.SITRUS_BERRY)
            */
            .addOptionalTag(cobbleTag("berries/nature_recovery"))
            .addOptionalTag(cobbleTag("berries/non_battle"))
            .addOptionalTag(cobbleTag("berries/pp_recovery"))
            // stat buff contains starf
            /* disabled because of compile errors
            .addOptional(CobblemonItems.KEE_BERRY)
            .addOptional(CobblemonItems.MARANGA_BERRY)
            .addOptional(CobblemonItems.LIECHI_BERRY)
            .addOptional(CobblemonItems.GANLON_BERRY)
            .addOptional(CobblemonItems.SALAC_BERRY)
            .addOptional(CobblemonItems.PETAYA_BERRY)
            .addOptional(CobblemonItems.APICOT_BERRY)
            .addOptional(CobblemonItems.LANSAT_BERRY)
            .addOptional(CobblemonItems.MICLE_BERRY)
            .addOptional(CobblemonItems.CUSTAP_BERRY)
             */
            .addOptionalTag(cobbleTag("berries/status_recovery"))

    }

    private fun FabricTagProvider<Item>.addOptional(item: ItemLike): FabricTagBuilder
        = this.addOptional(item)

    private fun cobbleTag(id: String) =
        TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("cobblemon", id))

    override fun getName(): String = "ModCompatTagProviderItems"
}