package com.sug.survival.assistant.plus.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.List;

public class ConfigUi extends GuiConfigsBase {
    private static Tab tab = Tab.ALL;

    public ConfigUi() {
        super(10, 50, "sug_survival_assistant_plus", null, "SUG Survival Assistant PLUS 配置界面");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;
        for (Tab value : Tab.values()) {
            x += this.createButton(x, y, -1, value);
        }
    }

    private int createButton(int x, int y, int width, Tab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getTranslate());
        button.setEnabled(ConfigUi.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs = Configs.getConfigsForTab(tab);
        return ConfigOptionWrapper.createFor(configs);
    }

    private static class ButtonListener implements IButtonActionListener {
        private final ConfigUi parent;
        private final Tab tab;

        private ButtonListener(Tab tab, ConfigUi parent) {
            this.tab = tab;
            this.parent = parent;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            ConfigUi.tab = this.tab;
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    public enum Tab {
        ALL("所有"),
        GENERAL("通用"),
        ASSIST("辅助"),
        VISUAL("视觉"),
        CHAT("聊天");

        private final String translation;

        Tab(String translation) {
            this.translation = translation;
        }

        public String getTranslate() {
            return StringUtils.translate(this.translation);
        }
    }
}
