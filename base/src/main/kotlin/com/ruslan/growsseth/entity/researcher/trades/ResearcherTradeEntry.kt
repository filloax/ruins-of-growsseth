package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.*
import com.filloax.fxlib.api.nbt.*
import com.filloax.fxlib.api.codec.*
import com.filloax.fxlib.api.getStructTagOrKey
import com.filloax.fxlib.api.json.ResourceLocationSerializer
import com.google.common.collect.ImmutableList
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.advancements.StructureAdvancements
import com.ruslan.growsseth.config.QuestConfig
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.SerializableItemListing
import com.ruslan.growsseth.entity.researcher.Researcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import net.minecraft.core.RegistryAccess
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.level.saveddata.maps.MapDecorationType
import kotlin.jvm.optionals.getOrNull
import kotlin.math.roundToInt


data class ResearcherTradeEntry(
    val itemListing: ResearcherItemListing,
    val priority: Int,
    val replace: Boolean = false,
) {
    companion object {
        val CODEC: Codec<ResearcherTradeEntry> = RecordCodecBuilder.create { b -> b.group(
            ResearcherItemListing.CODEC.fieldOf("itemListing").forGetter(ResearcherTradeEntry::itemListing),
            Codec.INT.fieldOf("priority").forGetter(ResearcherTradeEntry::priority),
            Codec.BOOL.fieldOf("replace").forGetter(ResearcherTradeEntry::replace),
        ).apply(b, ::ResearcherTradeEntry) }

        val LIST_CODEC = CODEC.listOf()
        val MLIST_CODEC = mutableListCodec(CODEC)
    }

    fun looselyMatches(other: ResearcherTradeEntry) = this.itemListing.looselyMatches(other.itemListing)
}


/**
 * Describes a trade for the researcher.
 * @param gives Trade output
 * @param wants list of one or two itemstacks for trade input
 * @param maxUses Maximum uses of the trade, note that it is currently sort-of-unused as the uses reset when the trades are regenerated
 * @param mapInfo If the trade item is a map, TradeItemMapInfo object to describe its target structure or position
 * @param bookId Id of book template to use if this is book
 * @param noNotification Prevent this trade from being notified to the player
 * @param replace Replace a higher priority trade with the same output
 */
class ResearcherItemListing(
    gives: ItemStack,
    wants: List<ItemCost>,
    maxUses: Int,
    val mapPool: List<TradeItemMapInfo> = BLANK_MAP_POOL,
    val bookId: String? = null,
    xp: Int = 0,
    priceMul: Float = 1f,
    val noNotification: Boolean = false,
    val randomWeight: Float = 0f,
) : SerializableItemListing(gives, wants, maxUses, xp, priceMul) {
    companion object {
        val CODEC: Codec<ResearcherItemListing> = RecordCodecBuilder.create { b -> b.group(
            ItemStack.CODEC.fieldOf("gives").forGetter(ResearcherItemListing::gives),
            ItemCost.CODEC.listOf().fieldOf("wants").forGetter(ResearcherItemListing::wants),
            Codec.INT.fieldOf("maxUses").forGetter(ResearcherItemListing::maxUses),
            TradeItemMapInfo.CODEC.listOf().optionalFieldOf("mapPool", BLANK_MAP_POOL).forGetter(ResearcherItemListing::mapPool),
            Codec.STRING.optionalFieldOf("diaryId").forNullableGetter { null }, // Deprecated, backwards compat
            Codec.STRING.optionalFieldOf("bookId").forNullableGetter(ResearcherItemListing::bookId),
            Codec.INT.fieldOf("xp").forGetter(ResearcherItemListing::xp),
            Codec.FLOAT.fieldOf("priceMul").forGetter(ResearcherItemListing::priceMul),
            Codec.BOOL.fieldOf("noNotification").forGetter(ResearcherItemListing::noNotification),
            Codec.FLOAT.fieldOf("randomWeight").forGetter(ResearcherItemListing::randomWeight),
        ).apply(b) { gives, wants, maxUses, mapPool, diaryIdOpt, bookIdOpt, xp, priceMul, noNotification, randomWeight ->
            val diaryId = diaryIdOpt.getOrNull()
            val bookId = bookIdOpt.getOrNull()
            ResearcherItemListing(gives, wants, maxUses, mapPool, bookId ?: diaryId, xp, priceMul, noNotification, randomWeight)
        } }

        val MLIST_CODEC: Codec<MutableList<ResearcherItemListing>> = mutableListCodec(CODEC)
        val LIST_CODEC: Codec<List<ResearcherItemListing>> = Codec.list(CODEC)

        val BLANK_MAP_POOL = ImmutableList.of<TradeItemMapInfo>()

        const val SET_MAP_TAG = "ResearcherSetMap"
        const val MAP_INFO_TAG = "ResearcherMapInfo"
        const val BOOK_TEMPLATE_TAG = "ResearcherBookId"
    }

    override fun getOffer(trader: Entity, random: RandomSource): MerchantOffer {
        var costMultiplier = 1f
        if (trader is Researcher) {
            // No donkey penalty while healed
            if (trader.donkeyWasBorrowed && !trader.healed) costMultiplier *= ResearcherConfig.researcherBorrowPenalty
            if (trader.healed) costMultiplier *= QuestConfig.researcherCuredDiscount
        } else {
            RuinsOfGrowsseth.LOGGER.warn("ResearcherTradeEntry used for non-Researcher!")
        }
        // Do not use priceMultiplier field as that is related to demand updating

        val offer = super.getOffer(trader, random)

        offer.addToSpecialPriceDiff((offer.costA.count * (costMultiplier - 1)).roundToInt())

        if (mapPool.isNotEmpty()) {
            val map = mapPool[random.nextInt(0, mapPool.size)]
            CustomData.update(DataComponents.CUSTOM_DATA, offer.result) { it.saveField(MAP_INFO_TAG, TradeItemMapInfo.CODEC) { map } }
        }

        bookId?.let { id ->
            CustomData.update(DataComponents.CUSTOM_DATA, offer.result) { it.putString(BOOK_TEMPLATE_TAG, id) }
        }

        return offer
    }

    fun looselyMatches(other: ResearcherItemListing) =
        ItemStack.matches(gives(), other.gives()) && mapPool == other.mapPool

    fun isDiscoveredStructure(player: ServerPlayer): Boolean {
        return mapPool.any { mapInfo ->
                val structId = getStructTagOrKey(mapInfo.structure)
                val fixedStructId = mapInfo.fixedStructureId?.let(::getStructTagOrKey)
                StructureAdvancements.playerHasFoundStructure(player, structId)
                && fixedStructId?.let { StructureAdvancements.playerHasFoundStructure(player, it) } == true
            }
    }

    // In case of jigsaw piece maps (ie village houses), this may include vanilla structure ids;
    // If things were done properly, the fixedstructureid should still lead to a mod structure
    // which will allow recognition anyways
    fun getAllPossibleStructures(registryAccess: RegistryAccess): List<ResourceLocation> {
        val baseStructures = mapPool
            .flatMap { ResearcherTradeUtils.getMatchingStructures(registryAccess, it.structure) }
        val fixedStructures = mapPool
            .filter { it.fixedStructureId != null }
            .flatMap { ResearcherTradeUtils.getMatchingStructures(registryAccess, it.fixedStructureId!!) }

        return baseStructures + fixedStructures
    }
}

