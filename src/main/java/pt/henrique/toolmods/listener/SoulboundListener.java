package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Soulbound — items with this mod are preserved on death and returned on respawn.
 * <p>
 * On death: soulbound items are removed from death drops and stored in memory.
 * On respawn: stored items are given back to the player immediately.
 * On server restart: pending soulbound items are persisted to a file and restored on join.
 */
public class SoulboundListener implements Listener {

    private final ToolMods plugin;

    /** In-memory storage: player UUID → list of soulbound items awaiting respawn. */
    private final Map<UUID, List<ItemStack>> pendingItems = new ConcurrentHashMap<>();

    /** File for persisting soulbound items across server restarts. */
    private File soulboundFile;

    public SoulboundListener(ToolMods plugin) {
        this.plugin = plugin;
        this.soulboundFile = new File(plugin.getDataFolder(), "soulbound-data.yml");
        loadPersisted();
    }

    // ========================
    // Event Handlers
    // ========================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // When keepInventory is active the player retains ALL items (including
        // armor/offhand). Cloning them into pendingItems here would hand back a
        // second copy on respawn — a straight item dupe. Nothing to preserve, so
        // skip Soulbound processing entirely.
        if (event.getKeepInventory()) return;

        List<ItemStack> drops = event.getDrops();
        List<ItemStack> soulbound = new ArrayList<>();

        Iterator<ItemStack> it = drops.iterator();
        while (it.hasNext()) {
            ItemStack item = it.next();
            if (item != null && ModUtils.isModEnabled(item, ModType.SOULBOUND)) {
                soulbound.add(item.clone());
                it.remove();
            }
        }

        // Also check armor slots and offhand (they may not be in drops depending on keepInventory)
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && ModUtils.isModEnabled(armor, ModType.SOULBOUND)) {
                if (!containsExact(soulbound, armor)) {
                    soulbound.add(armor.clone());
                }
            }
        }

        if (!soulbound.isEmpty()) {
            pendingItems.put(player.getUniqueId(), soulbound);
            persistToFile();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<ItemStack> items = pendingItems.remove(uuid);
        if (items == null || items.isEmpty()) return;

        // Give items back on the next tick (after respawn is fully processed)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                // Player disconnected before we could return items — re-queue
                pendingItems.put(uuid, items);
                persistToFile();
                return;
            }
            for (ItemStack item : items) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            persistToFile();
        }, 1L);
    }

    /**
     * On join: restore any persisted soulbound items (from a server restart).
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<ItemStack> items = pendingItems.remove(uuid);
        if (items == null || items.isEmpty()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (ItemStack item : items) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack leftover : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
            persistToFile();
        }, 20L); // slight delay for join
    }

    // ========================
    // Persistence
    // ========================

    /**
     * Saves all pending soulbound items to disk.
     * Called after any change to the pendingItems map.
     */
    public void persistToFile() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<UUID, List<ItemStack>> entry : pendingItems.entrySet()) {
                String key = entry.getKey().toString();
                config.set(key, entry.getValue());
            }
            config.save(soulboundFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save soulbound data", e);
        }
    }

    /**
     * Loads pending soulbound items from disk (for server restart recovery).
     */
    @SuppressWarnings("unchecked")
    private void loadPersisted() {
        if (!soulboundFile.exists()) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(soulboundFile);
            for (String key : config.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                List<?> rawList = config.getList(key);
                if (rawList == null) continue;

                List<ItemStack> items = new ArrayList<>();
                for (Object obj : rawList) {
                    if (obj instanceof ItemStack item) {
                        items.add(item);
                    }
                }
                if (!items.isEmpty()) {
                    pendingItems.put(uuid, items);
                }
            }
            if (!pendingItems.isEmpty()) {
                plugin.getLogger().info("Loaded " + pendingItems.size()
                        + " pending soulbound item set(s) from disk.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load soulbound data", e);
        }
    }

    /**
     * Called on plugin disable to ensure data is persisted.
     */
    public void shutdown() {
        persistToFile();
    }

    private boolean containsExact(List<ItemStack> list, ItemStack target) {
        for (ItemStack item : list) {
            if (item.isSimilar(target)) return true;
        }
        return false;
    }
}

