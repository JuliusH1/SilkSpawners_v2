package de.corneliusmay.silkspawners.plugin.config.formatters;

import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValueFormatter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageConfigValue implements ConfigValueFormatter<String> {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

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

        return formatted
                .replaceAll("(?<!\\\\)[\\$&]", "§")
                .replace("\\$", "$")
                .replace("\\&", "&");
    }

    private boolean isMiniMessage(String value) {
        int open = value.indexOf('<');
        if (open < 0) return false;
        int close = value.indexOf('>', open + 1);
        return close > open;
    }
}
