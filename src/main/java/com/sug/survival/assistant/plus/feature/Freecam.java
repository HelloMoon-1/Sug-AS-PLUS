package com.sug.survival.assistant.plus.feature;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import com.sug.survival.assistant.plus.config.Configs;
import com.sug.survival.assistant.plus.mixin.PlayerEntityAccessor;

import java.util.UUID;

public final class Freecam {
    private static boolean active;
    private static Vec3 pos = Vec3.ZERO;
    private static Vec3 prevPos = Vec3.ZERO;
    private static float yaw;
    private static float pitch;
    private static float lastYaw;
    private static float lastPitch;
    private static CameraType perspective;
    private static RemotePlayer fakePlayer;
    private static int fakePlayerId = -1000000;
    private static boolean transformingEntity;
    private static boolean updatingMovementKeys;
    private static boolean forward;
    private static boolean backward;
    private static boolean right;
    private static boolean left;
    private static boolean up;
    private static boolean down;

    private Freecam() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (active) deactivate(client);
            return;
        }

        if (Configs.FREECAM.getBooleanValue()) {
            boolean wasActive = active;
            if (!active) activate(client);
            if (wasActive && (fakePlayer == null || client.level.getEntity(fakePlayer.getId()) != fakePlayer)) spawnFakePlayer(client);
            syncFakePlayer(client);
            updateMovement(client);
        } else if (active) {
            deactivate(client);
        }
    }

    private static void activate(Minecraft client) {
        active = true;
        perspective = client.options.getCameraType();

        yaw = client.player.getYRot();
        pitch = client.player.getXRot();
        lastYaw = yaw;
        lastPitch = pitch;
        pos = client.player.getEyePosition();
        prevPos = pos;
        captureMovementKeys(client);
        unpressMovementKeys(client);
        stopHorizontalMovement(client.player);

        if (!client.options.getCameraType().isFirstPerson()) {
            if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
                yaw += 180.0F;
                pitch *= -1.0F;
                lastYaw = yaw;
                lastPitch = pitch;
            }
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        spawnFakePlayer(client);
    }

    private static void deactivate(Minecraft client) {
        active = false;
        forward = false;
        backward = false;
        right = false;
        left = false;
        up = false;
        down = false;
        removeFakePlayer(client);
        if (perspective != null) client.options.setCameraType(perspective);
        perspective = null;
    }

    private static void spawnFakePlayer(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;

        removeFakePlayer(client);
        fakePlayer = new FreecamFakePlayer(client.level, new GameProfile(UUID.randomUUID(), player.getGameProfile().name()), player.getUUID());
        fakePlayer.setId(fakePlayerId--);
        syncFakePlayer(client);
        client.level.addEntity(fakePlayer);
    }

    private static void removeFakePlayer(Minecraft client) {
        if (fakePlayer == null) return;
        if (client.level != null) client.level.removeEntity(fakePlayer.getId(), RemovalReason.DISCARDED);
        fakePlayer = null;
    }

    private static void syncFakePlayer(Minecraft client) {
        LocalPlayer player = client.player;
        if (fakePlayer == null || player == null) return;

        fakePlayer.restoreFrom(player);
        fakePlayer.setDeltaMovement(player.getDeltaMovement());
        fakePlayer.setSprinting(player.isSprinting());
        fakePlayer.setSwimming(player.isSwimming());
        fakePlayer.setPose(player.getPose());
        fakePlayer.setHealth(player.getHealth());
        fakePlayer.setInvisible(player.isInvisible());
        fakePlayer.getEntityData().set(PlayerEntityAccessor.sug_survival_assistant_plus$getPlayerModelParts(), player.getEntityData().get(PlayerEntityAccessor.sug_survival_assistant_plus$getPlayerModelParts()));
        syncScale(player, fakePlayer);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fakePlayer.setItemSlot(slot, player.getItemBySlot(slot).copy());
        }

        // 同步挥手动画
        fakePlayer.swinging = player.swinging;
        fakePlayer.swingingArm = player.swingingArm;
        fakePlayer.swingTime = player.swingTime;
        fakePlayer.oAttackAnim = player.oAttackAnim;
        fakePlayer.attackAnim = player.attackAnim;
    }

    private static void syncScale(LocalPlayer player, RemotePlayer fakePlayer) {
        AttributeInstance playerScale = player.getAttribute(Attributes.SCALE);
        AttributeInstance fakeScale = fakePlayer.getAttribute(Attributes.SCALE);
        if (playerScale == null || fakeScale == null) return;
        double scale = playerScale.getValue();
        if (fakeScale.getBaseValue() == scale) return;
        fakeScale.setBaseValue(scale);
    }

    private static void updateMovement(Minecraft client) {
        if (perspective != null && !client.options.getCameraType().isFirstPerson()) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }

        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw);
        Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
        double speed = Configs.FREECAM_SPEED.getDoubleValue() * (client.options.keySprint.isDown() ? 1.0D : 0.5D);
        double velX = 0.0D;
        double velY = 0.0D;
        double velZ = 0.0D;
        boolean movingForward = false;
        boolean movingSideways = false;

        if (Freecam.forward) {
            velX += forward.x * speed;
            velZ += forward.z * speed;
            movingForward = true;
        }
        if (backward) {
            velX -= forward.x * speed;
            velZ -= forward.z * speed;
            movingForward = true;
        }
        if (Freecam.right) {
            velX += right.x * speed;
            velZ += right.z * speed;
            movingSideways = true;
        }
        if (left) {
            velX -= right.x * speed;
            velZ -= right.z * speed;
            movingSideways = true;
        }
        if (movingForward && movingSideways) {
            double diagonal = 1.0D / Math.sqrt(2.0D);
            velX *= diagonal;
            velZ *= diagonal;
        }
        if (up) velY += speed;
        if (down) velY -= speed;
        unpressMovementKeys(client);
        stopHorizontalMovement(client.player);

        prevPos = pos;
        pos = pos.add(velX, velY, velZ);
    }

    public static boolean isActive() {
        return active;
    }

    private static void captureMovementKeys(Minecraft client) {
        forward = client.options.keyUp.isDown();
        backward = client.options.keyDown.isDown();
        right = client.options.keyRight.isDown();
        left = client.options.keyLeft.isDown();
        up = client.options.keyJump.isDown();
        down = client.options.keyShift.isDown();
    }

    private static void unpressMovementKeys(Minecraft client) {
        updatingMovementKeys = true;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        updatingMovementKeys = false;
    }

    private static void stopHorizontalMovement(LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(0.0D, velocity.y, 0.0D);
    }

    public static boolean handleMovementKey(KeyMapping keyBinding, boolean pressed) {
        if (!active || updatingMovementKeys) return false;

        Minecraft client = Minecraft.getInstance();
        if (keyBinding == client.options.keyUp) {
            forward = pressed;
        } else if (keyBinding == client.options.keyDown) {
            backward = pressed;
        } else if (keyBinding == client.options.keyRight) {
            right = pressed;
        } else if (keyBinding == client.options.keyLeft) {
            left = pressed;
        } else if (keyBinding == client.options.keyJump) {
            up = pressed;
        } else if (keyBinding == client.options.keyShift) {
            down = pressed;
        } else {
            return false;
        }

        updatingMovementKeys = true;
        keyBinding.setDown(false);
        updatingMovementKeys = false;
        return true;
    }

    public static void changeLookDirection(double deltaX, double deltaY) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) deltaX;
        pitch += (float) deltaY;
        pitch = Mth.clamp(pitch, -90.0F, 90.0F);
    }

    public static double getX(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.x, pos.x);
    }

    public static double getY(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.y, pos.y);
    }

    public static double getZ(float tickDelta) {
        return Mth.lerp(tickDelta, prevPos.z, pos.z);
    }

    public static float getYaw(float tickDelta) {
        return Mth.lerp(tickDelta, lastYaw, yaw);
    }

    public static float getPitch(float tickDelta) {
        return Mth.lerp(tickDelta, lastPitch, pitch);
    }

    public static boolean renderHands() {
        return !active || Configs.FREECAM_RENDER_HANDS.getBooleanValue();
    }

    public static boolean isTransformingEntity() {
        return transformingEntity;
    }

    public static void withCameraTransform(Entity entity, Runnable runnable) {
        if (!active || transformingEntity) {
            runnable.run();
            return;
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        double lastX = entity.xOld;
        double lastY = entity.yOld;
        double lastZ = entity.zOld;
        float originalYaw = entity.getYRot();
        float originalPitch = entity.getXRot();
        float originalLastYaw = entity.yRotO;
        float originalLastPitch = entity.xRotO;

        transformingEntity = true;
        entity.setPos(pos.x, pos.y - entity.getEyeHeight(entity.getPose()), pos.z);
        entity.xOld = prevPos.x;
        entity.yOld = prevPos.y - entity.getEyeHeight(entity.getPose());
        entity.zOld = prevPos.z;
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yRotO = lastYaw;
        entity.xRotO = lastPitch;

        try {
            runnable.run();
        } finally {
            entity.setPos(x, y, z);
            entity.xOld = lastX;
            entity.yOld = lastY;
            entity.zOld = lastZ;
            entity.setYRot(originalYaw);
            entity.setXRot(originalPitch);
            entity.yRotO = originalLastYaw;
            entity.xRotO = originalLastPitch;
            transformingEntity = false;
        }
    }

    public static Entity getFakePlayer() {
        return fakePlayer;
    }

    private static class FreecamFakePlayer extends RemotePlayer {
        private final UUID realPlayerUuid;

        private FreecamFakePlayer(ClientLevel world, GameProfile profile, UUID realPlayerUuid) {
            super(world, profile);
            this.realPlayerUuid = realPlayerUuid;
        }

        @Override
        public boolean isPickable() {
            return false;
        }

        @Override
        public boolean isAttackable() {
            return false;
        }

        @Override
        public boolean isPushable() {
            return false;
        }

        @Override
        public boolean canCollideWith(Entity entity) {
            return false;
        }

        @Override
        public boolean canBeCollidedWith(Entity entity) {
            return false;
        }

        @Override
        public boolean skipAttackInteraction(Entity entity) {
            return true;
        }

        @Override
        protected PlayerInfo getPlayerInfo() {
            Minecraft client = Minecraft.getInstance();
            if (client.getConnection() != null) {
                PlayerInfo entry = client.getConnection().getPlayerInfo(realPlayerUuid);
                if (entry != null) return entry;
            }
            return super.getPlayerInfo();
        }
    }
}
