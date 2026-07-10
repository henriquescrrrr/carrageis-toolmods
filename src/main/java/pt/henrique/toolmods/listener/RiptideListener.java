package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Riptide (Spear/Trident) — right-click to launch yourself forward like Riptide,
 * without needing water.
 * <p>
 * Costs 1 water bucket per use. Free during rain.
 * Grants fall damage immunity for a configurable duration after launch.
 * Does not activate when the item has the vanilla Riptide enchantment.
 */
public class RiptideListener implements Listener {

    private final ToolMods plugin;

    /** Tracks players with active fall damage immunity (UUID → immunity expiry ms). */
    private final Map<UUID, Long> fallImmunity = new ConcurrentHashMap<>();

    public RiptideListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.RIPTIDE)) return;

        // Don't activate if item has vanilla Riptide enchantment
        if (tool.containsEnchantment(Enchantment.RIPTIDE)) return;

        UUID playerId = player.getUniqueId();

        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(playerId, ModType.RIPTIDE)) return;

        // Check for water bucket (free in rain)
        boolean freeInRain = plugin.getConfigManager().isRiptideFreeInRain();
        boolean isRaining = player.getWorld().hasStorm()
                && player.getLocation().getBlock().getTemperature() > 0.15
                && player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY();
        boolean needsBucket = !(freeInRain && isRaining);

        if (needsBucket) {
            int bucketSlot = findWaterBucket(player);
            if (bucketSlot == -1) {
                String lang = plugin.getLangManager().getPlayerLanguage(player);
                String raw = plugin.getLangManager().getRaw(lang, "mod.riptide.no-water-bucket");
                Component msg = MiniMessage.miniMessage().deserialize(raw);
                player.sendMessage(msg);
                return;
            }
            // Consume the water bucket → replace with empty bucket
            player.getInventory().setItem(bucketSlot, new ItemStack(Material.BUCKET));
        }

        // Cancel the event to prevent normal trident charge/throw
        event.setCancelled(true);

        // Set cooldown
        int cooldownSeconds = plugin.getConfigManager().getRiptideCooldownSeconds();
        plugin.getCooldownManager().setCooldown(playerId, ModType.RIPTIDE, cooldownSeconds * 1000L);

        // Launch the player in facing direction
        double launchPower = plugin.getConfigManager().getRiptideLaunchPower();
        Vector direction = player.getLocation().getDirection().normalize().multiply(launchPower);
        // Slight upward boost to help clear obstacles
        direction.setY(direction.getY() + 0.3);
        player.setVelocity(direction);

        // Grant fall damage immunity
        int immunitySeconds = plugin.getConfigManager().getRiptideFallImmunitySeconds();
        fallImmunity.put(playerId, System.currentTimeMillis() + (immunitySeconds * 1000L));

        // Particles and sound
        Location loc = player.getLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
        loc.getWorld().spawnParticle(Particle.SPLASH, loc, 30, 0.5, 0.3, 0.5, 0.1);
        loc.getWorld().spawnParticle(Particle.BUBBLE_POP, loc, 15, 0.3, 0.3, 0.3, 0.05);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        UUID playerId = player.getUniqueId();
        Long immuneUntil = fallImmunity.get(playerId);
        if (immuneUntil == null) return;

        if (System.currentTimeMillis() < immuneUntil) {
            event.setCancelled(true);
            // Cloud poof particles on safe landing
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(),
                    10, 0.3, 0.1, 0.3, 0.05);
        }
        // Always clean up after landing
        fallImmunity.remove(playerId);
    }

    private int findWaterBucket(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.WATER_BUCKET) {
                return i;
            }
        }
        return -1;
    }
}

