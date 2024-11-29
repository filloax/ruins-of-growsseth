package com.ruslan.growsseth.client.gui.locationtitles

import com.ruslan.growsseth.utils.notNull
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component

class LocationTitlesControllerTitle : LocationTitlesController {
    private val client: Minecraft = Minecraft.getInstance()

    override fun showLocationTitle(title: String) {
        val lang = Language.getInstance()
        val translated = lang.getOrDefault(title)
        val (titleComp, subtitleComp) = translated.split("-")
            .map{ Component.literal(it.trim()) }
            .let { Pair(it[0], it.getOrNull(1)) }
        if (notNull(subtitleComp)) client.gui.setSubtitle(subtitleComp)
        client.gui.setTitle(titleComp.withStyle(ChatFormatting.UNDERLINE))
    }

    override fun isShowingTitle(): Boolean {
        return false
    }
}