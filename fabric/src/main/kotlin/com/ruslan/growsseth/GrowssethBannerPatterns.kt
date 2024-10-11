package com.ruslan.growsseth

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.world.level.block.entity.BannerPattern
import net.minecraft.core.registries.Registries
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey

object GrowssethBannerPatterns {
    val all = mutableListOf<BannerTag>()

    // Data since 1.20.5

    val GROWSSETH = make("growsseth")

    private fun make(name: String): BannerTag {
        val id = resLoc(name)
        val pattern = BannerPattern(id, "item.${id.toLanguageKey()}_banner_pattern.desc")
        // Note: this needs the tag json to be defined, in data/*/pattern_item
        val tag = TagKey.create(Registries.BANNER_PATTERN, resLoc("pattern_item/$name"))
        val data = BannerTag(pattern, tag, name)
        all.add(data)
        return data
    }

    fun bootstrap(context: BootstrapContext<BannerPattern>) {
        all.forEach{ banner ->
            context.register(banner.id(), banner.bannerPattern)
        }
    }

    data class BannerTag(val bannerPattern: BannerPattern, val tag: TagKey<BannerPattern>, val name: String) {
        fun id(): ResourceKey<BannerPattern> = ResourceKey.create(Registries.BANNER_PATTERN, resLoc(name))
    }
}