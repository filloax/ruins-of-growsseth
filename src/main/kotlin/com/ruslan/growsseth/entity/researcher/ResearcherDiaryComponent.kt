package com.ruslan.growsseth.entity.researcher

import com.filloax.fxlib.api.*
import com.filloax.fxlib.api.codec.mutableMapCodec
import com.filloax.fxlib.api.codec.mutableSetCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.Constants.DEFAULT_LANGUAGE
import com.ruslan.growsseth.Constants.TEMPLATE_DIARY_FOLDER
import com.ruslan.growsseth.GrowssethTags
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.config.GrowssethConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.http.ApiEvent
import com.ruslan.growsseth.http.GrowssethApi
import com.ruslan.growsseth.structure.GrowssethStructures
import com.ruslan.growsseth.templates.BookData
import com.ruslan.growsseth.templates.BookTemplates
import com.ruslan.growsseth.templates.TemplateListener
import com.ruslan.growsseth.utils.*
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset.isGrowssethPreset
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.world.Container
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.LecternBlock
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.phys.AABB


/**
 * Generate a diary in the researcher tent when a player has visited that tent.
 * (Mostly meant for singleplayer)
 * NOTE: uses structure tags instead of ids, make sure to set them. This is to allow
 * diary to work on variants of same structure.
 */
class ResearcherDiaryComponent(val researcher: Researcher) {

    companion object {
        val PERSIST_CODEC: Codec<DiaryData> = RecordCodecBuilder.create { b ->
            b.group(
                mutableMapCodec(TagKey.codec(Registries.STRUCTURE), Codec.BOOL).fieldOf("recordedStructures").forGetter(DiaryData::recordedStructures),
                mutableSetCodec(Codec.STRING).fieldOf("recordedEvents").forGetter(DiaryData::recordedEvents),
            ).apply(b, ::DiaryData)
        }

        val structToTag = mutableMapOf<ServerLevel, Map<ResourceKey<Structure>, TagKey<Structure>>>()
        val targetingConditions: TargetingConditions = TargetingConditions.forNonCombat().ignoreLineOfSight().ignoreInvisibilityTesting()

        // Cache diaries by structure, etc for efficiency
        // Needs to also reload on language change
        private var structureDiaries_: Map<TagKey<Structure>, BookData>? = null
        val structureDiaries get() = structureDiaries_ ?: throw IllegalStateException("StructureDiaries not init!")
        private var lastLanguageCode: String? = null

        fun init() {
            val prefix ="$TEMPLATE_DIARY_FOLDER/"
            TemplateListener.onReload(TemplateListener.TemplateKind.BOOK) { langTemplates, keys, _ ->
                val diaries = mutableMapOf<TagKey<Structure>, BookData>()
                val langCode = GrowssethConfig.serverLanguage
                lastLanguageCode = langCode
                keys.filter { it.startsWith(prefix) }.forEach { key ->
                    val structTag = TagKey.create(Registries.STRUCTURE, resLoc(key.replace(prefix, "")))
                    diaries[structTag] = langTemplates[key] ?: throw IllegalStateException("Error in caching structure diaries")
                }
                structureDiaries_ = diaries
                RuinsOfGrowsseth.LOGGER.info("Updated structure researcher diaries, has ${diaries.size}")
            }
        }

        private fun checkLanguageChanged() {
            val langCode = GrowssethConfig.serverLanguage
            if (lastLanguageCode != langCode) {
                lastLanguageCode = langCode
                val diaries = mutableMapOf<TagKey<Structure>, BookData>()
                val keys = BookTemplates.getAvailableTemplates()
                val prefix ="$TEMPLATE_DIARY_FOLDER/"
                keys.filter { it.startsWith(prefix) }.forEach { key ->
                    val structTag = TagKey.create(Registries.STRUCTURE, resLoc(key.replace(prefix, "")))
                    diaries[structTag] = BookTemplates.templates[key] ?: throw IllegalStateException("Error in caching structure diaries")
                }
                structureDiaries_ = diaries

                RuinsOfGrowsseth.LOGGER.info("Updated structure researcher diaries on language change, has ${diaries.size}")
            }
        }
    }

