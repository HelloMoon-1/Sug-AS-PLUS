package com.sug.survival.assistant.plus.config;

import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;

public class HotkeysCallback implements IHotkeyCallback {
    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key) {
        if (key == Configs.OPEN_CONFIG.getKeybind()) {
            // OneConfig is optional and not required for this port.
            if (!OneConfigBridge.openGui()) {
                Minecraft.getInstance().gui.setScreen(new ConfigUi());
            }
            return true;
        }
        return false;
    }

    public static void init() {
        HotkeysCallback callback = new HotkeysCallback();
        Configs.OPEN_CONFIG.getKeybind().setCallback(callback);
    }
}
