package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Last Stand (Chestplate) — when health drops below 3 hearts (6 HP),
 * gain Resistance II + Strength I for 5 seconds.
 * <p>
 * Cooldown: 60 seconds (shown on action bar). Heartbeat sound + red particle aura.
 */
public class LastStandListener implements Listener {

    private final ToolMods plugin;

    public LastStandListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || !ModUtils.isModEnabled(chestplate, ModType.LAST_STAND)) return;

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), ModType.LAST_STAND)) return;

        double threshold = plugin.getConfigManager().getLastStandHealthThreshold();
        double healthAfterDamage = player.getHealth() - event.getFinalDamage();

        if (healthAfterDamage > threshold || healthAfterDamage <= 0) return;

        // Activate Last Stand!
        int durationTicks = plugin.getConfigManager().getLastStandDurationSeconds() * 20;
        int cooldownSeconds = plugin.getConfigManager().getLastStandCooldownSeconds();

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, durationTicks, 1, false, true, true)); // Resistance II
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.STRENGTH, durationTicks, 0, false, true, true)); // Strength I

        // Set cooldown
        plugin.getCooldownManager().setCooldown(player.getUniqueId(), ModType.LAST_STAND,
                cooldownSeconds * 1000L);

        // Heartbeat sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.5f);
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.5f, 0.6f), 5L);

        // Red particle aura
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                20, 0.5, 0.8, 0.5, 0,
                new Particle.DustOptions(Color.RED, 1.5f));
    }
}

