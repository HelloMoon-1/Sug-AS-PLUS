package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class SilentUseHotkeys {
    private static boolean pearlWasPressed;
    private static boolean fireworkWasPressed;

    private SilentUseHotkeys() {
    }

    public static void tick(Minecraft client) {
        if (!InventoryHelper.canOperateInventory(client)) {
            pearlWasPressed = false;
            fireworkWasPressed = false;
            return;
        }

        SilentUseAction.tick();
        if (client.gui.screen() != null) {
            return;
        }

        boolean pearlPressed = isPressed(client, Configs.SILENT_PEARL.getKeybind());
        boolean fireworkPressed = isPressed(client, Configs.SILENT_FIREWORK.getKeybind());
        if (pearlPressed && !pearlWasPressed) {
            SilentUseAction.usePearl();
        }
        if (fireworkPressed && !fireworkWasPressed) {
            SilentUseAction.useFirework();
        }
        pearlWasPressed = pearlPressed;
        fireworkWasPressed = fireworkPressed;
    }

    private static boolean isPressed(Minecraft client, IKeybind keybind) {
        if (!keybind.isValid() || keybind.getKeys().isEmpty()) {
            return false;
        }
        long handle = client.getWindow().handle();
        for (Integer keyObj : keybind.getKeys()) {
            int key = keyObj;
            if (key < 0) {
                int button = key + 100;
                if (button < 0 || button > 7) {
                    return false;
                }
                if (GLFW.glfwGetMouseButton(handle, button) != 1) {
                    return false;
                }
            } else if (GLFW.glfwGetKey(handle, key) != 1) {
                return false;
            }
        }
        return true;
    }
}
