package pt.henrique.toolmods.listener;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Auto Smelt utility — converts raw ore drops into their smelted form.
 * <p>
 * Called by other listeners (Area Mine, Vein Miner) during block processing.
 * Does NOT activate if the tool has Silk Touch.
 */
public final class AutoSmeltListener {

    private AutoSmeltListener() {}

    /** Conversion map: raw material → smelted material. */
    private static final Map<Material, Material> SMELT_MAP = Map.ofEntries(
            Map.entry(Material.RAW_IRON, Material.IRON_INGOT),
            Map.entry(Material.RAW_GOLD, Material.GOLD_INGOT),
            Map.entry(Material.RAW_COPPER, Material.COPPER_INGOT),
            Map.entry(Material.COBBLESTONE, Material.STONE),
            Map.entry(Material.SAND, Material.GLASS),
            Map.entry(Material.RED_SAND, Material.GLASS),
            Map.entry(Material.WET_SPONGE, Material.SPONGE),
            Map.entry(Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP)
    );

    /**
     * Converts a collection of drops into their smelted equivalents.
     * Items not in the conversion map are returned unchanged.
     *
     * @param drops the original drops from a block break
     * @return a new list with smelted items where applicable
     */
    public static List<ItemStack> smelt(Collection<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            Material converted = SMELT_MAP.get(drop.getType());
            if (converted != null) {
                result.add(new ItemStack(converted, drop.getAmount()));
            } else {
                result.add(drop.clone());
            }
        }
        return result;
    }

    /**
     * Checks if a material is smeltable by Auto Smelt.
     */
    public static boolean isSmeltable(Material material) {
        return SMELT_MAP.containsKey(material);
    }
}

