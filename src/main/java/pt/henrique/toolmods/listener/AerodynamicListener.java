package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * Aerodynamic — while gliding, 30% less speed decay.
 * <p>
 * Repeating task every 5 ticks. If a player is gliding with elytra that has this mod,
 * applies a small velocity boost to counteract drag. Caps speed to prevent infinite acceleration.
 */
public class AerodynamicListener {

    private static final double MAX_VELOCITY = 3.0; // ~60 blocks/sec cap

    private final ToolMods plugin;
    private BukkitTask task;

    public AerodynamicListener(ToolMods plugin) {
        this.plugin = plugin;
        start();
    }

    private void start() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            double dragReduction = plugin.getConfigManager().getAerodynamicDragReduction();
            // Convert drag reduction to a small velocity boost factor
            // Applied every 5 ticks: a small additive boost to counteract ~30% of drag
            double boostFactor = 1.0 + (dragReduction * 0.1); // e.g., 1.03 for 0.30

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.isGliding()) continue;

                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate == null || chestplate.getType() != Material.ELYTRA) continue;
                if (!ModUtils.isModEnabled(chestplate, ModType.AERODYNAMIC)) continue;

                Vector velocity = player.getVelocity();
                double speed = velocity.length();

                // Only boost if there's meaningful movement and below cap
                if (speed < 0.1 || speed > MAX_VELOCITY) continue;

                // Apply small drag compensation
                Vector boosted = velocity.multiply(boostFactor);

                // Safety cap
                if (boosted.length() > MAX_VELOCITY) {
                    boosted = boosted.normalize().multiply(MAX_VELOCITY);
                }

                player.setVelocity(boosted);
            }
        }, 5L, 5L); // Every 5 ticks
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

