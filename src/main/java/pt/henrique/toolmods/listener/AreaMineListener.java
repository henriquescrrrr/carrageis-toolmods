package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.List;

/**
 * Listener for Area Mine 3×3 / 5×5 and single-block pickaxe mod processing.
 * <p>
 * Handles:
 * <ul>
 *   <li>Area Mine activation (NOT sneaking) — breaks a 3×3 or 5×5 plane</li>
 *   <li>Single-block Auto Smelt + Telepathy (when sneaking or no Area Mine)</li>
 * </ul>
 * <p>
 * Runs at NORMAL priority so VeinMinerListener (LOW) can intercept first.
 */
public class AreaMineListener implements Listener {

    private final ToolMods plugin;

    /** Max blocks to process per tick to avoid lag. */
    private static final int BLOCKS_PER_TICK = 10;

    public AreaMineListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Must be a pickaxe
        if (!ToolCategory.PICKAXE.matches(tool)) return;

        // Check for pickaxe mods
        boolean hasAreaMine3 = ModUtils.isModEnabled(tool, ModType.AREA_MINE_3X3);
        boolean hasAreaMine5 = ModUtils.isModEnabled(tool, ModType.AREA_MINE_5X5);
        boolean hasAutoSmelt = ModUtils.isModEnabled(tool, ModType.AUTO_SMELT);
        boolean hasTelepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);

        // No relevant mods → let vanilla handle it
        if (!hasAreaMine3 && !hasAreaMine5 && !hasAutoSmelt && !hasTelepathy) return;

        boolean sneaking = player.isSneaking();
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        Block brokenBlock = event.getBlock();

        // Sneak-to-activate setting: inverts the sneaking behavior for area mods
        boolean requireSneak = PlayerSettingsHook.requiresSneakToActivate(player.getUniqueId());
        boolean shouldActivateArea = requireSneak ? sneaking : !sneaking;

        // ========== Area Mine Mode ==========
        if ((hasAreaMine3 || hasAreaMine5) && shouldActivateArea) {
            event.setCancelled(true);

            // 5×5 takes priority over 3×3
            int radius = hasAreaMine5 ? 2 : 1;

            BlockFace face = BlockBreakHelper.getTargetBlockFace(player);
            List<Block> areaBlocks = BlockBreakHelper.getAreaBlocks(brokenBlock, face, radius);

            // Filter to blocks the pickaxe can harvest + LandClaim check
            List<Block> breakable = areaBlocks.stream()
                    .filter(b -> BlockBreakHelper.canPickaxeHarvest(tool, b))
                    .filter(b -> BlockBreakHelper.canBreak(player, b))
                    .toList();

            if (breakable.isEmpty()) return;

            // Process blocks over multiple ticks
            processBlocksAsync(breakable, tool, player, hasAutoSmelt, hasTelepathy, hasSilkTouch);
            return;
        }

        // ========== Single Block Mode (sneaking or no Area Mine) ==========
        // Apply Auto Smelt and/or Telepathy to the single block
        if (hasAutoSmelt || hasTelepathy) {
            event.setDropItems(false);
            int xp = event.getExpToDrop();
            event.setExpToDrop(0);

            Collection<ItemStack> drops = brokenBlock.getDrops(tool);
            // Block will be broken by Bukkit (event not cancelled).
            // Schedule drop processing for next tick (after block is actually removed).
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Only hand out drops if the block was ACTUALLY broken this tick. If a
                // protection/anti-cheat plugin cancels the BlockBreakEvent at a later
                // priority (e.g. HIGHEST), the block survives — processing drops anyway
                // would duplicate resources (farm items without breaking blocks).
                if (!brokenBlock.getType().isAir()) return;
                BlockBreakHelper.processDrops(brokenBlock, tool, player,
                        drops, xp, hasAutoSmelt, hasTelepathy, hasSilkTouch);
            });
        }
    }

    /**
     * Processes a list of blocks over multiple ticks.
     * Breaks ~BLOCKS_PER_TICK blocks each tick.
     */
    private void processBlocksAsync(List<Block> blocks, ItemStack tool, Player player,
                                     boolean autoSmelt, boolean telepathy, boolean hasSilkTouch) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                for (int i = 0; i < BLOCKS_PER_TICK && index < blocks.size(); i++, index++) {
                    // Check if tool is still alive
                    if (!BlockBreakHelper.isToolAlive(tool)) {
                        cancel();
                        return;
                    }
                    // Check if player is still online
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }

                    Block block = blocks.get(index);

                    // Skip if block was already broken (e.g., by another player)
                    if (block.getType().isAir()) continue;

                    // Re-check LandClaim (block state may have changed)
                    if (!BlockBreakHelper.canBreak(player, block)) continue;

                    // Break the block and collect drops
                    BlockBreakHelper.BlockDropResult result = BlockBreakHelper.breakAndCollect(block, tool);

                    // Process drops (Auto Smelt + Telepathy)
                    BlockBreakHelper.processDrops(block, tool, player,
                            result.drops(), result.xp(),
                            autoSmelt, telepathy, hasSilkTouch);

                    // Deduct durability
                    if (!BlockBreakHelper.deductDurability(tool, player)) {
                        cancel(); // tool broke
                        return;
                    }
                }

                if (index >= blocks.size()) {
                    cancel(); // all blocks processed
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

