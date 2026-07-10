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
 * Aqua Lung (Helmet) — permanent Water Breathing while helmet equipped.
 * Same refresh pattern as Night Owl.
 */
public class AquaLungListener implements Listener {

    public AquaLungListener(ToolMods plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet == null || !ModUtils.isModEnabled(helmet, ModType.AQUA_LUNG)) continue;
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.WATER_BREATHING, 300, 0, false, false, false));
            }
        }, 20L, 200L);
    }
}

