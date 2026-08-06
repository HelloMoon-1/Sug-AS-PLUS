package com.sug.survival.assistant.plus.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sug.survival.assistant.plus.config.Configs;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZhouLi {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZhouLi");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5L))
            .build();
    private static final String SYSTEM_PROMPT = loadSystemPrompt();
    private static final Random RANDOM = new Random();
    private static final String[] TONES = new String[] {
            "本次使用「温言相劝」的辞气：先体谅，再举例劝说；不盛气凌人。",
            "本次使用「大儒辩经」的辞气：建立貌似严谨的论证，加入反例或反问；适合争辩、吐槽与评论。",
            "本次使用「强行圆场」的辞气：为某种行为另立名分，找出勉强成立的礼法解释，最后判作近于君子。",
            "本次使用「痛心疾首」的辞气：把寻常小事提升到秩序与礼法的高度；郑重但不辱骂。"
    };

    private ZhouLi() {
    }

    private static String loadSystemPrompt() {
        try (InputStream in = ZhouLi.class.getClassLoader()
                .getResourceAsStream("assets/sug_survival_assistant_plus/zhouli_skill.md")) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            LOGGER.warn("zhouli_skill.md not found in classpath");
        } catch (IOException e) {
            LOGGER.error("Failed to load zhouli_skill.md", e);
        }
        return "";
    }

    public static boolean isActive() {
        return Configs.ZHOU_LI.getBooleanValue();
    }

    public static boolean shouldSkip(String message) {
        return message == null || message.isEmpty()
                || message.startsWith("/")
                || message.startsWith("!!")
                || message.startsWith("!");
    }

    private static JsonObject buildPayload(String original) {
        String model = Configs.ZHOU_LI_MODEL.getStringValue();
        JsonObject body = new JsonObject();
        body.addProperty("model", model);

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        String tone = TONES[RANDOM.nextInt(TONES.length)];
        systemMsg.addProperty(
                "content",
                SYSTEM_PROMPT + "\n\n" + tone + "\n\n只输出改写结果，不要解释，不要加引号，不要输出多条。"
        );
        messages.add(systemMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", original);
        messages.add(userMsg);

        body.add("messages", messages);
        body.addProperty("temperature", 0.8);
        return body;
    }

    public static CompletableFuture<String> rewriteAsync(String original) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String base = Configs.ZHOU_LI_API_BASE.getStringValue();
                if (base == null || base.isBlank()) {
                    return original;
                }
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                String apiKey = Configs.ZHOU_LI_API_KEY.getStringValue();
                JsonObject payload = buildPayload(original);
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(base + "/chat/completions"))
                        .timeout(Duration.ofSeconds(20L))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
                if (apiKey != null && !apiKey.isBlank()) {
                    builder.header("Authorization", "Bearer " + apiKey);
                }
                HttpResponse<String> response = HTTP_CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    LOGGER.warn("ZhouLi API status {}: {}", response.statusCode(), response.body());
                    return original;
                }
                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray choices = root.getAsJsonArray("choices");
                if (choices == null || choices.isEmpty()) {
                    return original;
                }
                JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
                if (message == null || !message.has("content")) {
                    return original;
                }
                String content = message.get("content").getAsString();
                if (content == null || content.isBlank()) {
                    return original;
                }
                return content.trim();
            } catch (Exception e) {
                LOGGER.warn("ZhouLi rewrite failed", e);
                return original;
            }
        });
    }
}
