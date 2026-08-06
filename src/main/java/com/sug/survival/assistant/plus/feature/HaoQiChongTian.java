package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class HaoQiChongTian {
    private static final int FREEZE_TICKS = 100;
    private static final int COOLDOWN_TICKS = 100;
    private static final int SEGMENT_MILLIS = 5000;
    private static final Random RANDOM = new Random();
    private static final AtomicInteger PLAYBACK_TOKEN = new AtomicInteger();
    private static final Object AUDIO_LOCK = new Object();
    private static volatile CachedAudio cachedAudio;
    private static volatile boolean audioLoadFailed;
    private static boolean frozen;
    private static int freezeTicks;
    private static int cooldownTicks;
    private static Vec3 frozenPos = Vec3.ZERO;
    private static Vec3 previousCameraPos = Vec3.ZERO;
    private static Vec3 cameraPos = Vec3.ZERO;
    private static float cameraYaw;
    private static float previousCameraYaw;
    private static float cameraPitch = 15.0F;
    private static CameraType savedCameraType;

    private HaoQiChongTian() {
    }

    public static void tick(Minecraft client) {
        if (!Configs.HAO_QI_CHONG_TIAN.getBooleanValue() || client.player == null || client.level == null) {
            deactivate(client);
            return;
        }
        if (frozen) {
            updateFrozen(client);
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }
        if (!client.player.onGround()) {
            activate(client);
        }
    }

    private static void activate(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        frozen = true;
        freezeTicks = FREEZE_TICKS;
        frozenPos = player.position();
        cameraYaw = player.getYRot();
        previousCameraYaw = cameraYaw;
        updateCameraPosition(player, 0.0F);
        previousCameraPos = cameraPos;
        savedCameraType = client.options.getCameraType();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        playRandomSegment();
        freezePlayer(player);
    }

    private static void updateFrozen(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            deactivate(client);
            return;
        }
        if (!Configs.HAO_QI_CHONG_TIAN.getBooleanValue()) {
            deactivate(client);
            return;
        }
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        freezePlayer(player);
        previousCameraYaw = cameraYaw;
        previousCameraPos = cameraPos;
        cameraYaw += 6.0F;
        updateCameraPosition(player, 4.0F);
        freezeTicks--;
        if (freezeTicks <= 0) {
            finishFreeze(client);
        }
    }

    private static void finishFreeze(Minecraft client) {
        frozen = false;
        cooldownTicks = COOLDOWN_TICKS;
        PLAYBACK_TOKEN.incrementAndGet();
        restoreCameraType(client);
    }

    private static void deactivate(Minecraft client) {
        if (!frozen && cooldownTicks == 0 && savedCameraType == null) {
            return;
        }
        frozen = false;
        freezeTicks = 0;
        cooldownTicks = 0;
        PLAYBACK_TOKEN.incrementAndGet();
        restoreCameraType(client);
    }

    private static void restoreCameraType(Minecraft client) {
        if (savedCameraType != null) {
            client.options.setCameraType(savedCameraType);
        }
        savedCameraType = null;
    }

    private static void freezePlayer(LocalPlayer player) {
        player.setPos(frozenPos.x, frozenPos.y, frozenPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.setOnGround(false);
    }

    private static void updateCameraPosition(LocalPlayer player, float yawStep) {
        float yaw = cameraYaw + yawStep;
        Vec3 focus = frozenPos.add(0.0D, player.getEyeHeight(player.getPose()) * 0.7D, 0.0D);
        Vec3 direction = Vec3.directionFromRotation(cameraPitch, yaw);
        cameraPos = focus.subtract(direction.scale(4.0D));
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static boolean shouldCancelLook(LocalPlayer player) {
        return frozen && player == Minecraft.getInstance().player;
    }

    public static double getCameraX(float tickDelta) {
        return Mth.lerp(tickDelta, previousCameraPos.x, cameraPos.x);
    }

    public static double getCameraY(float tickDelta) {
        return Mth.lerp(tickDelta, previousCameraPos.y, cameraPos.y);
    }

    public static double getCameraZ(float tickDelta) {
        return Mth.lerp(tickDelta, previousCameraPos.z, cameraPos.z);
    }

    public static float getCameraYaw(float tickDelta) {
        return Mth.lerp(tickDelta, previousCameraYaw, cameraYaw);
    }

    public static float getCameraPitch() {
        return cameraPitch;
    }

    private static void playRandomSegment() {
        int token = PLAYBACK_TOKEN.incrementAndGet();
        Thread thread = new Thread(() -> playRandomSegment(token), "SUG-HaoQiChongTian-JH");
        thread.setDaemon(true);
        thread.start();
    }

    private static void playRandomSegment(int token) {
        try {
            CachedAudio cached = loadAudio();
            if (cached == null) {
                return;
            }
            byte[] audio = cached.bytes();
            AudioFormat pcmFormat = cached.format();
            int frameSize = Math.max(1, pcmFormat.getFrameSize());
            float speed = 0.9F + RANDOM.nextFloat() * 0.6F;
            int sourceSegmentBytes = Math.min(
                    audio.length,
                    (int) (pcmFormat.getFrameRate() * SEGMENT_MILLIS / 1000.0F * speed) * frameSize
            );
            int maxStart = Math.max(0, audio.length - sourceSegmentBytes);
            int start = maxStart == 0 ? 0 : RANDOM.nextInt(maxStart / frameSize + 1) * frameSize;
            AudioFormat playFormat = new AudioFormat(
                    pcmFormat.getEncoding(),
                    pcmFormat.getSampleRate() * speed,
                    pcmFormat.getSampleSizeInBits(),
                    pcmFormat.getChannels(),
                    pcmFormat.getFrameSize(),
                    pcmFormat.getFrameRate() * speed,
                    pcmFormat.isBigEndian()
            );
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFormat);
            try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                line.open(playFormat);
                line.start();
                int end = Math.min(audio.length, start + sourceSegmentBytes);
                for (int offset = start; offset < end && PLAYBACK_TOKEN.get() == token; offset += 4096) {
                    int length = Math.min(4096, end - offset);
                    line.write(audio, offset, length);
                }
                if (PLAYBACK_TOKEN.get() == token) {
                    line.drain();
                }
                line.stop();
            }
        } catch (Exception ignored) {
        }
    }

    private static CachedAudio loadAudio() {
        CachedAudio cached = cachedAudio;
        if (cached != null || audioLoadFailed) {
            return cached;
        }
        synchronized (AUDIO_LOCK) {
            if (cachedAudio != null || audioLoadFailed) {
                return cachedAudio;
            }
            try (InputStream stream = openJHSound()) {
                if (stream == null) {
                    audioLoadFailed = true;
                    return null;
                }
                byte[] resourceBytes = readAllBytes(stream);
                try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(resourceBytes))) {
                    AudioFormat baseFormat = source.getFormat();
                    AudioFormat pcmFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * 2,
                            baseFormat.getSampleRate(),
                            false
                    );
                    try (AudioInputStream pcm = AudioSystem.getAudioInputStream(pcmFormat, source)) {
                        byte[] audio = readAllBytes(pcm);
                        if (audio.length == 0) {
                            audioLoadFailed = true;
                            return null;
                        }
                        cachedAudio = new CachedAudio(audio, pcmFormat);
                        return cachedAudio;
                    }
                }
            } catch (Exception e) {
                audioLoadFailed = true;
                return null;
            }
        }
    }

    private static InputStream openJHSound() {
        InputStream stream = HaoQiChongTian.class.getResourceAsStream("/assets/sug_survival_assistant_plus/sounds/jh.wav");
        if (stream != null) {
            return stream;
        }
        return HaoQiChongTian.class.getResourceAsStream("/JH.wav");
    }

    private static byte[] readAllBytes(InputStream stream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private record CachedAudio(byte[] bytes, AudioFormat format) {
    }
}
