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
 * Swift (Leggings) — permanent Speed I while equipped.
 * Same refresh pattern as Night Owl. Stacks with beacons/potions
 * (higher amplifier takes precedence naturally).
 */
public class SwiftListener implements Listener {

    public SwiftListener(ToolMods plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack leggings = player.getInventory().getLeggings();
                if (leggings == null || !ModUtils.isModEnabled(leggings, ModType.SWIFT)) continue;
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, 300, 0, false, false, false));
            }
        }, 20L, 200L);
    }
}

