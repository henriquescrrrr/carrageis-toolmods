package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Momentum (Spear) — on charge hit, bonus damage scales with distance traveled.
 * <p>
 * +10% damage per block traveled during the charge (capped at +100%).
 * Action bar shows the current momentum percentage while charging.
 */
public class MomentumListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ToolMods plugin;

    public MomentumListener(ToolMods plugin) {
        this.plugin = plugin;
        startActionBarTask();
    }

    // ========================
    // Damage Modifier
    // ========================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.MOMENTUM)) return;

        // Charge hits only
        if (!plugin.getSpearChargeTracker().isCharging(player)) return;

        Location startLoc = plugin.getSpearChargeTracker().getChargeStartLocation(player.getUniqueId());
        if (startLoc == null) return;

        // Same world check
        if (!startLoc.getWorld().equals(player.getWorld())) return;

        double distance = startLoc.distance(player.getLocation());
        double percentPerBlock = plugin.getConfigManager().getMomentumPercentPerBlock();
        double maxBonus = plugin.getConfigManager().getMomentumMaxBonusPercent();

        double bonusPercent = Math.min(distance * percentPerBlock, maxBonus);

        if (bonusPercent > 0) {
            double newDamage = event.getDamage() * (1.0 + bonusPercent);
            event.setDamage(newDamage);
        }
    }

    // ========================
    // Action Bar Display (during charge)
    // ========================

    /**
     * Starts a repeating task (every 4 ticks) that shows the momentum multiplier
     * on the action bar for players currently charging with a Momentum spear.
     */
    private void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack tool = player.getInventory().getItemInMainHand();
                if (!ToolCategory.SPEAR.matches(tool)) continue;
                if (!ModUtils.isModEnabled(tool, ModType.MOMENTUM)) continue;
                if (!plugin.getSpearChargeTracker().isCharging(player)) continue;

                Location startLoc = plugin.getSpearChargeTracker()
                        .getChargeStartLocation(player.getUniqueId());
                if (startLoc == null || !startLoc.getWorld().equals(player.getWorld())) continue;

                double distance = startLoc.distance(player.getLocation());
                double percentPerBlock = plugin.getConfigManager().getMomentumPercentPerBlock();
                double maxBonus = plugin.getConfigManager().getMomentumMaxBonusPercent();
                double bonusPercent = Math.min(distance * percentPerBlock, maxBonus);

                int displayPercent = (int) Math.round(bonusPercent * 100);

                String lang = plugin.getLangManager().getPlayerLanguage(player);
                String raw = plugin.getLangManager().getRaw(lang, "momentum-display");
                raw = raw.replace("{percent}", String.valueOf(displayPercent));

                Component message = MINI.deserialize(raw);
                player.sendActionBar(message);
            }
        }, 4L, 4L);
    }
}

