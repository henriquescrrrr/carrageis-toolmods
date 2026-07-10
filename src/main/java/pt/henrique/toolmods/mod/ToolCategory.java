package pt.henrique.toolmods.mod;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Represents a tool/weapon category that mods can apply to.
 */
public enum ToolCategory {

    PICKAXE(Set.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    )),
    AXE(Set.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    )),
    SHOVEL(Set.of(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
    )),
    HOE(Set.of(
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
    )),
    SWORD(Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    )),
    SPEAR(Set.of(Material.TRIDENT), "_SPEAR"),
    SHIELD(Set.of(Material.SHIELD)),
    MACE(Set.of(Material.MACE)),
    BOW(Set.of(
            Material.BOW, Material.CROSSBOW
    )),
    HELMET(Set.of(
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
            Material.TURTLE_HELMET
    )),
    CHESTPLATE(Set.of(
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE
    )),
    LEGGINGS(Set.of(
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
    )),
    BOOTS(Set.of(
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
    )),
    ELYTRA(Set.of(Material.ELYTRA)),
    UNIVERSAL(Set.of()); // Universal matches any tool — checked separately

    private final Set<Material> materials;

    /**
     * Optional name suffix used for matching when materials cannot be referenced
     * at compile time (e.g. custom server-added items like spears).
     */
    private final String nameSuffix;

    ToolCategory(Set<Material> materials) {
        this(materials, null);
    }

    ToolCategory(Set<Material> materials, String nameSuffix) {
        this.materials = materials;
        this.nameSuffix = nameSuffix;
    }

    public Set<Material> getMaterials() {
        return materials;
    }

    /**
     * Checks if the given item is of this tool category.
     * <p>
     * UNIVERSAL matches any item that belongs to ANY other category.
     * Categories with a {@code nameSuffix} match by material name ending.
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        if (this == UNIVERSAL) {
            // Use matches() so suffix-based categories (SPEAR, etc.) are included
            for (ToolCategory cat : values()) {
                if (cat != UNIVERSAL && cat.matches(item)) {
                    return true;
                }
            }
            return false;
        }

        // Suffix-based matching (e.g. WOODEN_SPEAR, DIAMOND_SPEAR)
        if (nameSuffix != null) {
            if (item.getType().name().endsWith(nameSuffix)) return true;
            // Also check explicit materials (e.g. TRIDENT for SPEAR category)
        }

        return materials.contains(item.getType());
    }

    /**
     * Returns the config key for this category (e.g. "pickaxe", "bow").
     */
    public String configKey() {
        return name().toLowerCase();
    }

    /**
     * Returns the lang key for this category's display name (e.g. "tool.pickaxe").
     */
    public String langKey() {
        return "tool." + configKey();
    }
}

