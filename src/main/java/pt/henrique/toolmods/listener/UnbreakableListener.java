package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Unbreakable — cancels all durability damage on items with this mod.
 * <p>
 * Listens to {@link PlayerItemDamageEvent} and cancels it if the item
 * has the Unbreakable mod enabled. Simple and absolute — zero durability loss.
 */
public class UnbreakableListener implements Listener {

    private final ToolMods plugin;

    public UnbreakableListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) return;

        if (ModUtils.isModEnabled(item, ModType.UNBREAKABLE)) {
            event.setCancelled(true);
        }
    }
}

