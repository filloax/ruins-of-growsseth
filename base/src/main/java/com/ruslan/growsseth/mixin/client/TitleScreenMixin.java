package com.ruslan.growsseth.mixin.client;

import com.ruslan.growsseth.config.ClientConfigHandler;
import com.ruslan.growsseth.config.GrowssethConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(
        method = "init",
        at = @At("RETURN")
    )
    private void onInit(CallbackInfo ci) {
        if (Objects.equals(GrowssethConfig.serverLanguage, "client")) {
            // If first launch = has not seen accessibility screen, set later
            if (!Minecraft.getInstance().options.onboardAccessibility)
                ClientConfigHandler.setServerLangFromClient();
        }
    }
}
