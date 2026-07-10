package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Poseidon's Call (Spear/Trident) — throw your trident into water to create
 * a whirlpool that pulls enemies toward the impact point.
 * <p>
 * Only activates when the trident lands in water. Entities within the pull
 * radius are dragged toward the center for the configured duration.
 * Spinning water particles visualize the whirlpool. Cooldown-based.
 */
public class PoseidonsCallListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ToolMods plugin;

    public PoseidonsCallListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player player)) return;

        ItemStack tridentItem = trident.getItemStack();
        if (!ModUtils.isModEnabled(tridentItem, ModType.POSEIDONS_CALL)) return;

        UUID playerId = player.getUniqueId();

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.POSEIDONS_CALL)) return;

        Location impactLoc = trident.getLocation();

        // Must land in water
        if (impactLoc.getBlock().getType() != Material.WATER) {
            String lang = plugin.getLangManager().getPlayerLanguage(player);
            String raw = plugin.getLangManager().getRaw(lang, "mod.poseidons-call.not-in-water");
            Component msg = MINI.deserialize(raw);
            player.sendMessage(msg);
            return;
        }

        // Set cooldown
        int cooldownSeconds = plugin.getConfigManager().getPoseidonsCallCooldownSeconds();
        plugin.getCooldownManager().setCooldown(playerId, ModType.POSEIDONS_CALL, cooldownSeconds * 1000L);

        double pullRadius = plugin.getConfigManager().getPoseidonsCallPullRadius();
        int durationSeconds = plugin.getConfigManager().getPoseidonsCallPullDurationSeconds();
        double pullStrength = plugin.getConfigManager().getPoseidonsCallPullStrength();
        int totalTicks = durationSeconds * 20;

        // Sounds — trident thunder + underwater ambience
        impactLoc.getWorld().playSound(impactLoc, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 0.5f);
        impactLoc.getWorld().playSound(impactLoc, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 0.8f);

        // Start whirlpool pull task
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= totalTicks) {
                    cancel();
                    return;
                }
                ticksElapsed++;

                // Pull nearby entities toward impact point
                for (Entity entity : impactLoc.getWorld().getNearbyEntities(
                        impactLoc, pullRadius, pullRadius, pullRadius)) {
                    if (!(entity instanceof LivingEntity)) continue;
                    if (entity.getUniqueId().equals(playerId)) continue;
                    if (entity.isDead()) continue;

                    double distance = entity.getLocation().distance(impactLoc);
                    if (distance > pullRadius || distance < 0.5) continue;

                    // Pull strength scales — further entities get pulled harder
                    double strength = pullStrength * (distance / pullRadius);
                    Vector pull = impactLoc.toVector()
                            .subtract(entity.getLocation().toVector()).normalize()
                            .multiply(strength);
                    pull.setY(Math.max(pull.getY(), -0.05)); // don't slam into ground

                    entity.setVelocity(entity.getVelocity().add(pull));
                }

                // Whirlpool particles — spinning water ring
                double phase = ticksElapsed * 0.3;
                double progress = (double) ticksElapsed / totalTicks;
                double ringRadius = pullRadius * 0.5 * (1.0 - progress) + 0.5;

                for (int i = 0; i < 8; i++) {
                    double angle = phase + (2 * Math.PI * i / 8);
                    double x = Math.cos(angle) * ringRadius;
                    double z = Math.sin(angle) * ringRadius;
                    Location particleLoc = impactLoc.clone().add(x, 0.5, z);

                    impactLoc.getWorld().spawnParticle(Particle.BUBBLE_POP, particleLoc,
                            1, 0.1, 0.1, 0.1, 0);
                    impactLoc.getWorld().spawnParticle(Particle.SPLASH, particleLoc,
                            2, 0.15, 0.1, 0.15, 0.02);
                }

                // Center splash column
                impactLoc.getWorld().spawnParticle(Particle.SPLASH,
                        impactLoc.clone().add(0, 0.5, 0),
                        5, 0.3, 0.5, 0.3, 0.05);

                // Aqua dust particles
                impactLoc.getWorld().spawnParticle(Particle.DUST,
                        impactLoc.clone().add(0, 1, 0),
                        3, 1.0, 0.5, 1.0, 0,
                        new Particle.DustOptions(Color.fromRGB(0, 150, 200), 1.5f));
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}

