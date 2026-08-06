package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void sug_survival_assistant_plus$setPosition(double x, double y, double z);

    @Invoker("setRotation")
    void sug_survival_assistant_plus$setRotation(float yRot, float xRot);
}
