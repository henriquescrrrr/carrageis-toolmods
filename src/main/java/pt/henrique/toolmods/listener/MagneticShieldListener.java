package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * Magnetic Shield — while blocking, arrows within a configurable radius
 * are attracted toward the shield and destroyed.
 * <p>
 * Only affects Arrow and SpectralArrow entities (NOT tridents, fireballs, etc.).
 * Does not attract the player's own arrows.
 * Iron particle trail as arrows curve toward the shield.
 */
public class MagneticShieldListener implements Listener {

    private final ToolMods plugin;

    public MagneticShieldListener(ToolMods plugin) {
        this.plugin = plugin;
        startTask();
    }

    /**
     * Repeating task every 2 ticks: for each blocking player with Magnetic Shield,
     * attract nearby arrows toward the shield and destroy them on contact.
     */
    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double radius = plugin.getConfigManager().getMagneticShieldRadius();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isBlocking()) continue;

                ItemStack shield = findShield(player);
                if (shield == null) continue;
                if (!ModUtils.isModEnabled(shield, ModType.MAGNETIC_SHIELD)) continue;

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    // Only arrows — not tridents, fireballs, etc.
                    if (!(entity instanceof AbstractArrow arrow)) continue;
                    if (entity instanceof Trident) continue;
                    if (arrow.isOnGround() || arrow.isDead()) continue;

                    // Don't attract the player's own arrows
                    if (arrow.getShooter() instanceof Player shooter
                            && shooter.getUniqueId().equals(player.getUniqueId())) continue;

                    double distance = arrow.getLocation().distance(player.getLocation());
                    if (distance > radius) continue;

                    if (distance < 0.8) {
                        // Close enough — absorb the arrow
                        arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(),
                                5, 0.1, 0.1, 0.1, 0.02);
                        arrow.getWorld().playSound(arrow.getLocation(),
                                Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.8f);
                        arrow.remove();
                    } else {
                        // Pull toward the player's shield position (eye level, slightly in front)
                        Vector shieldPos = player.getEyeLocation()
                                .add(player.getLocation().getDirection().multiply(0.3)).toVector();
                        Vector pull = shieldPos.subtract(arrow.getLocation().toVector())
                                .normalize().multiply(0.6);
                        arrow.setVelocity(pull);

                        // Iron particle trail
                        arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(),
                                2, 0.05, 0.05, 0.05, 0);
                    }
                }
            }
        }, 2L, 2L);
    }

    private ItemStack findShield(Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) return off;
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) return main;
        return null;
    }
}

