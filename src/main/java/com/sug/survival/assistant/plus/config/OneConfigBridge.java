package com.sug.survival.assistant.plus.config;

import com.sug.survival.assistant.plus.client.Sug_survival_assistant_plusClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional OneConfig bridge. This port intentionally does not require OneConfig.
 * Malilib remains the primary config UI.
 */
public final class OneConfigBridge {
    private static final String[] MOD_IDS = {"oneconfig", "oneconfigbootstrap", "oneconfigv1"};

    static {
        for (String id : MOD_IDS) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                Sug_survival_assistant_plusClient.LOGGER.info(
                        "OneConfig detected; bridge is stubbed in this port (malilib UI remains primary).");
                break;
            }
        }
    }

    private OneConfigBridge() {
    }

    public static boolean isAvailable() {
        return false;
    }

    public static void init() {
    }

    public static void tick() {
    }

    public static void flush() {
    }

    public static boolean openGui() {
        return false;
    }

    public static Screen createConfigScreen(Screen parent) {
        return null;
    }
}
