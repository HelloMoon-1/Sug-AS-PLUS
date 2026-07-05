package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.spectator.PlayerMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.categories.TeleportToPlayerMenuCategory;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Mixin(TeleportToPlayerMenuCategory.class)
public abstract class TeleportToPlayerMenuCategoryMixin {
    @Mutable
    @Shadow
    @Final
    private List<SpectatorMenuItem> items;

    @Inject(method = "<init>(Ljava/util/Collection;)V", at = @At("TAIL"))
    private void sug_survival_assistant_plus$includeSpectatorPlayers(Collection<PlayerInfo> players, CallbackInfo ci) {
        if (!Configs.SPECTATOR_LIST_COMPLETION.getBooleanValue()) return;
        UUID localPlayerId = Minecraft.getInstance().getUser().getProfileId();
        items = players.stream()
                .filter(player -> !player.getProfile().id().equals(localPlayerId))
                .sorted(Comparator.comparing(player -> player.getProfile().id()))
                .map(PlayerMenuItem::new)
                .map(SpectatorMenuItem.class::cast)
                .toList();
    }
}
