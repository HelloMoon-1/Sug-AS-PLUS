package com.sug.survival.assistant.plus.feature;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.sug.survival.assistant.plus.client.Sug_survival_assistant_plusClient;
import com.sug.survival.assistant.plus.config.Configs;

public final class FireworkWarningHud {
    private FireworkWarningHud() {
    }

    public static void init() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Sug_survival_assistant_plusClient.MOD_ID, "firework_warning"),
                (context, tickCounter) -> render(context)
        );
    }

    private static void render(GuiGraphicsExtractor context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int y = context.guiHeight() - 68;
        if (Configs.FIREWORK_WARNING.getBooleanValue()) {
            int count = countInventoryItems(client, Items.FIREWORK_ROCKET);
            int threshold = Configs.FIREWORK_WARNING_THRESHOLD.getIntegerValue();
            if (count <= threshold) {
                drawCentered(context, client, Component.literal("烟花: " + count + " / " + threshold), y, 0xFFFF5555);
                y -= 10;
            }
        }

        if (Configs.TOTEM_WARNING.getBooleanValue()) {
            int inventoryCount = countInventoryItems(client, Items.TOTEM_OF_UNDYING);
            int shulkerCount = ShulkerRestock.countInShulkers(client, Items.TOTEM_OF_UNDYING);
            int total = inventoryCount + shulkerCount;
            int threshold = Configs.TOTEM_WARNING_THRESHOLD.getIntegerValue();
            if (total <= threshold) {
                drawCentered(context, client, Component.literal("请及时补充图腾: " + total + " / " + threshold + "（背包 " + inventoryCount + "，盒子 " + shulkerCount + "）"), y, 0xFFFF5555);
            }
        }
    }

    private static int countInventoryItems(Minecraft client, net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = client.player.getInventory().getItem(slot);
            if (stack.getItem() == item) count += stack.getCount();
        }
        ItemStack offHandStack = client.player.getOffhandItem();
        if (offHandStack.getItem() == item) count += offHandStack.getCount();
        return count;
    }

    private static void drawCentered(GuiGraphicsExtractor context, Minecraft client, Component text, int y, int color) {
        int x = (context.guiWidth() - client.font.width(text)) / 2;
        context.text(client.font, text, x, y, color, true);
    }
}
