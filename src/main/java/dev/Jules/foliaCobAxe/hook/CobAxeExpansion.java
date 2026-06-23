package dev.Jules.foliaCobAxe.hook;

import dev.Jules.foliaCobAxe.config.ConfigManager;
import dev.Jules.foliaCobAxe.cooldown.CooldownManager;
import dev.Jules.foliaCobAxe.tracker.HitTracker;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class CobAxeExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final HitTracker hits;
    private final CooldownManager cooldowns;

    public CobAxeExpansion(JavaPlugin plugin, ConfigManager config,
                           HitTracker hits, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.config = config;
        this.hits = hits;
        this.cooldowns = cooldowns;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "foliacobaxe";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Jules";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        ConfigManager.Settings s = config.settings();
        switch (params.toLowerCase(Locale.ROOT)) {
            case "hits_required" -> {
                return Integer.toString(s.hitsRequired());
            }
            case "radius" -> {
                return Integer.toString(s.clearRadius());
            }
            case "progress" -> {
                return player == null ? "0" : Integer.toString(hits.get(player.getUniqueId()));
            }
            case "cooldown" -> {
                if (player == null) {
                    return "0";
                }
                long rem = cooldowns.remainingMillis(player.getUniqueId());
                return Long.toString((long) Math.ceil(rem / 1000.0));
            }
            case "cooldown_precise" -> {
                if (player == null) {
                    return "0.0";
                }
                long rem = cooldowns.remainingMillis(player.getUniqueId());
                return String.format(Locale.US, "%.1f", rem / 1000.0);
            }
            default -> {
                return null;
            }
        }
    }
}
