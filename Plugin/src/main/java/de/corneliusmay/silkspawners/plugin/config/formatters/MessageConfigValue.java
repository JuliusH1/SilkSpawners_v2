package de.corneliusmay.silkspawners.plugin.config.formatters;

import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValueFormatter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageConfigValue implements ConfigValueFormatter<String> {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.legacySection();

    @Override
    public String format(String value) {
        if (isMiniMessage(value)) {
            return LEGACY_SERIALIZER.serialize(MINI_MESSAGE.deserialize(value));
        }
        return value.replaceAll("(?<!\\\\)\\$", "§").replace("\\$", "$");
    }

    private boolean isMiniMessage(String value) {
        int open = value.indexOf('<');
        if (open < 0) return false;
        int close = value.indexOf('>', open + 1);
        return close > open;
    }
}
