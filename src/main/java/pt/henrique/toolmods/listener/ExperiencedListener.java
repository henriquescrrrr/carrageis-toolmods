package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Experienced — multiplies XP gained from tool-related activities.
 * <p>
 * Mining with pickaxe/axe/shovel/hoe: multiplies block-break XP.
 * Killing with sword: multiplies mob death XP.
 * The multiplier is configurable (default 1.5 = +50%).
 * Additive with other XP sources (not multiplicative with other mods).
 */
public class ExperiencedListener implements Listener {

    private final ToolMods plugin;

    public ExperiencedListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    /**
     * Multiplies XP from breaking blocks.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getExpToDrop() <= 0) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ModUtils.isModEnabled(tool, ModType.EXPERIENCED)) return;

        // Only for tools that belong to a category
        if (!isToolOrWeapon(tool)) return;

        double multiplier = plugin.getConfigManager().getExperiencedXpMultiplier();
        int newXp = (int) Math.round(event.getExpToDrop() * multiplier);
        event.setExpToDrop(newXp);
    }

    /**
     * Multiplies XP from killing entities.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getDroppedExp() <= 0) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (!ModUtils.isModEnabled(tool, ModType.EXPERIENCED)) return;

        // Primarily for swords but works with any held tool/weapon
        if (!isToolOrWeapon(tool)) return;

        double multiplier = plugin.getConfigManager().getExperiencedXpMultiplier();
        int newXp = (int) Math.round(event.getDroppedExp() * multiplier);
        event.setDroppedExp(newXp);
    }

    /**
     * Checks if the item belongs to any known tool/weapon category.
     */
    private boolean isToolOrWeapon(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        for (ToolCategory cat : ToolCategory.values()) {
            if (cat.matches(item)) return true;
        }
        return false;
    }
}

