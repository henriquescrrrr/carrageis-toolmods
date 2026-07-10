package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Cripple (Spear) — applies Slowness to hit targets.
 * <p>
 * Jab: Slowness I for 3 seconds.
 * Charge: Slowness II for 4 seconds.
 * Chain/shackle particles on target's feet.
 */
public class CrippleListener implements Listener {

    private final ToolMods plugin;

    public CrippleListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.CRIPPLE)) return;

        boolean isCharge = plugin.getSpearChargeTracker().isCharging(player);

        int amplifier;
        int durationTicks;

        if (isCharge) {
            amplifier = plugin.getConfigManager().getCrippleChargeSlownessLevel();
            durationTicks = plugin.getConfigManager().getCrippleChargeDurationSeconds() * 20;
        } else {
            amplifier = plugin.getConfigManager().getCrippleJabSlownessLevel();
            durationTicks = plugin.getConfigManager().getCrippleJabDurationSeconds() * 20;
        }

        // Apply Slowness
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, durationTicks, amplifier, false, true, true
        ));

        // Chain/shackle particles at target's feet
        Location feetLoc = target.getLocation();
        target.getWorld().spawnParticle(Particle.DUST, feetLoc.clone().add(0, 0.2, 0),
                12, 0.3, 0.1, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(80, 80, 80), 1.2f));

        // Sound
        player.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.8f, 0.6f);
    }
}

