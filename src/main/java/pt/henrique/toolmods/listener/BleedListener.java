package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bleed (Spear) — applies a damage-over-time bleed effect on hit.
 * <p>
 * 1 heart (2 HP) every 2 seconds for 6 seconds (3 ticks total, 3 hearts).
 * Does NOT stack — a new hit resets the bleed timer.
 * Red particle drip on the target during bleed.
 */
public class BleedListener implements Listener {

    private final ToolMods plugin;

    /** Active bleed tasks per target entity UUID. */
    private final Map<UUID, BukkitTask> bleedTasks = new ConcurrentHashMap<>();

    public BleedListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.BLEED)) return;

        UUID targetId = target.getUniqueId();

        // Cancel existing bleed on this target (reset, don't stack)
        BukkitTask existing = bleedTasks.remove(targetId);
        if (existing != null) existing.cancel();

        // Config values
        double damagePerTick = plugin.getConfigManager().getBleedDamagePerTick();
        int intervalSeconds = plugin.getConfigManager().getBleedTickIntervalSeconds();
        int durationSeconds = plugin.getConfigManager().getBleedDurationSeconds();

        int intervalTicks = intervalSeconds * 20;
        int maxHits = durationSeconds / Math.max(1, intervalSeconds);

        // Start new bleed task
        BukkitTask task = new BukkitRunnable() {
            int hits = 0;

            @Override
            public void run() {
                if (target.isDead() || !target.isValid() || hits >= maxHits) {
                    bleedTasks.remove(targetId);
                    cancel();
                    return;
                }

                // Apply bleed damage (generic damage — not attributed to player to avoid
                // looping with other listeners)
                target.damage(damagePerTick);
                hits++;

                // Red particle drip
                target.getWorld().spawnParticle(Particle.DUST,
                        target.getLocation().add(0, target.getHeight() * 0.5, 0),
                        8, 0.2, 0.4, 0.2, 0,
                        new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.0f));
            }
        }.runTaskTimer(plugin, intervalTicks, intervalTicks);

        bleedTasks.put(targetId, task);
    }
}

