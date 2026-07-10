package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listener for Vein Miner — mines all connected ore blocks via BFS.
 * <p>
 * Activation: player breaks an ORE block while SNEAKING with a pickaxe that has Vein Miner.
 * <p>
 * Runs at LOW priority so it intercepts before AreaMineListener (NORMAL).
 * If Vein Miner handles the event, it cancels it so AreaMine doesn't also fire.
 */
public class VeinMinerListener implements Listener {

    private final ToolMods plugin;

    /** Max blocks per tick for the async processing. */
    private static final int BLOCKS_PER_TICK = 10;

    /** All ore block types that Vein Miner can activate on. */
    private static final Set<Material> ORE_BLOCKS = Set.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    public VeinMinerListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // Must be a pickaxe
        if (!ToolCategory.PICKAXE.matches(tool)) return;

        // Must have Vein Miner enabled
        if (!ModUtils.isModEnabled(tool, ModType.VEIN_MINER)) return;

        // Must be sneaking to activate Vein Miner (default behavior)
        // When sneak-to-activate is enabled, sneaking also activates (same behavior)
        boolean requireSneak = PlayerSettingsHook.requiresSneakToActivate(player.getUniqueId());
        // Vein Miner always requires sneak regardless of the setting
        if (!player.isSneaking()) return;

        Block brokenBlock = event.getBlock();

        // Must be an ore block
        if (!isOre(brokenBlock.getType())) return;

        // Cancel the event — we handle everything
        event.setCancelled(true);

        // BFS to find connected ores of the same type
        int maxBlocks = plugin.getConfigManager().getVeinMinerMaxBlocks();
        List<Block> vein = findConnectedOres(brokenBlock, brokenBlock.getType(), maxBlocks);

        // Filter by LandClaim
        List<Block> breakable = vein.stream()
                .filter(b -> BlockBreakHelper.canBreak(player, b))
                .toList();

        if (breakable.isEmpty()) return;

        // Check for other mods on the pickaxe
        boolean hasAutoSmelt = ModUtils.isModEnabled(tool, ModType.AUTO_SMELT);
        boolean hasTelepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);

        // Process blocks over multiple ticks
        processBlocksAsync(breakable, tool, player, hasAutoSmelt, hasTelepathy, hasSilkTouch);
    }

    /**
     * BFS flood-fill from the starting block to find all connected blocks of the same ore type.
     *
     * @param start    the starting block
     * @param oreType  the material type to search for
     * @param maxBlocks maximum number of blocks to find
     * @return list of connected ore blocks (includes the start block)
     */
    private List<Block> findConnectedOres(Block start, Material oreType, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(start);
        visited.add(blockKey(start));

        // 6 cardinal directions
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0},
                {0, 1, 0}, {0, -1, 0},
                {0, 0, 1}, {0, 0, -1}
        };

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            result.add(current);

            for (int[] dir : directions) {
                Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);
                long key = blockKey(neighbor);
                if (!visited.contains(key) && neighbor.getType() == oreType) {
                    visited.add(key);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    /**
     * Creates a unique long key for a block's position (for fast HashSet lookup).
     */
    private static long blockKey(Block block) {
        return ((long) block.getX() & 0x3FFFFFF) << 38
                | ((long) block.getZ() & 0x3FFFFFF) << 12
                | ((long) block.getY() & 0xFFF);
    }

    /**
     * Checks if a material is an ore type that Vein Miner works on.
     */
    public static boolean isOre(Material material) {
        return ORE_BLOCKS.contains(material);
    }

    /**
     * Processes a list of ore blocks over multiple ticks.
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
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }

                    Block block = blocks.get(index);

                    // Skip if block was already broken
                    if (block.getType().isAir()) continue;

                    // Re-check LandClaim
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
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

