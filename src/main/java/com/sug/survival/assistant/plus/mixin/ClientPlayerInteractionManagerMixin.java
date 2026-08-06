package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.AutoTool;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void sug_survival_assistant_plus$startDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AutoTool.beforeBlockAttack(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void sug_survival_assistant_plus$continueDestroyBlock(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir
    ) {
        AutoTool.beforeBlockAttack(pos);
    }
}