    val updatePeriod = 1f.secondsToTicks()

    var data = DiaryData()
        private set

    data class DiaryData(
        val recordedStructures: MutableMap<TagKey<Structure>, Boolean> = mutableMapOf(),
        val recordedEvents: MutableSet<String> = mutableSetOf(),
    )

    private val level = researcher.level() as ServerLevel
    private var lecternBlockEntity: LecternBlockEntity? = null
    private var previousDiariesChestBlockEntity: ChestBlockEntity? = null
    private var didFirstStructSearch = false

    fun aiStep() {
        if (!ResearcherConfig.researcherWritesDiaries) return
        /*
        FOR NOW: Outright disable with non-single researcher mode
        TODO: make multiple researchers mode make each researcher have diaries only
        for his structures and only if they're (re)discovered after a player reached that
        specific entity (harder part is the 2nd as we couldn't rely on just "player has discovered"
         */
        if (!ResearcherConfig.singleResearcher) return

        if (researcher.tickCount % updatePeriod == 0) {
            checkLanguageChanged()
            val anyNew = updateUnlockedStructures()

            if (anyNew || !data.recordedStructures.values.all { it }) {
                for ((structKey, alreadyRecorded) in data.recordedStructures) {
                    if (!alreadyRecorded) {
                        data.recordedStructures[structKey] = makeStructureDiary(structKey)
                    }
                }
            }

            // Custom event diaries
            val newEvents = CustomRemoteDiaries.diaries.filter { it.key !in data.recordedEvents }
            if (newEvents.isNotEmpty()) {
                for ((id, diary) in newEvents) {
                    makeEventDiary(diary)
                    data.recordedEvents.add(id)
                }
            }
        }
    }

    private fun makeEventDiary(customDiaryData: BookData): Boolean {
        val success = makeDiary({ customDiaryData }, {
            RuinsOfGrowsseth.LOGGER.info("Created custom remote diary (${it.name}), recording content...")
        }) { book -> CustomData.update(DataComponents.CUSTOM_DATA, book) { tag ->
            tag.put(DiaryHelper.TAG_REMOVE_DIARIES_ON_PUSH, ByteTag.valueOf(true))
        } }
        if (!success) {
            RuinsOfGrowsseth.LOGGER.info("Failed in creating custom remote diary ${customDiaryData.name}")
            return false
        }
        return true
    }

    private fun hasStructureDiary(forStructure: TagKey<Structure>): Boolean {
        return structureDiaries.containsKey(forStructure)
    }

    private fun makeStructureDiary(forStructure: TagKey<Structure>): Boolean {
        if (!hasStructureDiary(forStructure)) return false
        if (isGrowssethPreset(level.server) && forStructure == GrowssethTags.StructTags.BEEKEEPER_HOUSE)
            return false    // since cydo's beekeeper is a different character with a different diary

        val remoteDiaries = CustomRemoteDiaries.structureReplacementDiaries
        val success = makeDiary({ remoteDiaries[forStructure] ?: structureDiaries[forStructure] }) {
            RuinsOfGrowsseth.LOGGER.info("Created diary for ${forStructure.location} (${it.name}), recording content...")
        }
        if (!success) {
            RuinsOfGrowsseth.LOGGER.info("No diary for ${forStructure.location}")
            return false
        }
        return true
    }

    private fun makeDiary(selector: () -> BookData?, done: (BookData) -> Unit): Boolean
        = makeDiary(selector, done) {}

    private fun makeDiary(selector: () -> BookData?, done: (BookData) -> Unit, doOnDiary: (ItemStack) -> Unit): Boolean {
        val diaryData = selector() ?: return false
        val title = Component.literal(diaryData.name ?: "???")
        val pages = diaryData.pagesComponents
        val book = FxItemUtils.createWrittenBook(title, researcher.name, pages)

        done(diaryData)
        doOnDiary(book)
        pushDiaryToContainers(book)
        return true
    }

    fun makeArbitraryDiary(name: String, content: String) {
        val cname = Component.literal(name)
        val pages = content.split("===").map(Component::literal)
        val book = createWrittenBook(cname, researcher.name, pages)
        RuinsOfGrowsseth.LOGGER.info("Created test diary $name, recording content...")
        pushDiaryToContainers(book)
    }

