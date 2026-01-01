package com.ruslan.growsseth.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ruslan.growsseth.entity.researcher.Researcher;
import com.ruslan.growsseth.item.AbstractResearcherDaggerItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Unique
    Player player = (Player)(Object)this;
    @Unique
    boolean lastAttackerIsResearcher = false;

    @ModifyVariable(method = "attack", at = @At(value = "STORE"), ordinal = 3)
    private boolean noSweepingEdgeForResearcherDagger(boolean bl4) {    // editing the sweeping edge boolean
        Item itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (itemInHand instanceof AbstractResearcherDaggerItem)
            return false;
        return bl4;
    }

    @Inject(method = "blockUsingItem", at = @At("HEAD"))
    private void getAttacker(ServerLevel level, LivingEntity entity, CallbackInfo ci){
        lastAttackerIsResearcher = entity instanceof Researcher;
    }

    // todo: fix, new method is in net.minecraft.world.item.component.BlocksAttacks#disable, it was moved from the player to the component
//    @ModifyConstant(method = "disableShield", constant = @Constant(intValue = 100))
//    private int lessShieldCooldownForResearcher(int constant) {
//        if (lastAttackerIsResearcher)
//            return 30;      // 1.5 seconds of cooldown instead of usual 5
//        return constant;
//    }

    @ModifyReturnValue(method = "entityInteractionRange", at = @At("RETURN"))
    public double shortRangeForDagger(double original){
        Item itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND).getItem();
        if (itemInHand instanceof AbstractResearcherDaggerItem)
            return 2.5;     // half block less than normal
        return original;
    }
}
