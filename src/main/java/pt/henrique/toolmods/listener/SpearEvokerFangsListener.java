package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Evoker Fangs (Spear) — on charge hit, spawn a line of evoker fangs
 * in the direction of the charge, starting from the impact point.
 * <p>
 * 5 fangs, 3 magic damage each. Cooldown: 8 seconds.
 * Unlike the sword version, this does NOT require consecutive hits —
 * it triggers on every charge hit (subject to cooldown).
 */
public class SpearEvokerFangsListener implements Listener {

    private final ToolMods plugin;

    /** Recursion guard — prevents fang damage from re-triggering this handler. */
    private static final Set<UUID> processing = new HashSet<>();

    public SpearEvokerFangsListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.SPEAR_EVOKER_FANGS)) return;

        // Charge hits only
        if (!plugin.getSpearChargeTracker().isCharging(player)) return;

        UUID playerId = player.getUniqueId();
        if (processing.contains(playerId)) return;

        // Check cooldown via CooldownManager
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.SPEAR_EVOKER_FANGS)) return;

        processing.add(playerId);
        try {
            // Set cooldown
            int cooldownSeconds = plugin.getConfigManager().getSpearEvokerFangsCooldownSeconds();
            plugin.getCooldownManager().setCooldown(playerId, ModType.SPEAR_EVOKER_FANGS, cooldownSeconds * 1000L);

            // Spawn fangs in a line from the target in the charge direction
            spawnFangLine(player, target);
        } finally {
            processing.remove(playerId);
        }
    }

    /**
     * Spawns evoker fangs in a line from the player toward (and past) the target.
     */
    private void spawnFangLine(Player player, LivingEntity target) {
        int fangCount = plugin.getConfigManager().getSpearEvokerFangsFangCount();

        Location playerLoc = player.getLocation();
        Location targetLoc = target.getLocation();

        // Direction from player to target
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector());
        double totalDistance = direction.length();
        if (totalDistance < 0.5) {
            direction = player.getLocation().getDirection();
            totalDistance = 3.0;
        }
        direction.normalize();

        // Start fangs from the impact point, extending outward
        double startDistance = Math.max(1.0, totalDistance - 1.0);
        double spacing = 1.0;

        for (int i = 0; i < fangCount; i++) {
            double dist = startDistance + (i * spacing);
            Location fangLoc = playerLoc.clone().add(direction.clone().multiply(dist));
            // Snap to ground level
            fangLoc.setY(fangLoc.getWorld().getHighestBlockYAt(fangLoc));

            final Location spawnLoc = fangLoc;
            final int delay = i * 3; // 3 ticks apart

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                spawnLoc.getWorld().spawn(spawnLoc, EvokerFangs.class, entity ->
                        entity.setOwner(player)
                );
                spawnLoc.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                        spawnLoc.clone().add(0, 0.5, 0),
                        5, 0.2, 0.3, 0.2, 0.01);
            }, delay);
        }

        // Sound effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.2f, 0.8f);
    }
}

