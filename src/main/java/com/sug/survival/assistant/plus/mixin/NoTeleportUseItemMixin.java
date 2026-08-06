package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.feature.NoTeleport;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class NoTeleportUseItemMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void sug_survival_assistant_plus$guardEnderPearlUse(
            Player player,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (NoTeleport.shouldBlockEnderPearlUse(player.level(), player, hand)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
