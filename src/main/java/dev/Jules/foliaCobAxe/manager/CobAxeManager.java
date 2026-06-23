package dev.Jules.foliaCobAxe.manager;

import dev.Jules.foliaCobAxe.config.ConfigManager;
import dev.Jules.foliaCobAxe.config.ConfigManager.ParticleSpec;
import dev.Jules.foliaCobAxe.config.ConfigManager.Settings;
import dev.Jules.foliaCobAxe.config.ConfigManager.SoundSpec;
import dev.Jules.foliaCobAxe.cooldown.CooldownManager;
import dev.Jules.foliaCobAxe.tracker.HitTracker;
import dev.Jules.foliaCobAxe.util.ItemBuilder;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class CobAxeManager {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final ItemBuilder itemBuilder;
    private final HitTracker hits;
    private final CooldownManager cooldowns;

    public CobAxeManager(JavaPlugin plugin, ConfigManager config, ItemBuilder itemBuilder,
                         HitTracker hits, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.config = config;
        this.itemBuilder = itemBuilder;
        this.hits = hits;
        this.cooldowns = cooldowns;
    }

    public ItemStack createItem() {
        return itemBuilder.create(config.settings());
    }

    public boolean isCobAxe(ItemStack item) {
        return itemBuilder.isCobAxe(item);
    }

    public void give(Player player) {
        player.getInventory().addItem(createItem());
    }

    public void forget(UUID playerId) {
        hits.reset(playerId);
        cooldowns.clear(playerId);
    }

    public void handleHit(Player attacker, LivingEntity victim) {
        Settings s = config.settings();
        UUID id = attacker.getUniqueId();
        int count = hits.increment(id);

        if (count >= s.hitsRequired()) {
            hits.reset(id);
            triggerTrap(victim, s);
            attacker.sendActionBar(config.message("trap-actionbar", attacker));
        } else {
            attacker.sendActionBar(config.message("progress-actionbar", attacker,
                    "{progress}", Integer.toString(count),
                    "{required}", Integer.toString(s.hitsRequired())));
        }
    }

    private void triggerTrap(LivingEntity victim, Settings s) {
        Location loc = victim.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Block block = loc.getBlock();
        if (block.getType().isAir() || block.isReplaceable()) {
            block.setType(s.webMaterial(), false);
            scheduleWebRemoval(loc.clone(), s.webMaterial(), s.webDurationSeconds());
        }

        ParticleSpec p = s.particle();
        if (p.enabled() && p.type() != null) {
            world.spawnParticle(p.type(), loc.getX(), loc.getY() + 1.0, loc.getZ(),
                    p.count(), p.offsetX(), p.offsetY(), p.offsetZ(), p.speed());
        }
        playSound(world, loc, s.trapSound());
    }

    private void scheduleWebRemoval(Location loc, Material web, int durationSeconds) {
        long delayTicks = Math.max(1L, (long) durationSeconds * 20L);
        plugin.getServer().getRegionScheduler().runDelayed(plugin, loc, task -> {
            Block block = loc.getBlock();
            if (block.getType() == web) {
                block.setType(Material.AIR, false);
            }
        }, delayTicks);
    }

    public void handleClear(Player player) {
        UUID id = player.getUniqueId();
        long remaining = cooldowns.remainingMillis(id);
        if (remaining > 0L) {
            sendCooldown(player, remaining);
            return;
        }
        Settings s = config.settings();
        cooldowns.apply(id, s.clearCooldownSeconds());
        clearWebs(player, s);
    }

    private void sendCooldown(Player player, long remainingMillis) {
        double seconds = remainingMillis / 1000.0;
        String precise = String.format(Locale.US, "%.1f", seconds);
        String whole = Long.toString((long) Math.ceil(seconds));
        player.sendActionBar(config.message("cooldown-actionbar", player,
                "{seconds}", whole, "{seconds_precise}", precise));
        if (config.hasMessage("cooldown-chat")) {
            player.sendMessage(config.message("cooldown-chat", player,
                    "{seconds}", whole, "{seconds_precise}", precise));
        }
    }

    private void clearWebs(Player player, Settings s) {
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        int r = s.clearRadius();
        double cx = center.getX();
        double cy = center.getY();
        double cz = center.getZ();
        double r2 = (double) r * (double) r;

        int minBx = (int) Math.floor(cx - r);
        int maxBx = (int) Math.floor(cx + r);
        int minBz = (int) Math.floor(cz - r);
        int maxBz = (int) Math.floor(cz + r);
        int ymin = Math.max(world.getMinHeight(), (int) Math.floor(cy - r));
        int ymax = Math.min(world.getMaxHeight() - 1, (int) Math.floor(cy + r));

        int minCx = minBx >> 4;
        int maxCx = maxBx >> 4;
        int minCz = minBz >> 4;
        int maxCz = maxBz >> 4;

        AtomicInteger removed = new AtomicInteger();
        AtomicInteger pending = new AtomicInteger((maxCx - minCx + 1) * (maxCz - minCz + 1));
        UUID pid = player.getUniqueId();
        Set<Material> mats = s.clearMaterials();
        RegionScheduler scheduler = plugin.getServer().getRegionScheduler();

        for (int chunkX = minCx; chunkX <= maxCx; chunkX++) {
            for (int chunkZ = minCz; chunkZ <= maxCz; chunkZ++) {
                final int fcx = chunkX;
                final int fcz = chunkZ;
                Location anchor = new Location(world, (chunkX << 4) + 8, cy, (chunkZ << 4) + 8);
                scheduler.execute(plugin, anchor, () -> {
                    int c = clearChunk(world, fcx, fcz, cx, cy, cz, r2, ymin, ymax,
                            minBx, maxBx, minBz, maxBz, mats);
                    removed.addAndGet(c);
                    if (pending.decrementAndGet() == 0) {
                        finalizeClear(pid, removed.get(), s);
                    }
                });
            }
        }
    }

    private int clearChunk(World world, int chunkX, int chunkZ,
                           double cx, double cy, double cz, double r2,
                           int ymin, int ymax, int minBx, int maxBx, int minBz, int maxBz,
                           Set<Material> mats) {
        int xStart = Math.max(chunkX << 4, minBx);
        int xEnd = Math.min((chunkX << 4) + 15, maxBx);
        int zStart = Math.max(chunkZ << 4, minBz);
        int zEnd = Math.min((chunkZ << 4) + 15, maxBz);

        int count = 0;
        for (int x = xStart; x <= xEnd; x++) {
            double dx = x + 0.5 - cx;
            double dx2 = dx * dx;
            for (int z = zStart; z <= zEnd; z++) {
                double dz = z + 0.5 - cz;
                double dxz2 = dx2 + dz * dz;
                if (dxz2 > r2) {
                    continue;
                }
                for (int y = ymin; y <= ymax; y++) {
                    double dy = y + 0.5 - cy;
                    if (dxz2 + dy * dy > r2) {
                        continue;
                    }
                    Block block = world.getBlockAt(x, y, z);
                    if (mats.contains(block.getType())) {
                        block.setType(Material.AIR, false);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void finalizeClear(UUID pid, int removed, Settings s) {
        Player player = plugin.getServer().getPlayer(pid);
        if (player == null || !player.isOnline()) {
            return;
        }
        player.getScheduler().run(plugin, task -> {
            String rad = Integer.toString(s.clearRadius());
            String rem = Integer.toString(removed);
            if (removed > 0) {
                player.sendActionBar(config.message("clear-success-actionbar", player,
                        "{removed}", rem, "{radius}", rad));
                if (config.hasMessage("clear-success-chat")) {
                    player.sendMessage(config.message("clear-success-chat", player,
                            "{removed}", rem, "{radius}", rad));
                }
                playSound(player.getWorld(), player.getLocation(), s.clearSound());
            } else {
                player.sendActionBar(config.message("clear-empty-actionbar", player));
            }
        }, null);
    }

    private void playSound(World world, Location loc, SoundSpec spec) {
        if (spec != null && spec.sound() != null) {
            world.playSound(loc, spec.sound(), spec.volume(), spec.pitch());
        }
    }
}
