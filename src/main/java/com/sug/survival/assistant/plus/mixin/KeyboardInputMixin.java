package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.sug.survival.assistant.plus.feature.Freecam;
import com.sug.survival.assistant.plus.feature.HaoQiChongTian;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {
    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void sug_survival_assistant_plus$tick(CallbackInfo ci) {
        if (!Freecam.isActive() && !HaoQiChongTian.isFrozen()) return;
        this.keyPresses = Input.EMPTY;
        this.moveVector = Vec2.ZERO;
    }
}
