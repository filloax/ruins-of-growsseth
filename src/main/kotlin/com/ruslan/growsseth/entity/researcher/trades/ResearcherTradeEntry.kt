package com.ruslan.growsseth.entity.researcher.trades

import com.filloax.fxlib.*
import com.filloax.fxlib.nbt.*
import com.filloax.fxlib.codec.*
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.config.ResearcherConfig
import com.ruslan.growsseth.entity.SerializableItemListing
import com.ruslan.growsseth.entity.researcher.DiaryHelper
import com.ruslan.growsseth.entity.researcher.Researcher
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import net.minecraft.util.RandomSource
import net.minecraft.util.random.Weight
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.MerchantOffer
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
 * @param noNotification Prevent this trade from being notified to the player
 * @param replace Replace a higher priority trade with the same output
 */
class ResearcherItemListing(
    gives: ItemStack,
    wants: List<ItemStack>,
    maxUses: Int,
    val mapInfo: TradeItemMapInfo? = null,
    val diaryId: String? = null,
    xp: Int = 0,
    priceMul: Float = 1f,
    val noNotification: Boolean = false,
    val randomWeight: Float = 0f,
) : SerializableItemListing(gives, wants, maxUses, xp, priceMul) {
    companion object {
        val CODEC: Codec<ResearcherItemListing> = RecordCodecBuilder.create { b -> b.group(
            ItemStack.CODEC.fieldOf("gives").forGetter(ResearcherItemListing::gives),
            Codec.list(ItemStack.CODEC).fieldOf("wants").forGetter(ResearcherItemListing::wants),
            Codec.INT.fieldOf("maxUses").forGetter(ResearcherItemListing::maxUses),
            TradeItemMapInfo.CODEC.optionalFieldOf("mapInfo").forNullableGetter(ResearcherItemListing::mapInfo),
            Codec.STRING.optionalFieldOf("diaryId").forNullableGetter(ResearcherItemListing::diaryId),
            Codec.INT.fieldOf("xp").forGetter(ResearcherItemListing::xp),
            Codec.FLOAT.fieldOf("priceMul").forGetter(ResearcherItemListing::priceMul),
            Codec.BOOL.fieldOf("noNotification").forGetter(ResearcherItemListing::noNotification),
            Codec.FLOAT.fieldOf("randomWeight").forGetter(ResearcherItemListing::randomWeight),
        ).apply(b, constructorWithOptionals(ResearcherItemListing::class)::newInstance) }

        val MLIST_CODEC: Codec<MutableList<ResearcherItemListing>> = mutableListCodec(CODEC)
        val LIST_CODEC: Codec<List<ResearcherItemListing>> = Codec.list(CODEC)

        private const val SET_MAP_TAG = "ResearcherSetMap"
    }

    override fun getOffer(trader: Entity, random: RandomSource): MerchantOffer {
        var costMultiplier = 1f
        if (trader is Researcher) {
            // No donkey penalty while healed
            if (trader.donkeyWasBorrowed && !trader.healed) costMultiplier *= ResearcherConfig.researcherBorrowPenalty
            if (trader.healed) costMultiplier *= ResearcherConfig.researcherCuredDiscount
        } else {
            RuinsOfGrowsseth.LOGGER.warn("ResearcherTradeEntry used for non-Researcher!")
        }
        // Do not use priceMultiplier field as that is related to demand updating

        val offer = super.getOffer(trader, random)

        offer.addToSpecialPriceDiff((offer.costA.count * (costMultiplier - 1)).roundToInt())

        mapInfo?.let { map ->
            if (!trader.level().isClientSide && !gives.getOrCreateTag().contains(SET_MAP_TAG) && trader is Researcher) {
                gives.getOrCreateTag().putBoolean(SET_MAP_TAG, true)
                ResearcherTradeUtils.setTradeMapTarget(trader, gives, map, offer)
            }
        }

        diaryId?.let { id ->
            DiaryHelper.updateItemWithMiscDiary(offer.result, id, trader)
        }

        return offer
    }

    fun looselyMatches(other: ResearcherItemListing) =
        ItemStack.matches(gives, other.gives) && mapInfo == other.mapInfo
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
        ).apply(b, constructorWithOptionals(TradeItemMapInfo::class)::newInstance) }
    }

    @Serializable
    data class JsonDesc (
        val structure: String,
        val name: String,
        val description: JsonElement? = null,
        val x: Int? = null,
        val z: Int? = null,
        val fixedStructureId: String? = null,
        val scale: Int = 3,
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
            )
        }
    }
}