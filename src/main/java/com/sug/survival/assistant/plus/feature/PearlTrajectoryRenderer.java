package com.sug.survival.assistant.plus.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.sug.survival.assistant.plus.config.Configs;

public final class PearlTrajectoryRenderer {
    private static final Vec3[] POINTS = new Vec3[120];
    private static Vec3 simulatorPos = Vec3.ZERO;
    private static Vec3 simulatorVelocity = Vec3.ZERO;
    private static Vec3 hitPos;
    private static int pointCount;

    private PearlTrajectoryRenderer() {
    }

    public static void init() {
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(PearlTrajectoryRenderer::render);
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.PEARL_TRAJECTORY.getBooleanValue() || client.player == null || client.level == null) return;
        if (client.player.getMainHandItem().getItem() != Items.ENDER_PEARL && client.player.getOffhandItem().getItem() != Items.ENDER_PEARL) return;

        buildTrajectory(client);
        if (pointCount > 1) {
            renderTrajectory(context, Configs.PEARL_TRAJECTORY_COLOR.getColor());
        }
    }

    private static void buildTrajectory(Minecraft client) {
        pointCount = 0;
        hitPos = null;
        setSimulator(client.player);
        POINTS[pointCount++] = simulatorPos;

        for (int i = 1; i < POINTS.length; i++) {
            HitResult hit = tickSimulator(client);
            POINTS[pointCount++] = simulatorPos;
            if (hit != null || simulatorPos.y < client.level.getMinY()) return;
        }
    }

    private static void setSimulator(Entity user) {
        simulatorPos = user.getPosition(1.0F).add(0.0D, user.getEyeHeight(user.getPose()), 0.0D);

        double yaw = user.getYRot(1.0F) * (Math.PI / 180.0D);
        double pitch = user.getXRot(1.0F) * (Math.PI / 180.0D);
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        simulatorVelocity = new Vec3(x, y, z).normalize().scale(1.5D);
    }

    private static HitResult tickSimulator(Minecraft client) {
        Vec3 previous = simulatorPos;
        simulatorPos = simulatorPos.add(simulatorVelocity);

        HitResult hit = getCollision(client, previous, simulatorPos);
        if (hit != null && hit.getType() != HitResult.Type.MISS) {
            simulatorPos = hit.getLocation();
            hitPos = simulatorPos;
            return hit;
        }

        double drag = isTouchingWater(client) ? 0.8D : 0.99D;
        simulatorVelocity = simulatorVelocity.scale(drag).subtract(0.0D, 0.03D, 0.0D);
        return null;
    }

    private static HitResult getCollision(Minecraft client, Vec3 previous, Vec3 position) {
        HitResult blockHit = client.level.clip(new ClipContext(previous, position, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, client.player));
        if (blockHit.getType() != HitResult.Type.MISS) return blockHit;

        AABB box = new AABB(previous.x - 0.125D, previous.y, previous.z - 0.125D, previous.x + 0.125D, previous.y + 0.25D, previous.z + 0.125D)
                .expandTowards(simulatorVelocity)
                .inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(client.player, previous, position, box, entity -> !entity.isSpectator() && entity.isAlive() && entity.canBeHitByProjectile(), 0.0D);
        return entityHit;
    }

    private static boolean isTouchingWater(Minecraft client) {
        BlockPos pos = BlockPos.containing(simulatorPos);
        return client.level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static void renderTrajectory(LevelRenderContext context, Color4f color) {
        VertexConsumer consumer = context.bufferSource().getBuffer(RenderTypes.lines());
        Vec3 camera = context.levelState().cameraRenderState.pos;
        for (int i = 1; i < pointCount; i++) {
            Vec3 last = POINTS[i - 1].subtract(camera);
            Vec3 current = POINTS[i].subtract(camera);
            line(consumer, last.x, last.y, last.z, current.x, current.y, current.z, color);
        }

        if (hitPos != null) {
            Vec3 hit = hitPos.subtract(camera);
            renderHitAABB(consumer, hit, color);
        }
    }

    private static void renderHitAABB(VertexConsumer consumer, Vec3 pos, Color4f color) {
        double size = 0.25D;
        AABB box = new AABB(pos.x - size, pos.y - size, pos.z - size, pos.x + size, pos.y + size, pos.z + size);
        line(consumer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
        line(consumer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        line(consumer, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, color);
        line(consumer, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        line(consumer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
        line(consumer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
        line(consumer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        line(consumer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        line(consumer, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, color);
        line(consumer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
        line(consumer, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, color);
        line(consumer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
    }

    private static void line(VertexConsumer consumer, double x1, double y1, double z1, double x2, double y2, double z2, Color4f color) {
        float normalX = (float) (x2 - x1);
        float normalY = (float) (y2 - y1);
        float normalZ = (float) (z2 - z1);
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length == 0.0F) return;
        normalX /= length;
        normalY /= length;
        normalZ /= length;
        consumer.addVertex((float) x1, (float) y1, (float) z1).setColor(color.r, color.g, color.b, color.a).setNormal(normalX, normalY, normalZ).setLineWidth(1.0F);
        consumer.addVertex((float) x2, (float) y2, (float) z2).setColor(color.r, color.g, color.b, color.a).setNormal(normalX, normalY, normalZ).setLineWidth(1.0F);
    }
}
