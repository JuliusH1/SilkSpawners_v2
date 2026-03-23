package de.corneliusmay.silkspawners.plugin.config.formatters;

import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValueFormatter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageConfigValue implements ConfigValueFormatter<String> {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    @Override
    public String format(String value) {
        if (value == null) {
            return "";
        }

        String formatted = value;
        if (isMiniMessage(value)) {
            try {
                formatted = LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(value));
            } catch (ParsingException ignored) {
                formatted = value;
            }
        }

        // Convert &#RRGGBB hex format to §x§R§R§G§G§B§B before replacing & with §
        formatted = convertHex(formatted);

        return formatted
                .replaceAll("(?<!\\\\)[\\$&]", "§")
                .replace("\\$", "$")
                .replace("\\&", "&");
    }

    private String convertHex(String value) {
        // Matches &#RRGGBB and converts to §x§R§R§G§G§B§B
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("&#([A-Fa-f0-9]{6})")
                .matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) replacement.append('§').append(c);
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean isMiniMessage(String value) {
        int open = value.indexOf('<');
        if (open < 0) return false;
        int close = value.indexOf('>', open + 1);
        return close > open;
    }
}