    private fun pushDiaryToContainers(book: ItemStack) {
        findBlockEntsIfNull()
        DiaryHelper.pushDiaryToContainers(book, level, researcher, lecternBlockEntity, previousDiariesChestBlockEntity, skipExisting=true)
    }

    private val printedWarningFor = mutableSetOf<ResourceKey<Structure>>()

    private fun updateUnlockedStructures(): Boolean {
        val checkRange = 64.0
        val possiblePlayers: List<ServerPlayer> = level.getNearbyPlayers(
            targetingConditions,
            researcher,
            AABB.ofSize(researcher.position(), checkRange, checkRange / 2, checkRange)
        ).map { it as ServerPlayer }

        var found = false

        val structToTagLevel = structToTag[level] ?: structToTag[level.server.overworld()]

        if (structToTagLevel == null) {
            RuinsOfGrowsseth.LOGGER.error("No structToTag initialized for level or overworld! Level is $level")
            return false
        }

        possiblePlayers.forEach { player ->
            val unlocked = StructureAdvancements.getPlayerFoundStructures(player)
            val unlockedTags = unlocked.mapNotNull {
                val tag = structToTagLevel[it]
                if (tag == null && !printedWarningFor.contains(it)) {
                    RuinsOfGrowsseth.LOGGER.warn("Structure $it doesn't have a corresponding tag defined in GrowssethTags!")
                    printedWarningFor.add(it)
                }
                tag
            }.filter(this::hasStructureDiary)
            val new: List<TagKey<Structure>> = unlockedTags.minus(data.recordedStructures.keys)
            if (new.isNotEmpty()) {
                found = true
                data.recordedStructures.putAll(new.associateWith { false })
            }
        }

        return found
    }

    private fun findBlockEntsIfNull() {
        var findLectern = lecternBlockEntity == null || lecternBlockEntity?.isRemoved == true
        var findChest = previousDiariesChestBlockEntity == null || previousDiariesChestBlockEntity?.isRemoved == true
        val tent = researcher.tent
        if (findLectern || findChest) {
            if (tent == null) {
                val searchRange = 10
                val offset = Vec3i(searchRange, searchRange * 3 / 4, searchRange)
                for (center in listOfNotNull(researcher.startingPos, researcher.blockPosition())) {
                    val searchArea = BoundingBox.fromCorners(center.subtract(offset), center.offset(offset))
                    searchArea.iterBlocks { pos ->
                        val found = checkBlockPosForEnt(pos, findLectern, findChest)
                        findLectern = !found.first
                        findChest = !found.second

                        if (!findChest && !findLectern) {
                            return@iterBlocks
                        }
                    }
                }
            } else {
                val boundingBoxWithoutCellar = tent.boundingBox.clip(minY = tent.cellarTrapdoorPos?.y ?: (tent.boundingBox.minY() + 11))
                boundingBoxWithoutCellar.iterBlocks { pos ->
                    val found = checkBlockPosForEnt(pos, findLectern, findChest)
                    findLectern = !found.first
                    findChest = !found.second

                    if (!findChest && !findLectern) {
                        return@iterBlocks
                    }
                }
            }
        }
    }

    private fun checkBlockPosForEnt(pos: BlockPos, findLectern: Boolean, findChest: Boolean): Pair<Boolean, Boolean> {
        var foundLectern = false
        var foundChest = false
        if (findLectern) level.getBlockEntity(pos)?.let {
            if (it is LecternBlockEntity) {
                lecternBlockEntity = it
                foundLectern = true
            }
        }
        if (findChest) level.getBlockEntity(pos)?.let {
            if (it is ChestBlockEntity) {
                previousDiariesChestBlockEntity = it
                foundChest = true
            }
        }
        return Pair(foundLectern, foundChest)
    }

    fun writeNbt(tag: CompoundTag) {
        tag.put("DiaryData", PERSIST_CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow {
            throw Exception("Error in encoding DiaryData: $it")
        })
    }

