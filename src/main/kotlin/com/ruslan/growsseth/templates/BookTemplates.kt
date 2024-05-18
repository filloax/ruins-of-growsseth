package com.ruslan.growsseth.templates

import com.filloax.fxlib.api.getBookText
import com.filloax.fxlib.api.setBookTags
import com.ruslan.growsseth.Constants
import com.ruslan.growsseth.RuinsOfGrowsseth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object BookTemplates {
    const val PAGE_TEMPLATE_PREFIX = "%%TEMPLATE%%"

    private fun pageIsTemplate(page: String): Boolean {
        return page.trim().startsWith(PAGE_TEMPLATE_PREFIX)
    }

    private fun getTemplateIdFromPage(page: String): String {
        return page.trim().replace(PAGE_TEMPLATE_PREFIX, "").replace("\n", "")
    }

    fun templateExists(templateId: String): Boolean {
        return TemplateListener.books()[templateId] != null
    }

    fun getAvailableTemplates(): List<String> {
        val languageBooks = TemplateListener.books()
        val defaultLanguageBooks = TemplateListener.books(Constants.DEFAULT_LANGUAGE)
        return (languageBooks.keys + defaultLanguageBooks.keys).toList()
    }

    @JvmStatic
    fun bookIsTemplate(book: ItemStack): Boolean {
        val bookPages = book.getBookText()
        return bookPages.size == 1 && pageIsTemplate(bookPages.getOrNull(0)?.string ?: "")
    }

    fun loadTemplate(book: ItemStack, useTemplate: String? = null, prefix: String = "", override: ((String) -> BookData?)? = null): ItemStack {
        val bookPages = book.getBookText()
        val templateId = prefix + (useTemplate ?: bookPages.getOrNull(0)?.let { getTemplateIdFromPage(it.string) }?.trim()
            ?: run {
                RuinsOfGrowsseth.LOGGER.error("Book template loading error: cannot get template id")
                return book
            })

        val languageBooks = TemplateListener.books()
        val bookData = override?.let { it(templateId) } ?: languageBooks[templateId] ?: run {
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
        RuinsOfGrowsseth.LOGGER.info("Loaded book template $templateId")
        return fixedBook
    }
}




