package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

/**
 * Anchor (Spear/Trident) — on hit, grounds the target for a configurable duration.
 * <p>
 * Applies extreme Slowness to root the target in place and negative Jump Boost
 * to prevent jumping. Works with both melee jab and thrown trident hits.
 * If the target is a player, an action bar countdown is displayed.
 * Cooldown-based.
 */
public class AnchorListener implements Listener {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final ToolMods plugin;

    public AnchorListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        Player player;
        ItemStack tool;

        // Check melee (Player damager) or thrown (Trident damager)
        if (event.getDamager() instanceof Player p) {
            player = p;
            tool = player.getInventory().getItemInMainHand();
            if (!ToolCategory.SPEAR.matches(tool)) return;
        } else if (event.getDamager() instanceof Trident trident) {
            if (!(trident.getShooter() instanceof Player p)) return;
            player = p;
            tool = trident.getItemStack();
            if (!ToolCategory.SPEAR.matches(tool)) return;
        } else {
            return;
        }

        if (!ModUtils.isModEnabled(tool, ModType.ANCHOR)) return;

        UUID playerId = player.getUniqueId();

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.ANCHOR)) return;

        // Set cooldown
        int cooldownSeconds = plugin.getConfigManager().getAnchorCooldownSeconds();
        plugin.getCooldownManager().setCooldown(playerId, ModType.ANCHOR, cooldownSeconds * 1000L);

        int durationSeconds = plugin.getConfigManager().getAnchorDurationSeconds();
        int slownessLevel = plugin.getConfigManager().getAnchorSlownessLevel();
        int durationTicks = durationSeconds * 20;

        // Apply Slowness (very high level to root)
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, durationTicks, slownessLevel, false, true, true
        ));

        // Apply Jump Boost at amplifier 128 (wraps to -128 in signed byte → prevents jumping)
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST, durationTicks, 128, false, false, false
        ));

        // Anchor particles at target's feet — heavy chain/anvil visual
        Location feetLoc = target.getLocation();
        target.getWorld().spawnParticle(Particle.DUST, feetLoc.clone().add(0, 0.3, 0),
                20, 0.4, 0.1, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(50, 50, 50), 1.5f));
        target.getWorld().spawnParticle(Particle.CRIT, feetLoc.clone().add(0, 0.5, 0),
                10, 0.3, 0.3, 0.3, 0.02);

        // Sounds — anvil impact + chain
        target.getWorld().playSound(feetLoc, Sound.BLOCK_ANVIL_LAND, 0.5f, 0.4f);
        target.getWorld().playSound(feetLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.5f);

        // Action bar countdown for anchored target (if it's a player)
        if (target instanceof Player targetPlayer) {
            new BukkitRunnable() {
                int ticksRemaining = durationTicks;

                @Override
                public void run() {
                    if (ticksRemaining <= 0 || target.isDead() || !target.isValid()) {
                        cancel();
                        return;
                    }

                    double secondsLeft = ticksRemaining / 20.0;
                    String lang = plugin.getLangManager().getPlayerLanguage(targetPlayer);
                    String raw = plugin.getLangManager().getRaw(lang, "mod.anchor.anchored-actionbar");
                    raw = raw.replace("{time}", String.format("%.1f", secondsLeft));
                    Component msg = MINI.deserialize(raw);
                    targetPlayer.sendActionBar(msg);

                    ticksRemaining -= 2;
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }
    }
}

