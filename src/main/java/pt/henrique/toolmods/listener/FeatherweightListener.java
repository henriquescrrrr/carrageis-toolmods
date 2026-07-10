package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Featherweight (Boots) — 50% fall damage reduction permanently.
 * Stacks with Feather Falling (FF reduces first, then Featherweight halves the remainder).
 */
public class FeatherweightListener implements Listener {

    private final ToolMods plugin;

    public FeatherweightListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !ModUtils.isModEnabled(boots, ModType.FEATHERWEIGHT)) return;

        double reduction = plugin.getConfigManager().getFeatherweightReductionPercent();
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }
}

