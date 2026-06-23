package dev.Jules.foliaCobAxe.util;

import dev.Jules.foliaCobAxe.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemBuilder {

    public static final byte MARKER = (byte) 1;
    public static final String DUPE_KEYWORD = "cobaxe";

    private final NamespacedKey idKey;
    private final NamespacedKey dupeKey;

    public ItemBuilder(NamespacedKey idKey, NamespacedKey dupeKey) {
        this.idKey = idKey;
        this.dupeKey = dupeKey;
    }

    public ItemStack create(ConfigManager.Settings s) {
        ItemStack item = new ItemStack(s.itemMaterial());
        item.editMeta(meta -> applyMeta(meta, s));
        return item;
    }

    private void applyMeta(ItemMeta meta, ConfigManager.Settings s) {
        meta.displayName(Text.of(s.itemName()));

        List<String> loreLines = s.itemLore();
        if (!loreLines.isEmpty()) {
            List<Component> lore = new ArrayList<>(loreLines.size());
            for (String line : loreLines) {
                lore.add(Text.of(line));
            }
            meta.lore(lore);
        }

        meta.setUnbreakable(s.unbreakable());
        if (s.glint()) {
            meta.setEnchantmentGlintOverride(true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(idKey, PersistentDataType.BYTE, MARKER);
        pdc.set(dupeKey, PersistentDataType.STRING, DUPE_KEYWORD);
    }

    public boolean isCobAxe(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        Byte value = pdc.get(idKey, PersistentDataType.BYTE);
        return value != null && value == MARKER;
    }
}
