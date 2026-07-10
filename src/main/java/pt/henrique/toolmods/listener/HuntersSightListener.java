package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Hunter's Sight (Helmet) — hostile mobs within a configurable radius
 * get a red particle highlight visible only to this player.
 * <p>
 * Uses per-player particle rendering (no global glow) to ensure only
 * the player with this mod sees the highlights. Particles appear above
 * the mob's head every update cycle.
 */
public class HuntersSightListener implements Listener {

    public HuntersSightListener(ToolMods plugin) {
        int intervalTicks = plugin.getConfigManager().getHuntersSightUpdateIntervalTicks();
        double radius = plugin.getConfigManager().getHuntersSightRadius();

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet == null || !ModUtils.isModEnabled(helmet, ModType.HUNTERS_SIGHT)) continue;

                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof Monster mob)) continue;
                    if (mob.isDead()) continue;

                    // Per-player red particle highlight above the mob's head
                    player.spawnParticle(Particle.DUST,
                            mob.getLocation().add(0, mob.getHeight() + 0.4, 0),
                            4, 0.15, 0.1, 0.15, 0,
                            new Particle.DustOptions(Color.RED, 1.0f));
                }
            }
        }, 20L, intervalTicks);
    }
}

