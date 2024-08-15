package com.ruslan.growsseth.templates;

import net.minecraft.network.chat.Component
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.entity.SignText

object SignTemplates {
    const val LINE_TEMPLATE_PREFIX = "%%TEMPLATE%%"

    val templates get() = TemplateListener.signs()

    operator fun get(key: String) = templates[key]

    private val dyeColors = mapOf(
        "white" to DyeColor.WHITE,
        "orange" to DyeColor.ORANGE,
        "magenta" to DyeColor.MAGENTA,
        "light_blue" to DyeColor.LIGHT_BLUE,
        "yellow" to DyeColor.YELLOW,
        "lime" to DyeColor.LIME,
        "pink" to DyeColor.PINK,
        "gray" to DyeColor.GRAY,
        "light_gray" to DyeColor.LIGHT_GRAY,
        "cyan" to DyeColor.CYAN,
        "purple" to DyeColor.PURPLE,
        "blue" to DyeColor.BLUE,
        "brown" to DyeColor.BROWN,
        "green" to DyeColor.GREEN,
        "red" to DyeColor.RED,
        "black" to DyeColor.BLACK,
    )

    @JvmStatic
    fun getSignTemplate(templateId: String): SignText {
        if (templateExist(templateId)) {
            val template = templates[templateId]!!
            val signLines = template.linesComponents
            val signColor = dyeColors[template.color]?: DyeColor.BLACK
            val isGlowing = template.glowing!!

            val newLines = Array(4) { _ -> Component.empty() }
            for (i in 0..3) {       // extra lines in the json file will be ignored
                if (signLines.getOrNull(i) == null)
                    break
                newLines[i] = signLines[i].copy()
            }

            return SignText(newLines, newLines, signColor, isGlowing)
        }
        else {
            val errorLines = Array(4) { _ -> Component.empty() }
            errorLines[1] = Component.literal("NO TEMPLATE")
            return SignText(errorLines, errorLines, DyeColor.RED, true)
        }
    }

    private fun templateExist(templateId: String): Boolean {
        return templates[templateId] != null
    }
}
