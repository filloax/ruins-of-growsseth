package com.ruslan.growsseth.structure

import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.http.ApiEvent
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.templates.BookData
import com.ruslan.growsseth.templates.BookTemplates
import kotlinx.serialization.json.Json
import net.minecraft.world.item.ItemStack

object StructureBooks {
    @JvmStatic
    fun bookIsTemplate(book: ItemStack) = BookTemplates.bookIsTemplate(book)

    @JvmStatic
    fun loadTemplate(book: ItemStack): ItemStack {
        return BookTemplates.loadTemplate(book, null, "${Constants.TEMPLATE_STRUCT_FOLDER}/", RemoteStructureBooks.replacementBooks::get)
    }
}

object RemoteStructureBooks {
    const val BOOK_EVENT_NAME = "structbook"

    val replacementBooks = mutableMapOf<String, BookData>()

    private val JSON = Json { isLenient = true }

    private fun isBookEvent(event: ApiEvent): Boolean {
        return event.name.split("/")[0] == BOOK_EVENT_NAME
    }

    private fun bookFromEvent(event: ApiEvent): Pair<String, BookData>? {
        val templateId = event.name.split("/").getOrNull(1) ?: run {
            RuinsOfGrowsseth.LOGGER.error("Remote structure book: no template id!")
            return null
        }
        val content = event.desc ?: run {
            RuinsOfGrowsseth.LOGGER.error("Remote structure book: no desc in $templateId!")
            return null
        }
        val bookData = try {
            JSON.decodeFromString(BookData.serializer(), content)
        } catch (e: Exception) {
            RuinsOfGrowsseth.LOGGER.error("Remote structure book: couldn't parse json for $templateId: ${e.message}")
            e.printStackTrace()
            return null
        }

        return templateId to bookData
    }

    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            replacementBooks.clear()
            replacementBooks.putAll(api.events.filter{ isBookEvent(it) && it.active }.mapNotNull(this::bookFromEvent))
        }
    }

    fun onServerStopped() {
        replacementBooks.clear()
    }
}

