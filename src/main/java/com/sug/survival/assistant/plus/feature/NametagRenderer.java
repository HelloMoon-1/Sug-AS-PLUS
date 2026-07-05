package com.sug.survival.assistant.plus.feature;

import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.sug.survival.assistant.plus.client.Sug_survival_assistant_plusClient;
import com.sug.survival.assistant.plus.config.Configs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NametagRenderer {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static Matrix4f positionMatrix;
    private static Matrix4f projectionMatrix;
    private static Vec3 cameraPos = Vec3.ZERO;

    private NametagRenderer() {
    }

    public static void init() {
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(NametagRenderer::collect);
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(Sug_survival_assistant_plusClient.MOD_ID, "nametags"),
                NametagRenderer::render
        );
    }

    public static boolean shouldHideVanillaPlayerName() {
        return Configs.NAMETAGS.getBooleanValue();
    }

    private static void collect(LevelRenderContext context) {
        ENTRIES.clear();
        Minecraft client = Minecraft.getInstance();
        if (!Configs.NAMETAGS.getBooleanValue() || client.player == null || client.level == null) return;

        positionMatrix = new Matrix4f(context.levelState().cameraRenderState.viewRotationMatrix);
        projectionMatrix = new Matrix4f(context.levelState().cameraRenderState.projectionMatrix);
        cameraPos = context.levelState().cameraRenderState.pos;

        double maxDistance = Configs.NAMETAGS_RANGE.getIntegerValue();
        for (Player player : client.level.players()) {
            if (player == client.player && Configs.NAMETAGS_IGNORE_SELF.getBooleanValue()) continue;
            double distance = client.player.distanceTo(player);
            if (distance > maxDistance) continue;
            Vec3 pos = player.getPosition(client.getDeltaTracker().getGameTimeDeltaPartialTick(false)).add(0.0D, player.getBbHeight() + 0.55D, 0.0D);
            ENTRIES.add(new Entry(player, pos, distance));
        }

        ENTRIES.sort(Comparator.comparingDouble(entry -> -entry.distance));
    }

    private static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.NAMETAGS.getBooleanValue() || client.player == null || positionMatrix == null || projectionMatrix == null) return;

        for (Entry entry : ENTRIES) {
            ScreenPos screen = toScreen(context, entry.worldPos);
            if (screen == null) continue;
            renderEntry(context, client, entry.player, screen.x, screen.y, entry.distance);
        }
    }

    private static ScreenPos toScreen(GuiGraphicsExtractor context, Vec3 worldPos) {
        Vector4f clip = new Vector4f((float) (worldPos.x - cameraPos.x), (float) (worldPos.y - cameraPos.y), (float) (worldPos.z - cameraPos.z), 1.0F);
        clip.mul(positionMatrix);
        clip.mul(projectionMatrix);
        if (clip.w <= 0.0F) return null;

        float invW = 1.0F / clip.w;
        float ndcX = clip.x * invW;
        float ndcY = clip.y * invW;
        if (ndcX < -1.2F || ndcX > 1.2F || ndcY < -1.2F || ndcY > 1.2F) return null;

        double x = (ndcX * 0.5F + 0.5F) * context.guiWidth();
        double y = (0.5F - ndcY * 0.5F) * context.guiHeight();
        return new ScreenPos(x, y);
    }

    private static void renderEntry(GuiGraphicsExtractor context, Minecraft client, Player player, double x, double y, double distance) {
        Font textRenderer = client.font;
        double distanceScale = Mth.clamp(1.0D - distance * 0.01D, 0.55D, 1.0D);
        float scale = (float) (Configs.NAMETAGS_SCALE.getDoubleValue() * distanceScale);

        List<TextPart> line = buildLine(client, textRenderer, player, distance);
        List<ItemStack> items = getItems(player);
        int textWidth = getLineWidth(line);
        int itemWidth = getItemRowWidth(items);
        int width = Math.max(textWidth, itemWidth);
        int itemHeight = Configs.NAMETAGS_EQUIPMENT.getBooleanValue() && !items.isEmpty() ? 22 : 0;
        int height = itemHeight + 12;

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate((float) x, (float) y);
        matrices.scale(scale, scale);

        int left = -width / 2 - 3;
        int top = -height;
        int right = width / 2 + 3;
        int bottom = 2;
        if (!Configs.NAMETAGS_TRANSPARENT_BACKGROUND.getBooleanValue()) {
            context.fill(left, top, right, bottom, toArgb(Configs.NAMETAGS_BACKGROUND_COLOR.getColor()));
        }

        drawLine(context, textRenderer, line, -textWidth / 2, -10);

        if (Configs.NAMETAGS_EQUIPMENT.getBooleanValue() && !items.isEmpty()) {
            renderItems(context, textRenderer, items, -itemWidth / 2, top + 2);
        }

        matrices.popMatrix();
    }

    private static List<TextPart> buildLine(Minecraft client, Font textRenderer, Player player, double distance) {
        List<TextPart> parts = new ArrayList<>();
        if (Configs.NAMETAGS_HEALTH.getBooleanValue()) {
            int health = Mth.ceil(player.getHealth() + player.getAbsorptionAmount());
            parts.add(new TextPart("❤" + health + " ", 0xFFFF5555, textRenderer.width("❤" + health + " ")));
        }

        String name = player.getName().getString();
        parts.add(new TextPart(name, toArgb(Configs.NAMETAGS_TEXT_COLOR.getColor()), textRenderer.width(name)));

        if (Configs.NAMETAGS_PING.getBooleanValue()) {
            PlayerInfo entry = client.getConnection() == null ? null : client.getConnection().getPlayerInfo(player.getUUID());
            if (entry != null) {
                int ping = entry.getLatency();
                String text = " " + ping + "ms";
                parts.add(new TextPart(text, pingColor(ping), textRenderer.width(text)));
            }
        }

        if (Configs.NAMETAGS_DISTANCE.getBooleanValue()) {
            String text = " " + Mth.floor(distance) + "m";
            parts.add(new TextPart(text, 0xFFAAAAAA, textRenderer.width(text)));
        }

        return parts;
    }

    private static int getLineWidth(List<TextPart> parts) {
        int width = 0;
        for (TextPart part : parts) width += part.width;
        return width;
    }

    private static void drawLine(GuiGraphicsExtractor context, Font textRenderer, List<TextPart> parts, int x, int y) {
        int offset = 0;
        for (TextPart part : parts) {
            context.text(textRenderer, part.text, x + offset, y, part.color, true);
            offset += part.width;
        }
    }

    private static int pingColor(int ping) {
        if (ping < 50) return 0xFF55FF55;
        if (ping <= 300) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static List<ItemStack> getItems(Player player) {
        List<ItemStack> items = new ArrayList<>();
        addIfVisible(items, player.getMainHandItem());
        addIfVisible(items, player.getItemBySlot(EquipmentSlot.HEAD));
        addIfVisible(items, player.getItemBySlot(EquipmentSlot.CHEST));
        addIfVisible(items, player.getItemBySlot(EquipmentSlot.LEGS));
        addIfVisible(items, player.getItemBySlot(EquipmentSlot.FEET));
        addIfVisible(items, player.getOffhandItem());
        return items;
    }

    private static void addIfVisible(List<ItemStack> items, ItemStack stack) {
        if (!stack.isEmpty()) items.add(stack);
    }

    private static int getItemRowWidth(List<ItemStack> items) {
        if (items.isEmpty()) return 0;
        return items.size() * 18 - 2;
    }

    private static void renderItems(GuiGraphicsExtractor context, Font textRenderer, List<ItemStack> items, int x, int y) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int itemX = x + i * 18;
            context.item(stack, itemX, y);
            if (stack.getCount() > 1) context.itemDecorations(textRenderer, stack, itemX, y);

            if (Configs.NAMETAGS_DURABILITY.getBooleanValue() && stack.isDamageableItem()) {
                drawDurabilityBar(context, stack, itemX + 2, y + 17);
            }
        }
    }

    private static void drawDurabilityBar(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        int durability = Mth.floor((stack.getMaxDamage() - stack.getDamageValue()) * 100.0F / stack.getMaxDamage());
        int width = Mth.clamp(Mth.floor(durability * 12.0F / 100.0F), 0, 12);
        context.fill(x, y, x + 12, y + 2, 0xAA000000);
        context.fill(x, y, x + width, y + 2, durabilityColor(durability));
    }

    private static int durabilityColor(int durability) {
        if (durability > 66) return 0xFF55FF55;
        if (durability > 33) return 0xFFFFFF55;
        return 0xFFFF5555;
    }

    private static int toArgb(Color4f color) {
        int a = Mth.clamp((int) (color.a * 255.0F), 0, 255);
        int r = Mth.clamp((int) (color.r * 255.0F), 0, 255);
        int g = Mth.clamp((int) (color.g * 255.0F), 0, 255);
        int b = Mth.clamp((int) (color.b * 255.0F), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private record TextPart(String text, int color, int width) {
    }

    private record Entry(Player player, Vec3 worldPos, double distance) {
    }

    private record ScreenPos(double x, double y) {
    }
}
