package dev.Jules.foliaCobAxe.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static Component of(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }
}
