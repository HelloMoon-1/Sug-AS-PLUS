package com.sug.survival.assistant.plus.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.sug.survival.assistant.plus.feature.CommandCompletionFilter;
import java.util.List;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow
    @Final
    private EditBox input;

    @Inject(method = "sortSuggestions", at = @At("RETURN"), cancellable = true)
    private void sug_survival_assistant_plus$sortSuggestions(
            Suggestions suggestions,
            CallbackInfoReturnable<List<Suggestion>> cir
    ) {
        cir.setReturnValue(CommandCompletionFilter.filter(
                this.input.getValue(),
                this.input.getCursorPosition(),
                cir.getReturnValue()
        ));
    }
}
