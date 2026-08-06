package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PearlTrajectoryRenderer {
    private static final Vec3[] POINTS = new Vec3[120];
    private static Vec3 simulatorPos = Vec3.ZERO;
    private static Vec3 simulatorVelocity = Vec3.ZERO;
    private static Vec3 hitPos;
    private static int pointCount;

    private PearlTrajectoryRenderer() {
    }

    public static void init() {
        LevelRenderEvents.BEFORE_GIZMOS.register(PearlTrajectoryRenderer::render);
    }

    public static void tick(Minecraft client) {
        if (!Configs.PEARL_TRAJECTORY.getBooleanValue() || client.player == null || client.level == null) {
            clearCache();
            return;
        }
        boolean mainHandPearl = client.player.getMainHandItem().getItem() == Items.ENDER_PEARL;
        boolean offhandPearl = client.player.getOffhandItem().getItem() == Items.ENDER_PEARL;
        if (!mainHandPearl && !offhandPearl) {
            clearCache();
            return;
        }
        buildTrajectory(client);
    }

    private static void clearCache() {
        pointCount = 0;
        hitPos = null;
    }

    private static void render(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (!Configs.PEARL_TRAJECTORY.getBooleanValue() || client.player == null || client.level == null) {
            return;
        }
        if (client.player.getMainHandItem().getItem() != Items.ENDER_PEARL
                && client.player.getOffhandItem().getItem() != Items.ENDER_PEARL) {
            return;
        }
        if (pointCount > 1) {
            renderTrajectory(Configs.PEARL_TRAJECTORY_COLOR.getColor());
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
            if (hit != null || simulatorPos.y < client.level.getMinY()) {
                return;
            }
        }
    }

    private static void setSimulator(Entity user) {
        simulatorPos = user.getPosition(1.0F).add(0.0D, user.getEyeHeight(user.getPose()), 0.0D);
        double yaw = user.getYRot(1.0F) * 0.017453292519943295D;
        double pitch = user.getXRot(1.0F) * 0.017453292519943295D;
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
        BlockHitResult blockHitResult = client.level.clip(new ClipContext(
                previous,
                position,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                client.player
        ));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult;
        }
        AABB box = new AABB(
                previous.x - 0.125D,
                previous.y,
                previous.z - 0.125D,
                previous.x + 0.125D,
                previous.y + 0.25D,
                previous.z + 0.125D
        ).expandTowards(simulatorVelocity).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                client.player,
                previous,
                position,
                box,
                entity -> !entity.isSpectator() && entity.isAlive() && entity.canBeHitByProjectile(),
                0.0D
        );
    }

    private static boolean isTouchingWater(Minecraft client) {
        BlockPos pos = BlockPos.containing((Position) simulatorPos);
        return client.level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static void renderTrajectory(Color4f color) {
        int argb = color.toVanillaArgb();
        for (int i = 1; i < pointCount; i++) {
            Gizmos.line(POINTS[i - 1], POINTS[i], argb, 2.0F);
        }
        if (hitPos != null) {
            double size = 0.25D;
            AABB box = new AABB(
                    hitPos.x - size,
                    hitPos.y - size,
                    hitPos.z - size,
                    hitPos.x + size,
                    hitPos.y + size,
                    hitPos.z + size
            );
            Gizmos.cuboid(box, GizmoStyle.stroke(argb, 2.0F));
        }
    }
}
