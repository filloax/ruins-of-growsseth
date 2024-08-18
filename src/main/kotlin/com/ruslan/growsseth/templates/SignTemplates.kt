package com.ruslan.growsseth.templates;

import net.minecraft.network.chat.Component
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText

object SignTemplates {
    private const val LINE_TEMPLATE_PREFIX = "%TEMPLATE%"   // different from books to allow templates in hanging signs

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
    fun processSign(sign: SignBlockEntity) {
        // If first line is template prefix, the other lines get concatenated for the template id
        val frontMessages: Array<Component> = sign.getText(true).getMessages(false)
        val backMessages: Array<Component> = sign.getText(false).getMessages(false)

        if (frontMessages[0].string == LINE_TEMPLATE_PREFIX) {
            val templateIdFront = frontMessages[1].string + frontMessages[2].string + frontMessages[3].string
            val newFrontText = getSignTemplate(templateIdFront)
            sign.setText(newFrontText, true)
        }
        if (backMessages[0].string == LINE_TEMPLATE_PREFIX) {
            val templateIdBack = backMessages[1].string + backMessages[2].string + backMessages[3].string
            val newBackText = getSignTemplate(templateIdBack)
            sign.setText(newBackText, false)
        }
    }

    private fun getSignTemplate(templateId: String): SignText {
        if (templateExists(templateId)) {
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
            errorLines[1] = Component.literal("TEMPLATE")
            errorLines[2] = Component.literal("NOT FOUND")
            return SignText(errorLines, errorLines, DyeColor.RED, true)
        }
    }

    private fun templateExists(templateId: String): Boolean {
        return templates[templateId] != null
    }
}
