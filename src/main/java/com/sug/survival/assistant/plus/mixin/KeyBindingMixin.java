package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.feature.Freecam;

@Mixin(KeyMapping.class)
public abstract class KeyBindingMixin {
    @Inject(method = "setDown", at = @At("HEAD"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$setDown(boolean pressed, CallbackInfo ci) {
        if (Freecam.handleMovementKey((KeyMapping) (Object) this, pressed)) ci.cancel();
    }
}
