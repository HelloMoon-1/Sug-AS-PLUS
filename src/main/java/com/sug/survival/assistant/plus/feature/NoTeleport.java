package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NoTeleport {
    private static final int SIMULATION_TICKS = 120;
    private static final double PORTAL_SCAN_RADIUS = 0.35D;

    private NoTeleport() {
    }

    public static boolean isActive() {
        return Configs.NO_TELEPORT.getBooleanValue();
    }

    public static void tick(Minecraft client) {
        if (!isActive() || client.player == null || client.level == null) return;
        pushOutOfPortal(client.player, client.level);
    }

    public static boolean shouldBlockEnderPearlUse(Level level, Player player, InteractionHand hand) {
        if (!isActive() || level == null || player == null) return false;
        if (!player.getItemInHand(hand).is(Items.ENDER_PEARL)) return false;

        Vec3 position = player.getPosition(1.0F).add(0.0D, player.getEyeHeight(player.getPose()), 0.0D);
        Vec3 velocity = pearlVelocity(player);

        for (int i = 1; i < SIMULATION_TICKS; i++) {
            Vec3 previous = position;
            position = position.add(velocity);
            if (segmentTouchesPortal(level, previous, position)) return true;

            HitResult hit = getCollision(level, player, previous, position, velocity);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                return positionTouchesPortal(level, hit.getLocation());
            }

            double drag = level.getFluidState(BlockPos.containing(position)).is(FluidTags.WATER) ? 0.8D : 0.99D;
            velocity = velocity.scale(drag).subtract(0.0D, 0.03D, 0.0D);
            if (position.y < level.getMinY()) return false;
        }
        return false;
    }

    private static Vec3 pearlVelocity(Entity user) {
        double yaw = user.getYRot(1.0F) * (Math.PI / 180.0D);
        double pitch = user.getXRot(1.0F) * (Math.PI / 180.0D);
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return new Vec3(x, y, z).normalize().scale(1.5D);
    }

    private static HitResult getCollision(Level level, Player player, Vec3 previous, Vec3 position, Vec3 velocity) {
        HitResult blockHit = level.clip(new ClipContext(previous, position, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        if (blockHit.getType() != HitResult.Type.MISS) return blockHit;

        AABB box = new AABB(previous.x - 0.125D, previous.y, previous.z - 0.125D, previous.x + 0.125D, previous.y + 0.25D, previous.z + 0.125D)
                .expandTowards(velocity)
                .inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, previous, position, box, entity -> !entity.isSpectator() && entity.isAlive() && entity.canBeHitByProjectile(), 0.0D);
        return entityHit;
    }

    private static boolean segmentTouchesPortal(Level level, Vec3 start, Vec3 end) {
        int minX = (int) Math.floor(Math.min(start.x, end.x) - PORTAL_SCAN_RADIUS);
        int minY = (int) Math.floor(Math.min(start.y, end.y) - PORTAL_SCAN_RADIUS);
        int minZ = (int) Math.floor(Math.min(start.z, end.z) - PORTAL_SCAN_RADIUS);
        int maxX = (int) Math.floor(Math.max(start.x, end.x) + PORTAL_SCAN_RADIUS);
        int maxY = (int) Math.floor(Math.max(start.y, end.y) + PORTAL_SCAN_RADIUS);
        int maxZ = (int) Math.floor(Math.max(start.z, end.z) + PORTAL_SCAN_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) return true;
        }
        return false;
    }

    private static boolean positionTouchesPortal(Level level, Vec3 position) {
        int minX = (int) Math.floor(position.x - PORTAL_SCAN_RADIUS);
        int minY = (int) Math.floor(position.y - PORTAL_SCAN_RADIUS);
        int minZ = (int) Math.floor(position.z - PORTAL_SCAN_RADIUS);
        int maxX = (int) Math.floor(position.x + PORTAL_SCAN_RADIUS);
        int maxY = (int) Math.floor(position.y + PORTAL_SCAN_RADIUS);
        int maxZ = (int) Math.floor(position.z + PORTAL_SCAN_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) return true;
        }
        return false;
    }

    private static void pushOutOfPortal(LocalPlayer player, Level level) {
        AABB box = player.getBoundingBox().inflate(0.001D);
        if (!boxTouchesPortal(level, box)) return;

        double[][] candidates = {
                {0.0D, -0.81D},
                {0.0D, 0.81D},
                {-0.81D, 0.0D},
                {0.81D, 0.0D}
        };
        for (double[] candidate : candidates) {
            AABB movedBox = box.move(candidate[0], 0.0D, candidate[1]);
            if (boxTouchesPortal(level, movedBox)) continue;
            player.setPos(player.getX() + candidate[0], player.getY(), player.getZ() + candidate[1]);
            Vec3 velocity = player.getDeltaMovement();
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);
            return;
        }

        int highestPortalY = highestPortalY(level, box);
        if (highestPortalY == Integer.MIN_VALUE) return;
        player.setPos(player.getX(), highestPortalY + 1.01D, player.getZ());
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
    }

    private static boolean boxTouchesPortal(Level level, AABB box) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    mutable.set(x, y, z);
                    if (level.getBlockState(mutable).is(Blocks.NETHER_PORTAL)) return true;
                }
            }
        }
        return false;
    }

    private static int highestPortalY(Level level, AABB box) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int highest = Integer.MIN_VALUE;
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    mutable.set(x, y, z);
                    if (level.getBlockState(mutable).is(Blocks.NETHER_PORTAL)) highest = Math.max(highest, y);
                }
            }
        }
        return highest;
    }
}
