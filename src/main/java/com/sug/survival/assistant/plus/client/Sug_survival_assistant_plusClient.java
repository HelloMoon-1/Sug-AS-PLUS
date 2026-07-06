package com.sug.survival.assistant.plus.client;

import com.sug.survival.assistant.plus.feature.*;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.config.HotkeysCallback;
import com.sug.survival.assistant.plus.config.InputHandler;

public class Sug_survival_assistant_plusClient implements ClientModInitializer {
    public static final String MOD_ID = "sug_survival_assistant_plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private int ticks;

    @Override
    public void onInitializeClient() {
        Configs.INSTANCE.load();
        HotkeysCallback.init();
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
//        EspRenderer.init();
        PearlTrajectoryRenderer.init();
        NametagRenderer.init();
        FireworkWarningHud.init();
        ShulkerRestock.init();
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Configs.HAO_QI_CHONG_TIAN.setBooleanValue(false));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            ShulkerRestock.tick(client);
            SilentUseHotkeys.tick(client);
            NoTeleport.tick(client);
            Freecam.tick(client);
            HaoQiChongTian.tick(client);
            AutoEat.tick(client);
            AutoTool.tick(client);
            GhostHand.tick(client);
//            EspRenderer.tick(client);
            if (ticks % Math.max(1, Configs.AUTO_TOTEM_INTERVAL.getIntegerValue()) == 0) {
                AutoTotem.tick(client);
            }
        });

        LOGGER.info("SUG Survival Assistant PLUS client loaded");
    }
}
