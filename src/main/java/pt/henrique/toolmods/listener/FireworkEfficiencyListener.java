package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * Firework Efficiency — firework rockets used during elytra flight last 50% longer.
 * <p>
 * Detects firework spawn near a gliding player with elytra that has this mod,
 * then increases the firework's flight duration.
 */
public class FireworkEfficiencyListener implements Listener {

    private final ToolMods plugin;

    public FireworkEfficiencyListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onFireworkSpawn(EntitySpawnEvent event) {
        if (event.getEntity().getType() != EntityType.FIREWORK_ROCKET) return;
        if (!(event.getEntity() instanceof Firework firework)) return;

        // Check if a nearby gliding player spawned this firework (boost rocket)
        // Fireworks used for elytra boost spawn at the player's location
        for (Player player : firework.getLocation().getWorld().getNearbyPlayers(firework.getLocation(), 1.5)) {
            if (!player.isGliding()) continue;

            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate == null || chestplate.getType() != org.bukkit.Material.ELYTRA) continue;
            if (!ModUtils.isModEnabled(chestplate, ModType.FIREWORK_EFFICIENCY)) continue;

            // Increase firework flight duration
            FireworkMeta meta = firework.getFireworkMeta();
            double multiplier = plugin.getConfigManager().getFireworkEfficiencyMultiplier();
            int currentPower = meta.getPower();
            int newPower = Math.max(1, (int) Math.ceil(currentPower * multiplier));
            meta.setPower(Math.min(newPower, 127)); // cap at max
            firework.setFireworkMeta(meta);

            // Also give a velocity boost proportional to the multiplier
            org.bukkit.util.Vector velocity = player.getVelocity();
            org.bukkit.util.Vector direction = player.getLocation().getDirection();
            double boostFactor = (multiplier - 1.0) * 0.5; // e.g., 0.25 for 1.5x multiplier
            org.bukkit.util.Vector boosted = velocity.add(direction.multiply(boostFactor));
            // Cap resulting speed to prevent runaway acceleration from firework spam
            // (many rockets spawning near a gliding player would otherwise compound).
            double maxSpeed = 4.0; // ~80 blocks/sec
            if (boosted.length() > maxSpeed) {
                boosted = boosted.normalize().multiply(maxSpeed);
            }
            player.setVelocity(boosted);
            break;
        }
    }
}

