package com.sug.survival.assistant.plus.config;

import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        Configs.KEY_LIST.forEach(hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
        Configs.SWITCH_KEY.forEach(hotkey -> manager.addKeybindToMap(hotkey.getKeybind()));
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory("sug_survival_assistant_plus", "按下式", Configs.KEY_LIST);
        manager.addHotkeysForCategory("sug_survival_assistant_plus", "切换式", Configs.SWITCH_KEY);
    }

    public static InputHandler getInstance() {
        return INSTANCE;
    }
}