    fun readNbt(tag: CompoundTag) {
        data = DiaryData()
        tag.get("DiaryData")?.let { diaryData ->
            data = PERSIST_CODEC.decode(NbtOps.INSTANCE, diaryData).getOrThrow {
                throw Exception("Error in decoding DiaryData: $it")
            }.first
        }
    }

    object Callbacks {
        fun onServerLevel(level: ServerLevel) {
            // Probably not optimized? Oh. well
            // Get only once instead of running for every player
            // Run now for registry access reasons and to run only once
            val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)

            // Might include datapack-added things
            val registryTagAssociations = GrowssethTags.StructTags.ALL.flatMap { tag ->
                registry.getTagOrEmpty(tag).filter { it.unwrapKey().isPresent }.map { it.unwrapKey().get() to tag }
            }.associate { it }
            // Also includes placeholders (most importantly village house references),
            // without this structures tracked by jigsaw piece (village house) advancements
            // won't be detected
            val staticTagAssociations = GrowssethStructures.info.mapValues { it.value.tag }

            structToTag[level] = staticTagAssociations + registryTagAssociations
        }
    }
}

object DiaryHelper {
    const val TAG_REMOVE_DIARIES_ON_PUSH = "RemoveOnResDiaryPush"

    fun hasCustomEndDiary(): Boolean {
        return CustomRemoteDiaries.endDiary != null
    }

    fun getCustomEndDiary(author: Component): ItemStack? {
        val book = ItemStack(Items.WRITTEN_BOOK)
        val diaryData = CustomRemoteDiaries.endDiary ?: run {
            RuinsOfGrowsseth.LOGGER.error("getCustomEndDiary: no end diary loaded!")
            return null
        }

        val pages = diaryData.pages
        RuinsOfGrowsseth.LOGGER.info("Created end diary (${diaryData.name}, ${pages.size} pages)")
        return BookTemplates.loadTemplate(book, diaryData)
    }

    private fun diaryMatches(book1: ItemStack, book2: ItemStack): Boolean {
        val text1 = book1.getBookText()
        val text2 = book2.getBookText()
        if (!(book1.item == book2.item
                && (!book1.`is`(Items.WRITTEN_BOOK) || book1.getBookTitle() == book2.getBookTitle())
                && (!book1.`is`(Items.WRITTEN_BOOK) || book1.getBookAuthor() == book2.getBookAuthor())
                && text1.size == text2.size
        )) return false

        for (i in text1.indices) {
            if (text1[i].string != text2[i].string)
                return false
        }
        return true
    }

    fun pushDiaryToContainers(book: ItemStack, level: ServerLevel, entity: LivingEntity, lectern: LecternBlockEntity?, chest: ChestBlockEntity?, skipExisting: Boolean = false) {
        var currentItem: ItemStack? = book

        lectern?.let {
            val state = level.getBlockState(it.blockPos)
            if (skipExisting && !it.book.isEmpty && diaryMatches(it.book, currentItem!!)) {
                return
            }
            if (!LecternBlock.tryPlaceBook(entity, level, it.blockPos, state, currentItem!!)) {
                val prevBook = it.book
                it.book = currentItem
                it.setChanged()
                level.gameEvent(GameEvent.BLOCK_CHANGE, it.blockPos, GameEvent.Context.of(entity, state))
                currentItem = prevBook
            }
        }
        if (currentItem?.isEmpty == true) currentItem = null

        // If item on lectern that is made by events etc, do not place in chest
        if (currentItem?.let { it[DataComponents.CUSTOM_DATA]?.contains(TAG_REMOVE_DIARIES_ON_PUSH) } == true) {
            currentItem = null
        }

        if (currentItem != null) {
            chest?.let {
                if (skipExisting) {
                    for (slot in 0 until it.containerSize) {
                        val item = it.getItem(slot)
                        if (!item.isEmpty && (item.`is`(Items.WRITABLE_BOOK) || item.`is`(Items.WRITTEN_BOOK)) && diaryMatches(item, currentItem!!)) {
                            return
                        }
                    }
                }
                val success = addToContainer(it, currentItem!!)
                if (success) {
                    currentItem = null
                }
            }
        }
        if (currentItem?.isEmpty == true) currentItem = null

        if (currentItem != null) {
            val itemEntity = ItemEntity(level, entity.x, entity.eyeY - 0.3F, entity.z, currentItem!!)
            itemEntity.setPickUpDelay(20)
            level.addFreshEntity(itemEntity)
        }
    }

