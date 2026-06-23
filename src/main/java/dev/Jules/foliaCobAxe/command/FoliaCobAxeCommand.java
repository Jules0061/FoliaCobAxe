package dev.Jules.foliaCobAxe.command;

import dev.Jules.foliaCobAxe.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public final class FoliaCobAxeCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "foliacobaxe.reload";

    private final ConfigManager config;

    public FoliaCobAxeCommand(ConfigManager config) {
        this.config = config;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NonNull [] args) {
        Player ctx = sender instanceof Player p ? p : null;

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(config.message("usage-foliacobaxe", ctx));
            return true;
        }
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(config.message("no-permission", ctx));
            return true;
        }

        try {
            config.load();
            sender.sendMessage(config.message("reload-success", ctx));
        } catch (RuntimeException ex) {
            sender.sendMessage(config.message("usage-foliacobaxe", ctx));
            java.util.logging.Logger.getLogger("FoliaCobAxe")
                    .log(Level.SEVERE, "Reload failed", ex);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String @NonNull [] args) {
        if (args.length == 1 && sender.hasPermission(PERMISSION)
                && "reload".startsWith(args[0].toLowerCase())) {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}
