package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Night Owl (Helmet) — permanent Night Vision while helmet equipped.
 * Repeating task refreshes the effect every 200 ticks (10s) with a 300-tick duration
 * so it never flickers. No potion particles.
 */
public class NightOwlListener implements Listener {

    public NightOwlListener(ToolMods plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet == null || !ModUtils.isModEnabled(helmet, ModType.NIGHT_OWL)) continue;
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 300, 0, false, false, false));
            }
        }, 20L, 200L);
    }
}

