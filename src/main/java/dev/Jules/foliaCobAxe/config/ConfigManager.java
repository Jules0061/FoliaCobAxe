package dev.Jules.foliaCobAxe.config;

import dev.Jules.foliaCobAxe.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private volatile Settings settings;
    private volatile boolean papiEnabled;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Settings settings() {
        return settings;
    }

    public void setPapiEnabled(boolean papiEnabled) {
        this.papiEnabled = papiEnabled;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        saveResourceIfAbsent();

        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration msg = loadYaml();
        this.settings = parse(cfg, msg);
    }

    public Component message(String key, Player context, String... replacements) {
        Settings s = this.settings;
        String raw = s.messages().getOrDefault(key, "");
        if (raw.isEmpty()) {
            return Component.empty();
        }
        raw = raw.replace("{prefix}", s.prefix());
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        if (papiEnabled && context != null) {
            raw = PlaceholderHook.apply(context, raw);
        }
        return Text.of(raw);
    }

    public boolean hasMessage(String key) {
        String raw = settings.messages().get(key);
        return raw != null && !raw.isEmpty();
    }


    private Settings parse(FileConfiguration c, FileConfiguration m) {
        Material itemMaterial = matchMaterial(c.getString("item.material"), Material.NETHERITE_AXE);
        String itemName = c.getString("item.name", "&fCob Axe");
        List<String> lore = c.getStringList("item.lore");
        boolean glint = c.getBoolean("item.glint", true);
        boolean unbreakable = c.getBoolean("item.unbreakable", true);

        int hitsRequired = Math.max(1, c.getInt("ability.hits-required", 10));
        int webDuration = Math.max(1, c.getInt("ability.web-duration-seconds", 3));
        Material webMaterial = matchMaterial(c.getString("ability.web-material"), Material.COBWEB);

        ParticleSpec particle = new ParticleSpec(
                c.getBoolean("ability.particles.enabled", true),
                matchParticle(c.getString("ability.particles.type")),
                Math.max(0, c.getInt("ability.particles.count", 25)),
                c.getDouble("ability.particles.offset-x", 0.4),
                c.getDouble("ability.particles.offset-y", 0.8),
                c.getDouble("ability.particles.offset-z", 0.4),
                c.getDouble("ability.particles.speed", 0.05)
        );

        int clearRadius = Math.max(1, c.getInt("clear.radius", 5));
        int clearCooldown = Math.max(0, c.getInt("clear.cooldown-seconds", 35));
        Set<Material> clearMaterials = EnumSet.noneOf(Material.class);
        for (String raw : c.getStringList("clear.materials")) {
            Material mat = matchMaterial(raw, null);
            if (mat != null) {
                clearMaterials.add(mat);
            }
        }
        if (clearMaterials.isEmpty()) {
            clearMaterials.add(Material.COBWEB);
        }

        SoundSpec clearSound = parseSound(c, "sounds.clear");
        SoundSpec trapSound = parseSound(c, "sounds.trap");

        Map<String, String> messages = new ConcurrentHashMap<>();
        for (String key : m.getKeys(true)) {
            if (m.isString(key)) {
                messages.put(key, m.getString(key, ""));
            }
        }
        String prefix = messages.getOrDefault("prefix", "");

        return new Settings(
                itemMaterial, itemName, List.copyOf(lore), glint, unbreakable,
                hitsRequired, webDuration, webMaterial, particle,
                clearRadius, clearCooldown, Set.copyOf(clearMaterials),
                clearSound, trapSound, prefix, Map.copyOf(messages)
        );
    }

    private SoundSpec parseSound(FileConfiguration c, String path) {
        Sound sound = matchSound(c.getString(path + ".name"));
        float volume = (float) c.getDouble(path + ".volume", 1.0);
        float pitch = (float) c.getDouble(path + ".pitch", 1.0);
        return new SoundSpec(sound, volume, pitch);
    }

    private Material matchMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material mat = Material.matchMaterial(name.trim());
        if (mat == null) {
            plugin.getLogger().warning("Unknown material '" + name + "', using " + fallback + ".");
            return fallback;
        }
        return mat;
    }

    private Particle matchParticle(String name) {
        if (name == null || name.isBlank()) {
            return Particle.CRIT;
        }
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown particle '" + name + "', using " + Particle.CRIT + ".");
            return Particle.CRIT;
        }
    }

    private Sound matchSound(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String target = name.trim().toUpperCase(Locale.ROOT);
        for (Sound sound : Registry.SOUNDS) {
            NamespacedKey key = Registry.SOUNDS.getKey(sound);
            if (key == null) {
                continue;
            }
            String enumLike = key.getKey().toUpperCase(Locale.ROOT).replace('.', '_');
            if (enumLike.equals(target) || key.getKey().equalsIgnoreCase(name.trim())) {
                return sound;
            }
        }
        plugin.getLogger().warning("Unknown sound '" + name + "'.");
        return null;
    }


    private FileConfiguration loadYaml() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                yaml.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to read defaults for " + "messages.yml", ex);
        }
        return yaml;
    }

    private void saveResourceIfAbsent() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }


    public record SoundSpec(Sound sound, float volume, float pitch) {
    }

    public record ParticleSpec(boolean enabled, Particle type, int count,
                               double offsetX, double offsetY, double offsetZ, double speed) {
    }

    public record Settings(
            Material itemMaterial,
            String itemName,
            List<String> itemLore,
            boolean glint,
            boolean unbreakable,
            int hitsRequired,
            int webDurationSeconds,
            Material webMaterial,
            ParticleSpec particle,
            int clearRadius,
            int clearCooldownSeconds,
            Set<Material> clearMaterials,
            SoundSpec clearSound,
            SoundSpec trapSound,
            String prefix,
            Map<String, String> messages
    ) {
    }
}
