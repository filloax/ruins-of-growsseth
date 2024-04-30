package com.ruslan.growsseth.structure

import com.filloax.fxlib.json.KotlinJsonResourceReloadListener
import com.filloax.fxlib.nbt.getListOrNull
import com.filloax.fxlib.setBookTags
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.entity.researcher.CustomRemoteDiaries
import com.ruslan.growsseth.http.ApiEvent
import com.ruslan.growsseth.http.GrowssethApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StructureBooks {
    const val PAGE_TEMPLATE_PREFIX = "%%TEMPLATE%%"
    const val DEFAULT_LANGUAGE = "en_us"

    private fun pageIsTemplate(page: String): Boolean {
        return page.trim().startsWith(PAGE_TEMPLATE_PREFIX)
    }

    private fun getTemplateIdFromPage(page: String): String {
        return page.trim().replace(PAGE_TEMPLATE_PREFIX, "").replace("\n", "")
    }

    fun templateExists(templateId: String): Boolean {
        val languageBooks = StructureBookListener.BOOKS_BY_LANG[GrowssethConfig.serverLanguage] ?: StructureBookListener.BOOKS_BY_LANG[DEFAULT_LANGUAGE]
        return languageBooks?.get(templateId) != null
    }

    fun getAvailableTemplates(): List<String> {
        val languageBooks = StructureBookListener.BOOKS_BY_LANG[GrowssethConfig.serverLanguage] ?: StructureBookListener.BOOKS_BY_LANG[DEFAULT_LANGUAGE]
        return languageBooks?.keys?.toList() ?: listOf()
    }

    @JvmStatic
    fun bookIsTemplate(book: ItemStack): Boolean {
        val tag = book.orCreateTag
        tag.getListOrNull("pages", Tag.TAG_STRING)?.let { pages ->
            return pages.size == 1 && pages[0]?.let{ pageIsTemplate(it.asString) } == true
        }
        return false
    }

    @JvmStatic
    fun loadTemplate(book: ItemStack): ItemStack {
        return loadTemplate(book, null)
    }

    fun loadTemplate(book: ItemStack, useTemplate: String?): ItemStack {
        val tag = book.orCreateTag
        val templateId = useTemplate ?: tag.getListOrNull("pages", Tag.TAG_STRING)?.let { pages ->
            pages[0]?.let { getTemplateIdFromPage(it.asString) }?.trim()
        } ?: run {
            RuinsOfGrowsseth.LOGGER.error("Book template loading error: cannot get template id")
            return book
        }

        val languageBooks = StructureBookListener.BOOKS_BY_LANG[GrowssethConfig.serverLanguage] ?: StructureBookListener.BOOKS_BY_LANG[DEFAULT_LANGUAGE]
        val remoteBooks = RemoteStructureBooks.replacementBooks
        val bookData = remoteBooks[templateId] ?: languageBooks?.get(templateId) ?: run {
            RuinsOfGrowsseth.LOGGER.error("Book template loading error: cannot find template with id $templateId")
            return book
        }

        val fixedBook = if (bookData.writable && book.`is`(Items.WRITTEN_BOOK)) {
            Items.WRITABLE_BOOK.defaultInstance
        } else if (!bookData.writable && book.`is`(Items.WRITABLE_BOOK)) {
            Items.WRITTEN_BOOK.defaultInstance
        } else {
            book
        }

        fixedBook.setBookTags(bookData.name, bookData.author ?: "???", bookData.pagesComponents)
        RuinsOfGrowsseth.LOGGER.info("Loaded book template $templateId in structure gen")
        return fixedBook
    }
}

@Serializable
data class BookData(
    val pages: List<String>,
    val name: String? = null,
    val author: String? = null,
    val writable: Boolean = false,
) {
    @Transient
    val pagesComponents = pages.map(Component::literal)
}

class StructureBookListener : KotlinJsonResourceReloadListener(JSON, Constants.STRUCTURE_BOOK_FOLDER) {
    companion object {
        private val JSON = Json

        val BOOKS_BY_LANG : MutableMap<String, MutableMap<String, BookData>> = mutableMapOf()
    }

    override fun apply(loader: Map<ResourceLocation, JsonElement>, manager: ResourceManager, profiler: ProfilerFiller) {
        BOOKS_BY_LANG.clear()

        loader.forEach { (fileIdentifier, jsonElement) ->
            try {
                val entry: BookData = JSON.decodeFromJsonElement(BookData.serializer(), jsonElement)
                val split = fileIdentifier.path.split("/")
                val langCode = split[0]
                // template id is file name
                val templateId = split[1]
                val books = BOOKS_BY_LANG.computeIfAbsent(langCode) { mutableMapOf() }
                val existing = books.put(templateId, entry)
                if (existing != null) {
                    RuinsOfGrowsseth.LOGGER.warn("Book template $templateId inserted but already existed: $existing")
                }
            } catch (e: Exception) {
                RuinsOfGrowsseth.LOGGER.error( "Growsseth: Couldn't parse structure book file {}", fileIdentifier, e)
            }
        }
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