/**
 * @param structure Structure id for the corresponding structure, can also be tag if starting with #. Used to assign icon, and to search in vanilla worldgen
 *  structure placement if no coordinates or fixedStructureId are provided.
 * @param name Name for the map item.
 * @param description Tooltip for the map item (optional).
 * @param x Coordinates for the map to point to.
 * @param z Coordinates for the map to point to.
 * @param fixedStructureId Structure id for the structure to search among the fixed structure spawns (see [com.ruslan.growsseth.structure.CustomPlacedStructureTracker])
 * @param scale Map scale, defaults to 3 (note: default only in JsonDesc class but not "real" Kotlin class to make the Codec work nicely
 */
data class TradeItemMapInfo (
    val structure: String,
    val name: String,
    val description: List<String>? = null,
    val x: Int? = null,
    val z: Int? = null,
    val fixedStructureId: String? = null,
    val scale: Int? = null,
    val searchForJigsawIds: List<ResourceLocation>? = null,
    val overrideMapIcon: ResourceKey<MapDecorationType>? = null,
) {
    companion object {
        val CODEC: Codec<TradeItemMapInfo> = RecordCodecBuilder.create { b -> b.group(
            Codec.STRING.fieldOf("structure").forGetter(TradeItemMapInfo::structure),
            Codec.STRING.fieldOf("name").forGetter(TradeItemMapInfo::name),
            Codec.list(Codec.STRING).optionalFieldOf("description").forNullableGetter(TradeItemMapInfo::description),
            Codec.INT.optionalFieldOf("x").forNullableGetter(TradeItemMapInfo::x),
            Codec.INT.optionalFieldOf("z").forNullableGetter(TradeItemMapInfo::z),
            Codec.STRING.optionalFieldOf("fixedStructureId").forNullableGetter(TradeItemMapInfo::fixedStructureId),
            Codec.INT.optionalFieldOf("scale").forNullableGetter(TradeItemMapInfo::scale),
            ResourceLocation.CODEC.listOf().optionalFieldOf("searchForJigsawIds").forNullableGetter(TradeItemMapInfo::searchForJigsawIds),
            ResourceKey.codec(Registries.MAP_DECORATION_TYPE).optionalFieldOf("overrideMapIcon").forNullableGetter(TradeItemMapInfo::overrideMapIcon),
        ).apply(b, TradeItemMapInfo::class.constructorWithOptionals()::newInstance) }
    }

    @Serializable
    data class JsonDesc (
        val structure: String,
        val name: String,
        val description: JsonElement? = null,
        val x: Int? = null,
        val z: Int? = null,
        val fixedStructureId: String? = null,
        @Serializable(with = ResourceLocationSerializer::class)
        val overrideMapIcon: ResourceLocation? = null,
        val scale: Int = 3,
        @Serializable(with = ResourceLocationListSerializer::class)
        val searchForJigsawIds: List<@Serializable(with=ResourceLocationSerializer::class) ResourceLocation>? = null,
    ) {
        fun unwrap(): TradeItemMapInfo {
            return TradeItemMapInfo(
                structure, name,
                description?.let { desc -> when (desc) {
                    is JsonPrimitive -> listOf(description.jsonPrimitive.content)
                    is JsonArray     -> description.jsonArray.map { it.jsonPrimitive.content }
                    else             -> throw SerializationException("description must be string or list of strings! is $description")
                }},
                x, z, fixedStructureId,
                scale,
                searchForJigsawIds,
                overrideMapIcon = overrideMapIcon?.let { ResourceKey.create(Registries.MAP_DECORATION_TYPE, it) }
            )
        }
    }

    class ResourceLocationListSerializer : JsonTransformingSerializer<List<ResourceLocation>>(ListSerializer(ResourceLocationSerializer())) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            if (element is JsonPrimitive) {
                return JsonArray(listOf(element))
            }
            return element
        }
    }
}