package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.AutoEat;
import com.sug.survival.assistant.plus.feature.Freecam;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true)
    private void sug_survival_assistant_plus$setDown(boolean pressed, CallbackInfo ci) {
        KeyMapping key = (KeyMapping) (Object) this;
        if (Freecam.handleMovementKey(key, pressed)) {
            ci.cancel();
            return;
        }
        if (key == Minecraft.getInstance().options.keyUse && AutoEat.handleUseKey(pressed)) {
            ci.cancel();
        }
    }
}
