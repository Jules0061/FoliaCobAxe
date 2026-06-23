package dev.Jules.foliaCobAxe;

import dev.Jules.foliaCobAxe.command.CobAxeCommand;
import dev.Jules.foliaCobAxe.command.FoliaCobAxeCommand;
import dev.Jules.foliaCobAxe.config.ConfigManager;
import dev.Jules.foliaCobAxe.cooldown.CooldownManager;
import dev.Jules.foliaCobAxe.hook.CobAxeExpansion;
import dev.Jules.foliaCobAxe.listener.CobAxeListener;
import dev.Jules.foliaCobAxe.manager.CobAxeManager;
import dev.Jules.foliaCobAxe.tracker.HitTracker;
import dev.Jules.foliaCobAxe.util.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class FoliaCobAxe extends JavaPlugin {

    private HitTracker hitTracker;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.setPapiEnabled(
                getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"));
        configManager.load();

        this.hitTracker = new HitTracker();
        this.cooldownManager = new CooldownManager();

        NamespacedKey idKey = new NamespacedKey(this, "cob_axe");
        NamespacedKey dupeKey = new NamespacedKey(this, ItemBuilder.DUPE_KEYWORD);
        ItemBuilder itemBuilder = new ItemBuilder(idKey, dupeKey);
        CobAxeManager manager = new CobAxeManager(
                this, configManager, itemBuilder, hitTracker, cooldownManager);

        getServer().getPluginManager().registerEvents(new CobAxeListener(manager), this);

        bind("cobaxe", new CobAxeCommand(this, manager, configManager));
        bind("foliacobaxe", new FoliaCobAxeCommand(configManager));

        if (configManager.settings() != null
                && getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CobAxeExpansion(this, configManager, hitTracker, cooldownManager).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }

        getLogger().info("FoliaCobAxe enabled (Folia-native).");
    }

    @Override
    public void onDisable() {
        if (hitTracker != null) {
            hitTracker.clear();
        }
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
    }

    private void bind(String name, Object handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' missing from plugin.yml.");
            return;
        }
        command.setExecutor((org.bukkit.command.CommandExecutor) handler);
        command.setTabCompleter((org.bukkit.command.TabCompleter) handler);
    }
}
