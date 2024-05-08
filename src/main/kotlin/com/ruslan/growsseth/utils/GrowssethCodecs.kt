package com.ruslan.growsseth.utils

import com.filloax.fxlib.codec.mutableListCodec
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import java.util.*


object GrowssethCodecs {
    val MERCHANT_OFFER_CODEC: Codec<MerchantOffer> = RecordCodecBuilder.create { builder ->
        builder.group(
            ItemCost.CODEC.fieldOf("baseCostA").forGetter(MerchantOffer::getItemCostA),
            ItemCost.CODEC.optionalFieldOf("costB").forGetter(MerchantOffer::getItemCostB),
            ItemStack.CODEC.fieldOf("result").forGetter(MerchantOffer::getResult),
            Codec.INT.fieldOf("uses").forGetter(MerchantOffer::getUses),
            Codec.INT.fieldOf("maxUses").forGetter(MerchantOffer::getMaxUses),
            Codec.BOOL.fieldOf("rewardExp").forGetter(MerchantOffer::shouldRewardExp),
            Codec.INT.fieldOf("specialPriceDiff").forGetter(MerchantOffer::getSpecialPriceDiff),
            Codec.INT.fieldOf("demand").forGetter(MerchantOffer::getDemand),
            Codec.FLOAT.fieldOf("priceMultiplier").forGetter(MerchantOffer::getPriceMultiplier),
            Codec.INT.fieldOf("xp").forGetter(MerchantOffer::getXp)
        ).apply(builder) { baseCostA: ItemCost, costB: Optional<ItemCost>, result: ItemStack, uses: Int, maxUses: Int, rewardExp: Boolean, specialPriceDiff: Int, demand: Int, priceMultiplier: Float, xp: Int ->
            val out = MerchantOffer(baseCostA, costB, result, uses, maxUses, xp, priceMultiplier, demand)
            out.specialPriceDiff = specialPriceDiff
            out.rewardExp = rewardExp
            out
        }
    }

    val MERCHANT_OFFERS_CODEC: Codec<MerchantOffers> = mutableListCodec(MERCHANT_OFFER_CODEC).xmap({ mutableList ->
        MerchantOffers().also { it.addAll(mutableList) }
    }, { arrayList -> arrayList})
}