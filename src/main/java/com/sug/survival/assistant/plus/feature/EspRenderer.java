package com.sug.survival.assistant.plus.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.sug.survival.assistant.plus.config.Configs;

import java.util.*;

public final class EspRenderer {
    private static final Map<Long, CachedChunk> CHUNK_CACHE = new HashMap<>();
    private static final ArrayDeque<ChunkCoord> SCAN_QUEUE = new ArrayDeque<>();
    private static final int CHUNK_SCAN_BUDGET = 1;
    private static final int REFRESH_INTERVAL = 80;
    private static final int MAX_RENDER_BOXES = 4096;
    private static int lastContainerEspRange = -1;
    private static boolean lastContainerEspEnabled;
    private static String lastBlockEspBlocks = "";
    private static int lastBlockEspRange = -1;
    private static boolean lastBlockEspEnabled;
    private static int lastChunkX = Integer.MIN_VALUE;
    private static int lastChunkZ = Integer.MIN_VALUE;
    private static int ticks;

    private EspRenderer() {
    }

    public static void init() {
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(EspRenderer::render);
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        boolean containerEnabled = Configs.CONTAINER_ESP.getBooleanValue();
        boolean blockEnabled = Configs.BLOCK_ESP.getBooleanValue();
        if (!containerEnabled && !blockEnabled) {
            clearState();
            return;
        }

        int containerRange = Configs.CONTAINER_ESP_RANGE.getIntegerValue();
        int blockRange = Configs.BLOCK_ESP_RANGE.getIntegerValue();
        String blockEspBlocks = String.join(",", Configs.BLOCK_ESP_BLOCKS.getStrings());
        BlockPos center = client.player.blockPosition();
        int chunkX = center.getX() >> 4;
        int chunkZ = center.getZ() >> 4;

        boolean settingsChanged = lastContainerEspEnabled != containerEnabled
                || lastContainerEspRange != containerRange
                || lastBlockEspEnabled != blockEnabled
                || lastBlockEspRange != blockRange
                || !lastBlockEspBlocks.equals(blockEspBlocks);
        boolean movedChunk = chunkX != lastChunkX || chunkZ != lastChunkZ;

        lastContainerEspEnabled = containerEnabled;
        lastContainerEspRange = containerRange;
        lastBlockEspEnabled = blockEnabled;
        lastBlockEspRange = blockRange;
        lastBlockEspBlocks = blockEspBlocks;
        lastChunkX = chunkX;
        lastChunkZ = chunkZ;

        int range = Math.max(containerEnabled ? containerRange : 0, blockEnabled ? blockRange : 0);
        if (settingsChanged) {
            rebuildChunkQueue(client, range, true);
        } else if (movedChunk || ticks % REFRESH_INTERVAL == 0 || (CHUNK_CACHE.isEmpty() && SCAN_QUEUE.isEmpty())) {
            rebuildChunkQueue(client, range, false);
        }

        processChunkQueue(client, CHUNK_SCAN_BUDGET);
        ticks++;
    }

    private static void clearState() {
        CHUNK_CACHE.clear();
        SCAN_QUEUE.clear();
        lastContainerEspEnabled = false;
        lastBlockEspEnabled = false;
        lastChunkX = Integer.MIN_VALUE;
        lastChunkZ = Integer.MIN_VALUE;
    }

    private static void rebuildChunkQueue(Minecraft client, int range, boolean clearCache) {
        if (clearCache) CHUNK_CACHE.clear();
        SCAN_QUEUE.clear();

        BlockPos center = client.player.blockPosition();
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRange = (range + 15) >> 4;

        CHUNK_CACHE.entrySet().removeIf(entry -> !isChunkInRange(chunkX(entry.getKey()), chunkZ(entry.getKey()), centerChunkX, centerChunkZ, chunkRange));

        List<ChunkCoord> chunks = new ArrayList<>();
        for (int x = centerChunkX - chunkRange; x <= centerChunkX + chunkRange; x++) {
            for (int z = centerChunkZ - chunkRange; z <= centerChunkZ + chunkRange; z++) {
                if (isChunkInRange(x, z, centerChunkX, centerChunkZ, chunkRange)) chunks.add(new ChunkCoord(x, z));
            }
        }

        chunks.sort(Comparator.comparingInt(chunk -> chunk.distanceSquared(centerChunkX, centerChunkZ)));
        SCAN_QUEUE.addAll(chunks);
    }

    private static void processChunkQueue(Minecraft client, int budget) {
        Set<Block> blockTargets = getBlockTargets();
        int processed = 0;
        while (!SCAN_QUEUE.isEmpty() && processed < budget) {
            ChunkCoord chunk = SCAN_QUEUE.removeFirst();
            CHUNK_CACHE.put(chunkKey(chunk.x, chunk.z), scanChunk(client, chunk, blockTargets));
            processed++;
        }
    }

