package com.sug.survival.assistant.plus.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            Screen oneConfig = OneConfigBridge.createConfigScreen(parent);
            if (oneConfig != null) {
                return oneConfig;
            }
            ConfigUi ui = new ConfigUi();
            ui.setParent(parent);
            return ui;
        };
    }
}
