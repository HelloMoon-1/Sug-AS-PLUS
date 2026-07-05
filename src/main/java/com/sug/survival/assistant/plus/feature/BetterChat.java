package com.sug.survival.assistant.plus.feature;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import com.sug.survival.assistant.plus.config.Configs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class BetterChat {
    private static final Map<String, FoldedMessage> FOLDED_MESSAGES = new HashMap<>();

    private BetterChat() {
    }

    public static boolean handleMessage(List<GuiMessage> messages, Refresher refresher, Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag indicator, int ticks) {
        if (!Configs.BETTER_CHAT.getBooleanValue()) return false;

        String content = message.getString();
        if (isRegexFolded(content)) return true;

        FoldedMessage folded = FOLDED_MESSAGES.get(content);
        if (folded == null) {
            FOLDED_MESSAGES.put(content, new FoldedMessage(message, 1));
            return false;
        }

        folded.count++;
        removeExisting(messages, content, folded.count - 1);
        messages.add(0, new GuiMessage(ticks, Component.literal(content + "(" + folded.count + ")"), signature, source, indicator));
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
        for (String regex : Configs.CHAT_FOLD_REGEX.getStrings()) {
            if (regex == null || regex.isBlank()) continue;
            try {
                if (Pattern.compile(regex).matcher(content).find()) return true;
            } catch (PatternSyntaxException ignored) {
            }
        }
        return false;
    }

    public interface Refresher {
        void refresh();
    }

    private static final class FoldedMessage {
        private final Component message;
        private int count;

        private FoldedMessage(Component message, int count) {
            this.message = message;
            this.count = count;
        }
    }
}
