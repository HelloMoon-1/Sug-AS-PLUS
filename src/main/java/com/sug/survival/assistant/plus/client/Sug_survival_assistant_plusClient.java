package com.sug.survival.assistant.plus.client;

import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.config.HotkeysCallback;
import com.sug.survival.assistant.plus.config.InputHandler;
import com.sug.survival.assistant.plus.config.OneConfigBridge;
import com.sug.survival.assistant.plus.feature.AutoEat;
import com.sug.survival.assistant.plus.feature.AutoTool;
import com.sug.survival.assistant.plus.feature.AutoTotem;
import com.sug.survival.assistant.plus.feature.EspRenderer;
import com.sug.survival.assistant.plus.feature.FireworkWarningHud;
import com.sug.survival.assistant.plus.feature.Freecam;
import com.sug.survival.assistant.plus.feature.GhostHand;
import com.sug.survival.assistant.plus.feature.HaoQiChongTian;
import com.sug.survival.assistant.plus.feature.NametagRenderer;
import com.sug.survival.assistant.plus.feature.NoTeleport;
import com.sug.survival.assistant.plus.feature.PearlTrajectoryRenderer;
import com.sug.survival.assistant.plus.feature.ShulkerRestock;
import com.sug.survival.assistant.plus.feature.SilentUseHotkeys;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sug_survival_assistant_plusClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("sug_survival_assistant_plus");
    public static final String MOD_ID = "sug_survival_assistant_plus";

    private int ticks;

    @Override
    public void onInitializeClient() {
        Configs.INSTANCE.load();
        HotkeysCallback.init();
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());

        OneConfigBridge.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> OneConfigBridge.flush());

        EspRenderer.init();
        PearlTrajectoryRenderer.init();
        NametagRenderer.init();
        FireworkWarningHud.init();
        ShulkerRestock.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Configs.HAO_QI_CHONG_TIAN.setBooleanValue(false));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            this.ticks++;
            ShulkerRestock.tick(client);
            OneConfigBridge.tick();
            FireworkWarningHud.tick(client);
            PearlTrajectoryRenderer.tick(client);
            SilentUseHotkeys.tick(client);
            NoTeleport.tick(client);
            Freecam.tick(client);
            HaoQiChongTian.tick(client);
            AutoEat.tick(client);
            AutoTool.tick(client);
            GhostHand.tick(client);
            EspRenderer.tick(client);
            if (this.ticks % Math.max(1, Configs.AUTO_TOTEM_INTERVAL.getIntegerValue()) == 0) {
                AutoTotem.tick(client);
            }
        });

        LOGGER.info("SUG Survival Assistant PLUS client loaded (26.2)");
    }
}
