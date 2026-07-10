package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Frost Walker Plus (Boots) — enhanced Frost Walker with 4-block radius
 * and ice that lasts 10 seconds.
 * <p>
 * Freezes water blocks when the player walks nearby. Gives Speed I while
 * on frozen water. Uses FROSTED_ICE and schedules thaw after the configured duration.
 */
public class FrostWalkerPlusListener implements Listener {

    private final ToolMods plugin;

    public FrostWalkerPlusListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Only process on block change (not sub-block movement)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()) return;

        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || !ModUtils.isModEnabled(boots, ModType.FROST_WALKER_PLUS)) return;

        int radius = plugin.getConfigManager().getFrostWalkerPlusRadius();
        int iceDurationTicks = plugin.getConfigManager().getFrostWalkerPlusIceDurationSeconds() * 20;

        Location playerLoc = player.getLocation();
        Block below = playerLoc.getBlock().getRelative(BlockFace.DOWN);

        // Speed boost while on frozen water
        if (below.getType() == Material.FROSTED_ICE) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 40, 0, false, false, false));
        }

        // Freeze water blocks in radius
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue; // circular radius

                Block block = below.getRelative(x, 0, z);
                if (block.getType() != Material.WATER) continue;
                if (!block.getRelative(BlockFace.UP).getType().isAir()) continue;
                // Respect land protection — don't freeze water inside claims the
                // player can't build in (parity with the mining/placement mods).
                if (!BlockBreakHelper.canBreak(player, block)) continue;

                // Only freeze full water source blocks (not flowing)
                if (block.getBlockData() instanceof org.bukkit.block.data.Levelled levelled) {
                    if (levelled.getLevel() != 0) continue;
                }

                block.setType(Material.FROSTED_ICE);

                // Schedule thaw
                final Block frozenBlock = block;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (frozenBlock.getType() == Material.FROSTED_ICE) {
                        frozenBlock.setType(Material.WATER);
                    }
                }, iceDurationTicks);
            }
        }
    }
}

