package dev.Jules.foliaCobAxe.config;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

final class PlaceholderHook {

    private PlaceholderHook() {
    }

    static String apply(Player player, String input) {
        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
