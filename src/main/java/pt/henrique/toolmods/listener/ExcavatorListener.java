package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Listener for Excavator 3×3 / 5×5 — area digging with a shovel.
 * <p>
 * Same logic as Area Mine but only for shovel-appropriate blocks.
 * Sneaking disables the mod (single block mode).
 * Supports Telepathy for direct inventory drops.
 */
public class ExcavatorListener implements Listener {

    private final ToolMods plugin;
    private static final int BLOCKS_PER_TICK = 10;

    public ExcavatorListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolCategory.SHOVEL.matches(tool)) return;

        boolean hasExc3 = ModUtils.isModEnabled(tool, ModType.EXCAVATOR_3X3);
        boolean hasExc5 = ModUtils.isModEnabled(tool, ModType.EXCAVATOR_5X5);
        boolean hasTelepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);

        if (!hasExc3 && !hasExc5 && !hasTelepathy) return;

        Block brokenBlock = event.getBlock();

        // Only excavate shovel-appropriate blocks
        if (!BlockBreakHelper.isShovelBlock(brokenBlock.getType())) return;

        boolean sneaking = player.isSneaking();

        // Sneak-to-activate: inverts sneaking behavior for area mods
        boolean requireSneak = PlayerSettingsHook.requiresSneakToActivate(player.getUniqueId());
        boolean shouldActivateArea = requireSneak ? sneaking : !sneaking;

        // ========== Area Excavate Mode ==========
        if ((hasExc3 || hasExc5) && shouldActivateArea) {
            event.setCancelled(true);

            int radius = hasExc5 ? 2 : 1;
            BlockFace face = BlockBreakHelper.getTargetBlockFace(player);
            List<Block> areaBlocks = BlockBreakHelper.getAreaBlocks(brokenBlock, face, radius);

            List<Block> breakable = areaBlocks.stream()
                    .filter(b -> BlockBreakHelper.isShovelBlock(b.getType()))
                    .filter(b -> BlockBreakHelper.canBreak(player, b))
                    .toList();

            if (breakable.isEmpty()) return;

            processBlocksAsync(breakable, tool, player, hasTelepathy);
            return;
        }

        // ========== Single Block Mode ==========
        if (hasTelepathy) {
            event.setDropItems(false);
            int xp = event.getExpToDrop();
            event.setExpToDrop(0);

            var drops = brokenBlock.getDrops(tool);
            ToolMods inst = plugin;
            inst.getServer().getScheduler().runTask(inst, () -> {
                // Only hand out drops if the block was ACTUALLY broken this tick. If a
                // protection/anti-cheat plugin cancels the BlockBreakEvent at a later
                // priority the block survives — processing drops anyway would duplicate
                // resources (farm Telepathy items without breaking blocks).
                if (!brokenBlock.getType().isAir()) return;
                BlockBreakHelper.processDrops(brokenBlock, tool, player,
                        drops, xp, false, true, false);
            });
        }
    }

    private void processBlocksAsync(List<Block> blocks, ItemStack tool, Player player,
                                     boolean telepathy) {
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                for (int i = 0; i < BLOCKS_PER_TICK && index < blocks.size(); i++, index++) {
                    if (!BlockBreakHelper.isToolAlive(tool)) { cancel(); return; }
                    if (!player.isOnline()) { cancel(); return; }

                    Block block = blocks.get(index);
                    if (block.getType().isAir()) continue;
                    if (!BlockBreakHelper.canBreak(player, block)) continue;

                    BlockBreakHelper.BlockDropResult result = BlockBreakHelper.breakAndCollect(block, tool);
                    BlockBreakHelper.processDrops(block, tool, player,
                            result.drops(), result.xp(), false, telepathy, false);

                    if (!BlockBreakHelper.deductDurability(tool, player)) { cancel(); return; }
                }
                if (index >= blocks.size()) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

