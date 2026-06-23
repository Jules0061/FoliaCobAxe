package dev.Jules.foliaCobAxe.command;

import dev.Jules.foliaCobAxe.config.ConfigManager;
import dev.Jules.foliaCobAxe.manager.CobAxeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/** {@code /cobaxe} — gives the Cob Axe to the player who runs it. */
public final class CobAxeCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "foliacobaxe.give";

    private final JavaPlugin plugin;
    private final CobAxeManager manager;
    private final ConfigManager config;

    public CobAxeCommand(JavaPlugin plugin, CobAxeManager manager, ConfigManager config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(config.message("player-only", null));
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(config.message("no-permission", player));
            return true;
        }

        // Inventory mutation must occur on the player's region thread (Folia-safe).
        player.getScheduler().run(plugin, task -> {
            manager.give(player);
            player.sendMessage(config.message("given-item", player));
        }, null);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String @NonNull [] args) {
        return Collections.emptyList();
    }
}
