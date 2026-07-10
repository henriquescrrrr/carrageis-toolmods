package pt.henrique.toolmods.util;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks spear charge state for all players.
 * <p>
 * A "charge" starts when a player right-clicks with a spear, and ends when
 * they switch items, drop, disconnect, or the charge is consumed by a hit.
 * <p>
 * Used by all spear mods to distinguish jab (left-click) from charge (right-click hold).
 */
public class SpearChargeTracker implements Listener {

    /** Per-player charge data. */
    private final Map<UUID, ChargeData> chargeStates = new ConcurrentHashMap<>();

    public SpearChargeTracker(ToolMods plugin) {
        // no-op — registered as listener externally
    }

    // ========================
    // Event Listeners
    // ========================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!ToolCategory.SPEAR.matches(item)) return;

        chargeStates.put(player.getUniqueId(), new ChargeData(
                player.getLocation().clone(),
                System.currentTimeMillis()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSwitch(PlayerItemHeldEvent event) {
        chargeStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        chargeStates.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        chargeStates.remove(event.getPlayer().getUniqueId());
    }

    // ========================
    // Public API
    // ========================

    /**
     * Checks if the player is currently in a spear charge.
     * <p>
     * Primary check: {@code player.isHandRaised()} with a spear in hand.
     * Fallback: tracked charge state (within a 10-second window).
     */
    public boolean isCharging(Player player) {
        // Primary: Bukkit API — player is actively using the item
        if (player.isHandRaised()) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (ToolCategory.SPEAR.matches(item)) return true;
        }

        // Fallback: tracker state (covers the moment of impact where hand may no longer be raised)
        ChargeData data = chargeStates.get(player.getUniqueId());
        if (data != null) {
            long elapsed = System.currentTimeMillis() - data.startTime;
            return elapsed < 10_000; // charge can last up to 10 seconds
        }

        return false;
    }

    /**
     * Returns the location where the current charge started, or {@code null}.
     */
    public Location getChargeStartLocation(UUID playerId) {
        ChargeData data = chargeStates.get(playerId);
        return data != null ? data.startLocation : null;
    }

    /**
     * Ends the charge for a player (call after a charge hit is processed).
     */
    public void endCharge(UUID playerId) {
        chargeStates.remove(playerId);
    }

    // ========================
    // Inner Class
    // ========================

    public static class ChargeData {
        public final Location startLocation;
        public final long startTime;

        public ChargeData(Location startLocation, long startTime) {
            this.startLocation = startLocation;
            this.startTime = startTime;
        }
    }
}