    private static CachedChunk scanChunk(Minecraft client, ChunkCoord chunk, Set<Block> blockTargets) {
        CachedChunk cached = new CachedChunk();
        BlockPos center = client.player.blockPosition();
        boolean containerEnabled = Configs.CONTAINER_ESP.getBooleanValue();
        boolean blockEnabled = Configs.BLOCK_ESP.getBooleanValue();
        int containerRange = Configs.CONTAINER_ESP_RANGE.getIntegerValue();
        int blockRange = Configs.BLOCK_ESP_RANGE.getIntegerValue();
        int scanRange = Math.max(containerEnabled ? containerRange : 0, blockEnabled ? blockRange : 0);
        int minY = Math.max(client.level.getMinY(), center.getY() - scanRange);
        int maxY = Math.min(client.level.getMaxY(), center.getY() + scanRange);
        int minX = chunk.x << 4;
        int minZ = chunk.z << 4;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(x, y, z);
                    BlockState state = client.level.getBlockState(mutable);
                    Block block = state.getBlock();
                    if (containerEnabled && isWithinRange(center, mutable, containerRange) && isContainer(block)) {
                        addShapeBoxes(client, mutable, state, (currentState, currentBlock) -> isContainer(currentBlock), cached.containerBoxes);
                    }
                    if (blockEnabled && !blockTargets.isEmpty() && isWithinRange(center, mutable, blockRange)) {
                        if ((!containerEnabled || !isContainer(block)) && blockTargets.contains(block)) {
                            addShapeBoxes(client, mutable, state, (currentState, currentBlock) -> blockTargets.contains(currentBlock), cached.blockBoxes);
                        }
                    }
                }
            }
        }

        return cached;
    }

    private static Set<Block> getBlockTargets() {
        Set<Block> blocks = new HashSet<>();
        for (String value : Configs.BLOCK_ESP_BLOCKS.getStrings()) {
            for (String entry : value.split("[\\s,]+")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) continue;
                Identifier id = Identifier.tryParse(trimmed.contains(":") ? trimmed : "minecraft:" + trimmed);
                if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) blocks.add(BuiltInRegistries.BLOCK.getValue(id));
            }
        }
        return blocks;
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        if (Configs.CONTAINER_ESP.getBooleanValue()) {
            renderBoxes(context, collectBoxes(true), Configs.CONTAINER_ESP_LINE_COLOR.getColor(), Configs.CONTAINER_ESP_FILL_COLOR.getColor(), false, Configs.CONTAINER_ESP_RANGE.getIntegerValue(), false);
        }

        if (Configs.BLOCK_ESP.getBooleanValue()) {
            renderBoxes(context, collectBoxes(false), Configs.BLOCK_ESP_LINE_COLOR.getColor(), Configs.BLOCK_ESP_FILL_COLOR.getColor(), true, Configs.BLOCK_ESP_RANGE.getIntegerValue(), false);
        }
    }

    private static List<RenderAABB> collectBoxes(boolean containers) {
        List<RenderAABB> boxes = new ArrayList<>();
        for (CachedChunk chunk : CHUNK_CACHE.values()) {
            boxes.addAll(containers ? chunk.containerBoxes : chunk.blockBoxes);
        }
        return boxes;
    }

    private static void addShapeBoxes(Minecraft client, BlockPos pos, BlockState state, BlockPredicate predicate, List<RenderAABB> output) {
        BlockPos immutablePos = pos.immutable();
        output.removeIf(box -> box.pos.equals(immutablePos));

        VoxelShape shape = state.getShape(client.level, pos);
        if (shape.isEmpty()) {
            output.add(new RenderAABB(immutablePos, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D), predicate));
            return;
        }

        for (AABB box : shape.toAabbs()) {
            output.add(new RenderAABB(immutablePos, box.move(pos), predicate));
        }
    }

    private static void renderBoxes(LevelRenderContext context, List<RenderAABB> boxes, Color4f lineColor, Color4f fillColor, boolean renderFill, int range, boolean mergeAdjacent) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        MultiBufferSource consumers = context.bufferSource();
        if (consumers == null) return;

        BlockPos center = client.player.blockPosition();
        Vec3 camera = context.levelState().cameraRenderState.pos;
        List<AABB> renderBoxes = new ArrayList<>();
        for (RenderAABB renderAABB : boxes) {
            if (renderBoxes.size() >= MAX_RENDER_BOXES) break;
            if (!isWithinRange(center, renderAABB.pos, range)) continue;
            BlockState state = client.level.getBlockState(renderAABB.pos);
            if (!renderAABB.predicate.matches(state, state.getBlock())) continue;
            if (mergeAdjacent) mergeAABB(renderBoxes, renderAABB.box);
            else renderBoxes.add(renderAABB.box);
        }

        if (renderFill) {
            VertexConsumer fillConsumer = consumers.getBuffer(RenderTypes.debugFilledBox());
            float fillAlpha = Math.min(fillColor.a, 0.22F);
            PoseStack matrices = context.poseStack();
            for (AABB box : renderBoxes) {
                drawFilledAABB(matrices, fillConsumer, box.move(-camera.x, -camera.y, -camera.z), fillColor, fillAlpha);
            }
        }

        VertexConsumer lineConsumer = consumers.getBuffer(RenderTypes.lines());
        for (AABB box : renderBoxes) {
            drawWireAABB(lineConsumer, box.move(-camera.x, -camera.y, -camera.z), lineColor);
        }
    }

    private static void drawFilledAABB(PoseStack matrices, VertexConsumer consumer, AABB box, Color4f color, float alpha) {
        PoseStack.Pose pose = matrices.last();
        quad(consumer, pose, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color, alpha);
        quad(consumer, pose, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color, alpha);
        quad(consumer, pose, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, color, alpha);
        quad(consumer, pose, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color, alpha);
        quad(consumer, pose, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color, alpha);
        quad(consumer, pose, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ, color, alpha);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color4f color, float alpha) {
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color.r, color.g, color.b, alpha);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color.r, color.g, color.b, alpha);
        consumer.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(color.r, color.g, color.b, alpha);
        consumer.addVertex(pose, (float) x4, (float) y4, (float) z4).setColor(color.r, color.g, color.b, alpha);
    }

    private static void drawWireAABB(VertexConsumer consumer, AABB box, Color4f color) {
        float r = color.r * 0.35F;
        float g = color.g * 0.35F;
        float b = color.b * 0.35F;
        float a = Math.min(1.0F, color.a + 0.6F);

        line(consumer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        line(consumer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        line(consumer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        line(consumer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);

        line(consumer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(consumer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(consumer, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        line(consumer, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);

        line(consumer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        line(consumer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        line(consumer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        line(consumer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void line(VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, float r, float g, float b, float a) {
        float normalX = (float) (x2 - x1);
        float normalY = (float) (y2 - y1);
        float normalZ = (float) (z2 - z1);
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length == 0.0F) return;
        normalX /= length;
        normalY /= length;
        normalZ /= length;
        consumer.addVertex((float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(normalX, normalY, normalZ).setLineWidth(1.0F);
        consumer.addVertex((float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(normalX, normalY, normalZ).setLineWidth(1.0F);
    }

    private static void mergeAABB(List<AABB> boxes, AABB box) {
        for (int i = 0; i < boxes.size(); i++) {
            AABB existing = boxes.get(i);
            if (touchesOrOverlaps(existing, box)) {
                boxes.set(i, existing.minmax(box));
                return;
            }
        }
        boxes.add(box);
    }

    private static boolean touchesOrOverlaps(AABB a, AABB b) {
        double epsilon = 1.0E-4D;
        boolean overlapsY = a.minY <= b.maxY + epsilon && a.maxY + epsilon >= b.minY;
        boolean overlapsX = a.minX <= b.maxX + epsilon && a.maxX + epsilon >= b.minX;
        boolean overlapsZ = a.minZ <= b.maxZ + epsilon && a.maxZ + epsilon >= b.minZ;
        boolean touchesX = Math.abs(a.maxX - b.minX) < epsilon || Math.abs(b.maxX - a.minX) < epsilon;
        boolean touchesZ = Math.abs(a.maxZ - b.minZ) < epsilon || Math.abs(b.maxZ - a.minZ) < epsilon;
        return overlapsY && ((touchesX && overlapsZ) || (touchesZ && overlapsX) || (overlapsX && overlapsZ));
    }

    private static boolean isWithinRange(BlockPos center, BlockPos pos, int range) {
        return center.distSqr(pos) <= range * range;
    }

    private static boolean isChunkInRange(int chunkX, int chunkZ, int centerChunkX, int centerChunkZ, int chunkRange) {
        int dx = chunkX - centerChunkX;
        int dz = chunkZ - centerChunkZ;
        return dx * dx + dz * dz <= chunkRange * chunkRange;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static int chunkX(long key) {
        return (int) (key & 0xFFFFFFFFL);
    }

    private static int chunkZ(long key) {
        return (int) (key >>> 32);
    }

    public static boolean isContainer(Block block) {
        return block instanceof ChestBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof BarrelBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block == Blocks.ENDER_CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER;
    }

    private record ChunkCoord(int x, int z) {
        private int distanceSquared(int centerX, int centerZ) {
            int dx = x - centerX;
            int dz = z - centerZ;
            return dx * dx + dz * dz;
        }
    }

    private static final class CachedChunk {
        private final List<RenderAABB> containerBoxes = new ArrayList<>();
        private final List<RenderAABB> blockBoxes = new ArrayList<>();
    }

    private record RenderAABB(BlockPos pos, AABB box, BlockPredicate predicate) {
    }

    private interface BlockPredicate {
        boolean matches(BlockState state, Block block);
    }
}
