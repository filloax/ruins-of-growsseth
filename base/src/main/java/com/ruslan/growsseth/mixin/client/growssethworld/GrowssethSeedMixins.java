package com.ruslan.growsseth.mixin.client.growssethworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.ruslan.growsseth.client.gui.RawSetEditBox;
import com.ruslan.growsseth.worldgen.worldpreset.GrowssethWorldPreset;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.WorldOptions;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class GrowssethSeedMixins {
    @Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
    public static class WorldTabMixin {

        @Final @Shadow
        // CreateWorldScreen.this
        CreateWorldScreen field_42182;

        @Final @Shadow
        private EditBox seedEdit;

        @Inject(
                method = "<init>(Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;)V",
                at = @At("RETURN")
        )
        private void onInitialize(CreateWorldScreen createWorldScreen, CallbackInfo ci) {
            field_42182.getUiState().addListener(worldCreationUiState -> {
                WorldCreationUiState.WorldTypeEntry worldType = worldCreationUiState.getWorldType();

                if (GrowssethWorldPreset.isGrowssethPreset(worldType.preset())) {
                    seedEdit.setEditable(false);
                    ((RawSetEditBox)seedEdit).growsseth_rawSetValue(GrowssethWorldPreset.GROWSSETH_SEED);
                } else {
                    seedEdit.setEditable(true);
                }
            });
        }
    }

    @Mixin(WorldCreationUiState.class)
    public static class WorldCreationUiStateMixin {
        @Shadow
        private String seed;
        @Shadow
        private WorldCreationContext settings;

        @Shadow
        private WorldCreationUiState.WorldTypeEntry worldType;

        @Inject(
                method = "setSeed",
                at = @At(
                        value = "FIELD",
                        target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;seed:Ljava/lang/String;",
                        opcode= Opcodes.PUTFIELD,
                        ordinal = 0,
                        shift = At.Shift.AFTER
                )
        )
        private void onSetSeed(String seed, CallbackInfo ci) {
            if (GrowssethWorldPreset.isGrowssethPreset(worldType.preset())) {
                this.seed = GrowssethWorldPreset.GROWSSETH_SEED;
            }
        }

        @Inject(method = "onChanged", at = @At("HEAD"))
        private void onOnChanged(CallbackInfo ci) {
            if (GrowssethWorldPreset.isGrowssethPreset(worldType.preset())) {
                this.seed = GrowssethWorldPreset.GROWSSETH_SEED;
                this.settings = this.settings.withOptions(worldOptions -> worldOptions.withSeed(WorldOptions.parseSeed(this.seed)));
            }
        }
    }
}
