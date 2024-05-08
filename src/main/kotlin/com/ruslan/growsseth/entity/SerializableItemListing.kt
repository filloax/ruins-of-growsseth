package com.ruslan.growsseth.entity

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.npc.VillagerTrades
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import java.util.*

open class SerializableItemListing(
    val gives: ItemStack,
    val wants: List<ItemCost>,
    val maxUses: Int,
    val xp: Int = 0,
    val priceMul: Float = 1f,
    val foo: Float = -1f,
) : VillagerTrades.ItemListing {
    companion object {
        val CODEC: Codec<SerializableItemListing> = RecordCodecBuilder.create { b -> b.group(
            ItemStack.CODEC.fieldOf("gives").forGetter(SerializableItemListing::gives),
            ItemCost.CODEC.listOf().fieldOf("wants").forGetter(SerializableItemListing::wants),
            Codec.INT.fieldOf("maxUses").forGetter(SerializableItemListing::maxUses),
            Codec.INT.fieldOf("xp").forGetter(SerializableItemListing::xp),
            Codec.FLOAT.fieldOf("priceMul").forGetter(SerializableItemListing::priceMul),
            Codec.FLOAT.fieldOf("foo").orElse(-1f).forGetter(SerializableItemListing::foo),
        ).apply(b, ::SerializableItemListing) }
    }

    init {
        if (wants.size > 2) {
            throw IllegalArgumentException("Wants must be of max size 2")
        }
    }

    override fun getOffer(trader: Entity, random: RandomSource): MerchantOffer
        = MerchantOffer(wants[0], Optional.ofNullable(wants.getOrNull(1)), gives, maxUses, xp, priceMul)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializableItemListing

        if (gives != other.gives) return false
        if (wants != other.wants) return false
        if (maxUses != other.maxUses) return false
        if (xp != other.xp) return false
        return priceMul == other.priceMul
    }

    override fun hashCode(): Int {
        var result = gives.hashCode()
        result = 31 * result + wants.hashCode()
        result = 31 * result + maxUses
        result = 31 * result + xp
        result = 31 * result + priceMul.hashCode()
        return result
    }
}