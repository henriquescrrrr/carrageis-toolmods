package pt.henrique.toolmods.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating GUI ItemStacks.
 */
public class ItemBuilder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    /**
     * Sets display name from a MiniMessage string.
     */
    public ItemBuilder name(String miniMessage) {
        if (meta != null) {
            meta.displayName(MINI.deserialize(miniMessage));
        }
        return this;
    }

    /**
     * Sets display name from a Component.
     */
    public ItemBuilder name(Component name) {
        if (meta != null) {
            meta.displayName(name);
        }
        return this;
    }

    /**
     * Sets lore from MiniMessage strings.
     */
    public ItemBuilder lore(List<String> miniMessageLines) {
        if (meta != null) {
            List<Component> components = new ArrayList<>();
            for (String line : miniMessageLines) {
                components.add(MINI.deserialize(line));
            }
            meta.lore(components);
        }
        return this;
    }

    /**
     * Sets lore from Component list.
     */
    public ItemBuilder loreComponents(List<Component> lore) {
        if (meta != null) {
            meta.lore(lore);
        }
        return this;
    }

    /**
     * Adds a single lore line (MiniMessage).
     */
    public ItemBuilder addLore(String miniMessage) {
        if (meta != null) {
            List<Component> existing = meta.lore();
            List<Component> lore = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
            lore.add(MINI.deserialize(miniMessage));
            meta.lore(lore);
        }
        return this;
    }

    /**
     * Adds a glowing enchantment effect.
     */
    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    /**
     * Hides all item flags (attributes, enchants, etc.).
     */
    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    /**
     * Sets a string value in the item's PDC (for GUI identification).
     */
    public ItemBuilder pdc(NamespacedKey key, String value) {
        if (meta != null) {
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    /**
     * Builds the final ItemStack.
     */
    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}

