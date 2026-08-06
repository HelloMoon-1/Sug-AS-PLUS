package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.ShulkerRestock;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void sug_survival_assistant_plus$setScreen(Screen screen, CallbackInfo ci) {
        if (ShulkerRestock.shouldHideScreen() && screen instanceof ShulkerBoxScreen) {
            ci.cancel();
        }
    }
}
