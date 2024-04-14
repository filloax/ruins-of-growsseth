package com.ruslan.growsseth

import com.ruslan.growsseth.utils.resLoc
import net.minecraft.world.level.block.entity.BannerPattern
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey

object GrowssethBannerPatterns {
    val all = mutableListOf<BannerTag>()

    val GROWSSETH = make("growsseth")

    private fun make(name: String): BannerTag {
        val pattern = BannerPattern(RuinsOfGrowsseth.MOD_ID + ":" + name)
        // Note: this needs the tag json to be defined, in data/*/pattern_item
        val tag = TagKey.create(Registries.BANNER_PATTERN, resLoc("pattern_item/$name"))
        val data = BannerTag(pattern, tag, name)
        all.add(data)
        return data
    }

    fun registerAll(registry: Registry<BannerPattern>) {
        all.forEach{
            Registry.register(registry, resLoc(it.name), it.bannerPattern)
        }
    }

    data class BannerTag(val bannerPattern: BannerPattern, val tag: TagKey<BannerPattern>, val name: String)
}