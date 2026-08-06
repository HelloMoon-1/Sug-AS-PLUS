package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class NoTeleport {
    private static final int SIMULATION_TICKS = 120;
    private static final double PORTAL_SCAN_RADIUS = 0.35D;
    private static final double[] PUSH_DISTANCES = {0.35D, 0.55D, 0.75D, 1.05D};

    private NoTeleport() {
    }

    public static boolean isActive() {
        return Configs.NO_TELEPORT.getBooleanValue();
    }

    public static void tick(Minecraft client) {
        if (!isActive() || client.player == null || client.level == null) {
            return;
        }
        pushOutOfPortal(client.player, client.level);
    }

    public static boolean shouldBlockEnderPearlUse(Level level, Player player, InteractionHand hand) {
        if (!isActive() || level == null || player == null) {
            return false;
        }
        if (!player.getItemInHand(hand).is(Items.ENDER_PEARL)) {
            return false;
        }

        Vec3 position = player.getPosition(1.0F).add(0.0D, player.getEyeHeight(player.getPose()), 0.0D);
        Vec3 velocity = pearlVelocity(player);
        for (int i = 1; i < SIMULATION_TICKS; i++) {
            Vec3 previous = position;
            position = position.add(velocity);
            if (segmentTouchesPortal(level, previous, position)) {
                return true;
            }
            HitResult hit = getCollision(level, player, previous, position, velocity);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                return positionTouchesPortal(level, hit.getLocation());
            }
            double drag = level.getFluidState(BlockPos.containing((Position) position)).is(FluidTags.WATER) ? 0.8D : 0.99D;
            velocity = velocity.scale(drag).subtract(0.0D, 0.03D, 0.0D);
            if (position.y < level.getMinY()) {
                return false;
            }
        }
        return false;
    }

    private static Vec3 pearlVelocity(Entity user) {
        double yaw = user.getYRot(1.0F) * 0.017453292519943295D;
        double pitch = user.getXRot(1.0F) * 0.017453292519943295D;
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return new Vec3(x, y, z).normalize().scale(1.5D);
    }

    private static HitResult getCollision(Level level, Player player, Vec3 previous, Vec3 position, Vec3 velocity) {
        BlockHitResult blockHitResult = level.clip(new ClipContext(
                previous,
                position,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                player
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
        ).expandTowards(velocity).inflate(1.0D);
        return ProjectileUtil.getEntityHitResult(
                player,
                previous,
                position,
                box,
                entity -> !entity.isSpectator() && entity.isAlive() && entity.canBeHitByProjectile(),
                0.0D
        );
    }

    private static boolean segmentTouchesPortal(Level level, Vec3 start, Vec3 end) {
        int minX = (int) Math.floor(Math.min(start.x, end.x) - PORTAL_SCAN_RADIUS);
        int minY = (int) Math.floor(Math.min(start.y, end.y) - PORTAL_SCAN_RADIUS);
        int minZ = (int) Math.floor(Math.min(start.z, end.z) - PORTAL_SCAN_RADIUS);
        int maxX = (int) Math.floor(Math.max(start.x, end.x) + PORTAL_SCAN_RADIUS);
        int maxY = (int) Math.floor(Math.max(start.y, end.y) + PORTAL_SCAN_RADIUS);
        int maxZ = (int) Math.floor(Math.max(start.z, end.z) + PORTAL_SCAN_RADIUS);
        for (BlockPos pos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                return true;
            }
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
            if (level.getBlockState(pos).is(Blocks.NETHER_PORTAL)) {
                return true;
            }
        }
        return false;
    }

    private static void pushOutOfPortal(LocalPlayer player, Level level) {
        AABB box = player.getBoundingBox();
        if (!boxTouchesPortal(level, box.inflate(0.001D))) {
            return;
        }

        // Prefer a short free step out of the portal that does not clip solid blocks.
        Direction[] horizontal = {
                Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
        };
        for (double distance : PUSH_DISTANCES) {
            for (Direction direction : horizontal) {
                double dx = direction.getStepX() * distance;
                double dz = direction.getStepZ() * distance;
                if (tryMoveTo(player, level, box, dx, 0.0D, dz, true)) {
                    return;
                }
            }
        }

        // Diagonal short escapes when axis-aligned ones are blocked by frame/obsidian.
        double diag = 0.75D / Math.sqrt(2.0D);
        double[][] diagonals = {
                {diag, diag}, {diag, -diag}, {-diag, diag}, {-diag, -diag}
        };
        for (double[] offset : diagonals) {
            if (tryMoveTo(player, level, box, offset[0], 0.0D, offset[1], true)) {
                return;
            }
        }

        // Last resort: stand on top of the portal column only if that space is free.
        int highestPortalY = highestPortalY(level, box.inflate(0.001D));
        if (highestPortalY == Integer.MIN_VALUE) {
            return;
        }
        double upY = (highestPortalY + 1) - box.minY + 0.01D;
        tryMoveTo(player, level, box, 0.0D, upY, 0.0D, false);
    }

    private static boolean tryMoveTo(
            LocalPlayer player,
            Level level,
            AABB currentBox,
            double dx,
            double dy,
            double dz,
            boolean zeroHorizontalVelocity
    ) {
        AABB target = currentBox.move(dx, dy, dz);
        if (boxTouchesPortal(level, target.inflate(0.001D))) {
            return false;
        }
        if (collidesWithSolid(level, target)) {
            return false;
        }

        player.setPos(player.getX() + dx, player.getY() + dy, player.getZ() + dz);
        Vec3 velocity = player.getDeltaMovement();
        if (zeroHorizontalVelocity) {
            player.setDeltaMovement(0.0D, velocity.y, 0.0D);
        } else {
            player.setDeltaMovement(velocity.x, Math.max(0.0D, velocity.y), velocity.z);
        }
        return true;
    }

    private static boolean boxTouchesPortal(Level level, AABB box) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    if (level.getBlockState(mutable).is(Blocks.NETHER_PORTAL)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean collidesWithSolid(Level level, AABB box) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState(mutable);
                    if (state.isAir() || state.is(Blocks.NETHER_PORTAL)) {
                        continue;
                    }
                    VoxelShape shape = state.getCollisionShape(level, mutable);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    VoxelShape moved = shape.move(x, y, z);
                    if (Shapes.joinIsNotEmpty(moved, Shapes.create(box), BooleanOp.AND)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int highestPortalY(Level level, AABB box) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int highest = Integer.MIN_VALUE;
        int minX = (int) Math.floor(box.minX);
        int minY = (int) Math.floor(box.minY);
        int minZ = (int) Math.floor(box.minZ);
        int maxX = (int) Math.floor(box.maxX);
        int maxY = (int) Math.floor(box.maxY);
        int maxZ = (int) Math.floor(box.maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    mutable.set(x, y, z);
                    if (level.getBlockState(mutable).is(Blocks.NETHER_PORTAL)) {
                        highest = Math.max(highest, y);
                    }
                }
            }
        }
        return highest;
    }
}
