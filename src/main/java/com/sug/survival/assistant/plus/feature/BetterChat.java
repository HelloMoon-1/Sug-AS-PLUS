package com.sug.survival.assistant.plus.feature;

import com.sug.survival.assistant.plus.config.Configs;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class BetterChat {
    private static final int MAX_FOLDED_MESSAGES = 512;
    private static final Map<String, FoldedMessage> FOLDED_MESSAGES = new LinkedHashMap<>(64, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, FoldedMessage> eldest) {
            return size() > MAX_FOLDED_MESSAGES;
        }
    };

    private static List<String> regexSource = List.of();
    private static List<Pattern> regexPatterns = List.of();

    private BetterChat() {
    }

    public static boolean handleMessage(
            List<GuiMessage> messages,
            Refresher refresher,
            Component message,
            MessageSignature signature,
            GuiMessageSource source,
            GuiMessageTag indicator,
            int ticks
    ) {
        if (!Configs.BETTER_CHAT.getBooleanValue()) {
            return false;
        }

        String content = message.getString();
        if (isRegexFolded(content)) {
            return true;
        }

        FoldedMessage folded = FOLDED_MESSAGES.get(content);
        if (folded == null) {
            FOLDED_MESSAGES.put(content, new FoldedMessage(1));
            return false;
        }

        folded.count++;
        removeExisting(messages, content, folded.count - 1);
        messages.add(0, new GuiMessage(
                ticks,
                Component.literal(content + "(" + folded.count + ")"),
                signature,
                source,
                indicator
        ));
        refresher.refresh();
        return true;
    }

    public static void clear() {
        FOLDED_MESSAGES.clear();
    }

    private static void removeExisting(List<GuiMessage> messages, String content, int count) {
        Iterator<GuiMessage> iterator = messages.iterator();
        String folded = content + "(" + count + ")";
        while (iterator.hasNext()) {
            GuiMessage line = iterator.next();
            String text = line.content().getString();
            if (text.equals(content) || text.equals(folded)) {
                iterator.remove();
                return;
            }
        }
    }

    private static boolean isRegexFolded(String content) {
        refreshRegexPatterns();
        for (Pattern pattern : regexPatterns) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    private static void refreshRegexPatterns() {
        List<String> configured = Configs.CHAT_FOLD_REGEX.getStrings();
        List<String> current = configured == null ? List.of() : new ArrayList<>(configured);
        if (current.equals(regexSource)) {
            return;
        }

        List<Pattern> compiled = new ArrayList<>();
        for (String regex : current) {
            if (regex == null || regex.isBlank()) {
                continue;
            }
            try {
                compiled.add(Pattern.compile(regex));
            } catch (PatternSyntaxException ignored) {
            }
        }
        regexSource = current;
        regexPatterns = List.copyOf(compiled);
    }

    public interface Refresher {
        void refresh();
    }

    private static final class FoldedMessage {
        private int count;

        private FoldedMessage(int count) {
            this.count = count;
        }
    }
}
