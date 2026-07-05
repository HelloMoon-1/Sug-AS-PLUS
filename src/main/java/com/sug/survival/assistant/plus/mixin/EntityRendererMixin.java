package com.sug.survival.assistant.plus.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.feature.NametagRenderer;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true, remap = false)
    private void sug_survival_assistant_plus$getNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player && NametagRenderer.shouldHideVanillaPlayerName()) {
            cir.setReturnValue(null);
            return;
        }
        if (Configs.NAMETAGS_HIDE_ENTITY_HEALTH.getBooleanValue()) {
            Component value = cir.getReturnValue();
            if (value == null) return;
            String text = value.getString();
            String filtered = filterHealthText(text);
            if (!filtered.equals(text)) {
                cir.setReturnValue(Component.literal(filtered));
            }
        }
    }

    private static String filterHealthText(String text) {
        for (String pattern : Configs.NAMETAGS_ENTITY_HEALTH_PATTERNS.getStrings()) {
            if (pattern.isEmpty()) continue;
            try {
                text = text.replaceAll(pattern, "");
            } catch (Exception ignored) {
            }
        }
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }
}
