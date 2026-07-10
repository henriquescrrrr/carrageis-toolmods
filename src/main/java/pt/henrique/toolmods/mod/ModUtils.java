package pt.henrique.toolmods.mod;

import pt.henrique.toolmods.ToolMods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for reading/writing mods on items via PersistentDataContainer.
 * <p>
 * PDC is the source of truth. Lore is purely visual and rebuilt from PDC data.
 */
public final class ModUtils {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private ModUtils() {
    }

    // ========================
    // PDC Queries
    // ========================

    /**
     * Checks if the item has a specific mod applied.
     */
    public static boolean hasMod(ItemStack item, ModType mod) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(ToolMods.getInstance(), mod.getKey());
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    /**
     * Applies a mod to the item (sets PDC key + rebuilds lore).
     */
    public static void addMod(ItemStack item, ModType mod) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey key = new NamespacedKey(ToolMods.getInstance(), mod.getKey());
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        updateLore(item);
    }

    /**
     * Removes a mod from the item (removes PDC key + disabled toggle + rebuilds lore).
     */
    public static void removeMod(ItemStack item, ModType mod) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(ToolMods.getInstance(), mod.getKey());
        NamespacedKey disabledKey = new NamespacedKey(ToolMods.getInstance(), mod.getDisabledKey());
        pdc.remove(key);
        pdc.remove(disabledKey);
        item.setItemMeta(meta);
        updateLore(item);
    }

    /**
     * Returns all mods currently applied to this item.
     */
    public static List<ModType> getMods(ItemStack item) {
        List<ModType> result = new ArrayList<>();
        if (item == null || !item.hasItemMeta()) return result;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        for (ModType mod : ModType.values()) {
            NamespacedKey key = new NamespacedKey(ToolMods.getInstance(), mod.getKey());
            if (pdc.has(key, PersistentDataType.BYTE)) {
                result.add(mod);
            }
        }
        return result;
    }

    /**
     * Checks if a mod is enabled (not disabled) on the item.
     * A mod is enabled when it exists on the item AND the disabled toggle key is absent.
     */
    public static boolean isModEnabled(ItemStack item, ModType mod) {
        if (!hasMod(item, mod)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey disabledKey = new NamespacedKey(ToolMods.getInstance(), mod.getDisabledKey());
        return !meta.getPersistentDataContainer().has(disabledKey, PersistentDataType.BYTE);
    }

    /**
     * Sets whether a mod is enabled or disabled on the item.
     * When disabled, the mod's disabled toggle key is stored in PDC.
     */
    public static void setModEnabled(ItemStack item, ModType mod, boolean enabled) {
        if (item == null || !hasMod(item, mod)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey disabledKey = new NamespacedKey(ToolMods.getInstance(), mod.getDisabledKey());
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (enabled) {
            pdc.remove(disabledKey);
        } else {
            pdc.set(disabledKey, PersistentDataType.BYTE, (byte) 1);
        }
        item.setItemMeta(meta);
        updateLore(item);
    }

    /**
     * Checks if the item is the correct tool type for the given mod.
     */
    public static boolean isApplicableTool(ItemStack item, ModType mod) {
        if (item == null || item.getType().isAir()) return false;
        return mod.getCategory().matches(item);
    }

    /**
     * Calculates the mod sell bonus for Server Shop integration.
     * Each mod on the item adds (mod price × multiplier) to the sell value.
     */
    public static double calculateModSellBonus(ItemStack item) {
        if (item == null) return 0.0;
        ToolMods plugin = ToolMods.getInstance();
        double multiplier = plugin.getConfigManager().getModSellBonusMultiplier();

        double bonus = 0.0;
        for (ModType mod : getMods(item)) {
            double price = plugin.getConfigManager().getModPrice(mod);
            bonus += price * multiplier;
        }
        return bonus;
    }

    // ========================
    // Lore Management
    // ========================

    /** Marker tag stored in PDC to identify our lore section boundary. */
    private static final String LORE_MARKER = "toolmods_lore_start";

    /**
     * Rebuilds the mod lore section on the item.
     * Preserves any existing non-mod lore lines and appends the mod section below.
     */
    public static void updateLore(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        ToolMods plugin = ToolMods.getInstance();
        List<ModType> mods = getMods(item);

        // Get the existing lore and strip our old mod section
        List<Component> existingLore = meta.lore();
        List<Component> cleanLore = stripModLore(existingLore);

        if (mods.isEmpty()) {
            // No mods — just set the clean lore (or null if empty)
            meta.lore(cleanLore.isEmpty() ? null : cleanLore);
            item.setItemMeta(meta);
            return;
        }

        // Build mod lore section
        List<Component> newLore = new ArrayList<>(cleanLore);

        // Separator
        String separatorRaw = plugin.getLangManager().getRaw("lore.separator");
        newLore.add(MINI.deserialize(separatorRaw));

        // Header
        String headerRaw = plugin.getLangManager().getRaw("lore.header");
        newLore.add(MINI.deserialize(headerRaw));

        // Mod entries
        for (ModType mod : mods) {
            boolean enabled = isModEnabled(item, mod);
            String modName = plugin.getLangManager().getRaw(mod.getNameLangPath());

            String pattern;
            if (enabled) {
                pattern = plugin.getLangManager().getRaw("lore.mod-entry");
            } else {
                pattern = plugin.getLangManager().getRaw("lore.mod-entry-disabled");
            }
            String line = pattern.replace("{mod}", modName);
            newLore.add(MINI.deserialize(line));
        }

        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    /**
     * Strips the mod lore section from existing lore.
     * Detects the section by looking for the separator line.
     */
    private static List<Component> stripModLore(List<Component> lore) {
        if (lore == null || lore.isEmpty()) return new ArrayList<>();

        // Find where our mod section starts — it's the separator line
        // We serialise each line and look for "──────────────" as a signature
        List<Component> clean = new ArrayList<>();
        boolean inModSection = false;

        for (Component line : lore) {
            String plain = MiniMessage.miniMessage().serialize(line);
            // Detect separator by checking for the distinctive dash sequence
            if (!inModSection && plain.contains("──────────────")) {
                inModSection = true;
                continue;
            }
            if (inModSection) {
                continue; // skip all mod-section lines
            }
            clean.add(line);
        }
        return clean;
    }
}

