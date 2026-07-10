package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dodger (Leggings) — 20% chance to completely avoid melee or projectile attacks.
 * Internal cooldown: 1 second between dodges. Does NOT dodge environmental damage.
 * Whoosh particles + wind sound on dodge.
 */
public class DodgerListener implements Listener {

    private final ToolMods plugin;
    private final Map<UUID, Long> lastDodge = new ConcurrentHashMap<>();

    public DodgerListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Only dodge melee and projectile
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                && cause != EntityDamageEvent.DamageCause.PROJECTILE) return;

        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings == null || !ModUtils.isModEnabled(leggings, ModType.DODGER)) return;

        // Internal cooldown
        long now = System.currentTimeMillis();
        int internalCooldownMs = plugin.getConfigManager().getDodgerInternalCooldownSeconds() * 1000;
        Long last = lastDodge.get(player.getUniqueId());
        if (last != null && now - last < internalCooldownMs) return;

        // Roll dodge chance
        double chance = plugin.getConfigManager().getDodgerDodgeChance();
        if (Math.random() >= chance) return;

        // Dodge!
        event.setCancelled(true);
        lastDodge.put(player.getUniqueId(), now);

        // Whoosh particles + wind sound
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0),
                8, 0.4, 0.5, 0.4, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 0.8f, 1.5f);
    }
}

