package com.sug.survival.assistant.plus.mixin;

import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.feature.NametagRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {
    @Unique
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Unique
    private static List<String> healthPatternSource = List.of();

    @Unique
    private static List<Pattern> healthPatterns = List.of();

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void sug_survival_assistant_plus$getNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player && NametagRenderer.shouldHideVanillaPlayerName()) {
            cir.setReturnValue(null);
            return;
        }

        if (!Configs.NAMETAGS_HIDE_ENTITY_HEALTH.getBooleanValue()) {
            return;
        }

        Component value = cir.getReturnValue();
        if (value == null) {
            return;
        }

        String text = value.getString();
        String filtered = filterHealthText(text);
        if (!filtered.equals(text)) {
            cir.setReturnValue(Component.literal(filtered));
        }
    }

    @Unique
    private static String filterHealthText(String text) {
        refreshHealthPatterns();
        for (Pattern pattern : healthPatterns) {
            text = pattern.matcher(text).replaceAll("");
        }
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    @Unique
    private static void refreshHealthPatterns() {
        List<String> configured = Configs.NAMETAGS_ENTITY_HEALTH_PATTERNS.getStrings();
        List<String> current = configured == null ? List.of() : new ArrayList<>(configured);
        if (current.equals(healthPatternSource)) {
            return;
        }

        List<Pattern> compiled = new ArrayList<>();
        for (String expression : current) {
            if (expression == null || expression.isBlank() || Configs.isFeatureUnlockToken(expression)) {
                continue;
            }
            try {
                compiled.add(Pattern.compile(expression));
            } catch (PatternSyntaxException ignored) {
            }
        }

        healthPatternSource = current;
        healthPatterns = List.copyOf(compiled);
    }
}
