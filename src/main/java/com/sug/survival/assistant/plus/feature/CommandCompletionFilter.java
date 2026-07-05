package com.sug.survival.assistant.plus.feature;

import com.mojang.brigadier.suggestion.Suggestion;
import com.sug.survival.assistant.plus.config.Configs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CommandCompletionFilter {
    private CommandCompletionFilter() {
    }

    public static List<Suggestion> filter(String input, int cursor, List<Suggestion> suggestions) {
        if (!Configs.COMMAND_COMPLETION_FILTER.getBooleanValue() || suggestions.isEmpty()) return suggestions;
        String prefix = input.substring(0, Math.min(cursor, input.length()));
        if (!isRootCommand(prefix)) return suggestions;

        List<Suggestion> filtered = new ArrayList<>(suggestions.size());
        for (Suggestion suggestion : suggestions) {
            if (!isBlocked(suggestion.getText())) filtered.add(suggestion);
        }
        return filtered;
    }

    private static boolean isRootCommand(String prefix) {
        if (!prefix.startsWith("/")) return false;
        return prefix.indexOf(' ') == -1;
    }

    private static boolean isBlocked(String suggestion) {
        String command = normalize(suggestion);
        for (String blocked : Configs.COMMAND_COMPLETION_FILTER_LIST.getStrings()) {
            if (command.equals(normalize(blocked))) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        String command = value.trim();
        while (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        if (space != -1) command = command.substring(0, space);
        return command.toLowerCase(Locale.ROOT);
    }
}