    private fun addToContainer(container: Container, stack: ItemStack): Boolean {
        for (slot in 0 until container.containerSize) {
            val slotStack = container.getItem(slot)
            if (container.canPlaceItem(slot, stack)) {
                if (slotStack.isEmpty) {
                    container.setItem(slot, stack)
                    return true
                } else if (slotStack.count <= slotStack.maxStackSize - stack.count
                    && ItemStack.isSameItemSameComponents(stack, slotStack)
                ) {
                    slotStack.grow(stack.count)
                    return true
                }
            }
        }
        return false
    }
}

object CustomRemoteDiaries {
    // Not language separated
    val diaries = mutableMapOf<String, BookData>()
    var endDiary: BookData? = null
        private set
    val structureReplacementDiaries = mutableMapOf<TagKey<Structure>, BookData>()

    const val DIARY_EVENT_NAME = "rdiary"
    const val DIARY_END_EVENT_NAME = "enddiary"
    const val DIARY_STRUCT_EVENT_NAME = "structdiary"
    const val PAGEBREAK = "%PAGEBREAK%"

    private fun isDiaryEvent(event: ApiEvent): Boolean {
        return event.name.split("/")[0] == DIARY_EVENT_NAME
    }

    private fun isEndDiaryEvent(event: ApiEvent): Boolean {
        return event.name.split("/")[0] == DIARY_END_EVENT_NAME
    }

    private fun isStructDiaryEvent(event: ApiEvent): Boolean {
        return event.name.split("/")[0] == DIARY_STRUCT_EVENT_NAME
    }

    private fun diaryFromEvent(event: ApiEvent): Pair<String, BookData>? {
        val title = event.name.split("/").getOrNull(1) ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event researcher diary: no slash, cannot find title: $event.name")
            return null
        }
        val desc = event.desc ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event researcher diary: no description, needed for pages")
            return null
        }
        val pos = event.pos ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event researcher diary: no pos, needed for id")
            return null
        }
        val id = "$title-${pos.x}-${pos.y}-${pos.z}"

        val pages = desc.split(PAGEBREAK)

        val diary = BookData(pages=pages.map(BookData::pageEntry), name=title)

        return id to diary
    }

    private fun structDiaryFromEvent(event: ApiEvent): Pair<TagKey<Structure>, BookData>? {
        val title = event.name.split("/").getOrNull(2) ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event struct researcher diary: not enough slashes, cannot find title: ${event.name}")
            return null
        }
        val structName = event.name.split("/").getOrNull(1) ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event struct researcher diary: cannot find structName, unknown error: ${event.name}")
            return null
        }
        val desc = event.desc ?: run {
            RuinsOfGrowsseth.LOGGER.warn("Event struct researcher diary: no description, needed for pages")
            return null
        }
        val id = TagKey.create(Registries.STRUCTURE, resLoc(structName))

        val pages = desc.split(PAGEBREAK)

        val diary = BookData(pages=pages.map(BookData::pageEntry), name=title)

        return id to diary
    }

    fun init() {
        GrowssethApi.current.subscribe { api, server ->
            diaries.clear()
            diaries.putAll(api.events.filter{ isDiaryEvent(it) && it.active }.mapNotNull(this::diaryFromEvent))

            structureReplacementDiaries.clear()
            structureReplacementDiaries.putAll(api.events.filter { isStructDiaryEvent(it) && it.active }
                .mapNotNull(this::structDiaryFromEvent)
            )

            api.events.find { isEndDiaryEvent(it) && it.active }?.let(this::diaryFromEvent)?.let { (_, endDiary) ->
                this.endDiary = endDiary
                RuinsOfGrowsseth.LOGGER.info("Prepared researcher end diary (${endDiary.name}, ${endDiary.pages.size} pages)")
            }
        }
    }

    fun onServerStopped() {
        diaries.clear()
        structureReplacementDiaries.clear()
        endDiary = null
    }
}