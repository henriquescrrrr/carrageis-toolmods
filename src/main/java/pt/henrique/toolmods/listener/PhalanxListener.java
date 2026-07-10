package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Phalanx — while blocking, allies within a cone behind the player gain Resistance I.
 * <p>
 * "Behind" = 120° cone opposite the player's facing direction.
 * Effect removed naturally when its short duration expires (no need to explicitly remove).
 * Shield link particles from the blocker to protected allies.
 */
public class PhalanxListener implements Listener {

    private final ToolMods plugin;

    public PhalanxListener(ToolMods plugin) {
        this.plugin = plugin;
        startTask();
    }

    /**
     * Repeating task every 15 ticks (~0.75s). Applies Resistance I for 25 ticks
     * to allies in the cone so it overlaps without flickering.
     */
    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double radius = plugin.getConfigManager().getPhalanxRadius();
            int resistanceLevel = plugin.getConfigManager().getPhalanxResistanceLevel();

            for (Player blocker : Bukkit.getOnlinePlayers()) {
                if (!blocker.isBlocking()) continue;

                ItemStack shield = findShield(blocker);
                if (shield == null) continue;
                if (!ModUtils.isModEnabled(shield, ModType.PHALANX)) continue;

                // "Behind" direction = opposite of facing
                Vector behindDir = blocker.getLocation().getDirection().setY(0).normalize().multiply(-1);
                Location blockerLoc = blocker.getLocation();

                for (Player ally : Bukkit.getOnlinePlayers()) {
                    if (ally.equals(blocker)) continue;
                    if (!ally.getWorld().equals(blocker.getWorld())) continue;

                    double distance = ally.getLocation().distance(blockerLoc);
                    if (distance > radius) continue;

                    // Check 120° cone behind: cos(60°) = 0.5
                    Vector toAlly = ally.getLocation().toVector()
                            .subtract(blockerLoc.toVector()).setY(0);
                    if (toAlly.lengthSquared() < 0.01) continue;
                    toAlly.normalize();

                    double dot = behindDir.dot(toAlly);
                    if (dot < 0.5) continue; // outside 120° cone

                    // Apply Resistance I for 25 ticks (refreshes every 15 ticks)
                    ally.addPotionEffect(new PotionEffect(
                            PotionEffectType.RESISTANCE, 25, resistanceLevel,
                            false, false, true));

                    // Shield link particles: a small line from blocker to ally
                    spawnLinkParticles(blockerLoc, ally.getLocation());
                }
            }
        }, 15L, 15L);
    }

    /**
     * Draws a short particle line from the blocker to the ally.
     */
    private void spawnLinkParticles(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.5) return;
        direction.normalize();

        int points = Math.min(5, (int) (length / 0.8));
        for (int i = 0; i < points; i++) {
            double t = (i + 1.0) / (points + 1.0);
            Location point = from.clone().add(direction.clone().multiply(length * t)).add(0, 1.0, 0);
            from.getWorld().spawnParticle(Particle.DUST, point,
                    1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(100, 160, 255), 0.8f));
        }
    }

    private ItemStack findShield(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) return off;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) return main;
        return null;
    }
}

