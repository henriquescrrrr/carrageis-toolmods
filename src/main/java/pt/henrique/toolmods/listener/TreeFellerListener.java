package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.AntiXrayHook;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Listener for Tree Feller — chops entire trees, destroys leaves, and replants.
 * <p>
 * Activation: player breaks a LOG block while SNEAKING with an axe that has Tree Feller.
 * <p>
 * Includes:
 * <ul>
 *   <li><b>Tree Feller</b> — BFS through connected log blocks</li>
 *   <li><b>Leaf Blower</b> — destroys leaves within 6 blocks of any log</li>
 *   <li><b>Auto Replant</b> — places a matching sapling at the bottom</li>
 * </ul>
 * Processes ~blocks-per-tick blocks per tick to avoid lag.
 */
public class TreeFellerListener implements Listener {

    private final ToolMods plugin;

    /** Materials considered "log" for tree detection. */
    private static final Set<Material> LOG_MATERIALS = new HashSet<>();

    /** Map: log type → matching sapling type. */
    private static final Map<Material, Material> LOG_TO_SAPLING = new HashMap<>();

    static {
        // All log types (includes stripped, wood, etc. via Tag, but we also add manually)
        LOG_MATERIALS.addAll(Set.of(
                Material.OAK_LOG, Material.BIRCH_LOG, Material.SPRUCE_LOG, Material.JUNGLE_LOG,
                Material.ACACIA_LOG, Material.DARK_OAK_LOG, Material.MANGROVE_LOG, Material.CHERRY_LOG,
                Material.PALE_OAK_LOG,
                Material.OAK_WOOD, Material.BIRCH_WOOD, Material.SPRUCE_WOOD, Material.JUNGLE_WOOD,
                Material.ACACIA_WOOD, Material.DARK_OAK_WOOD, Material.MANGROVE_WOOD, Material.CHERRY_WOOD,
                Material.PALE_OAK_WOOD,
                Material.MUSHROOM_STEM
        ));

        LOG_TO_SAPLING.put(Material.OAK_LOG, Material.OAK_SAPLING);
        LOG_TO_SAPLING.put(Material.BIRCH_LOG, Material.BIRCH_SAPLING);
        LOG_TO_SAPLING.put(Material.SPRUCE_LOG, Material.SPRUCE_SAPLING);
        LOG_TO_SAPLING.put(Material.JUNGLE_LOG, Material.JUNGLE_SAPLING);
        LOG_TO_SAPLING.put(Material.ACACIA_LOG, Material.ACACIA_SAPLING);
        LOG_TO_SAPLING.put(Material.DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
        LOG_TO_SAPLING.put(Material.MANGROVE_LOG, Material.MANGROVE_PROPAGULE);
        LOG_TO_SAPLING.put(Material.CHERRY_LOG, Material.CHERRY_SAPLING);
        LOG_TO_SAPLING.put(Material.PALE_OAK_LOG, Material.PALE_OAK_SAPLING);
    }

    public TreeFellerListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolCategory.AXE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.TREE_FELLER)) return;
        if (!player.isSneaking()) return;

        Block brokenBlock = event.getBlock();
        if (!isLog(brokenBlock.getType())) return;

        // Validate: check for leaves within 6 blocks (confirm it's a tree, not a structure)
        if (!hasNearbyLeaves(brokenBlock, 6)) return;

        event.setCancelled(true);

        int maxBlocks = plugin.getConfigManager().getTreeFellerMaxBlocks();
        int blocksPerTick = plugin.getConfigManager().getTreeFellerBlocksPerTick();

        // BFS to find all connected log blocks
        List<Block> logs = findConnectedLogs(brokenBlock, maxBlocks);

        // Filter by LandClaim
        List<Block> breakable = logs.stream()
                .filter(b -> BlockBreakHelper.canBreak(player, b))
                .toList();

        if (breakable.isEmpty()) return;

        // Find the bottommost log for replanting
        Block bottomLog = breakable.stream()
                .min(Comparator.comparingInt(Block::getY))
                .orElse(brokenBlock);
        Material saplingType = LOG_TO_SAPLING.getOrDefault(brokenBlock.getType(), null);

        // Process logs over multiple ticks, then leaves, then replant
        processTreeFell(breakable, tool, player, blocksPerTick, bottomLog, saplingType);
    }

    /**
     * BFS to find all connected log blocks from the starting block.
     */
    private List<Block> findConnectedLogs(Block start, int maxBlocks) {
        List<Block> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Queue<Block> queue = new LinkedList<>();

        queue.add(start);
        visited.add(blockKey(start));

        // 6 cardinal + 4 diagonal on XZ (trees can be diagonal)
        int[][] directions = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {1, 1, 1}, {-1, 1, 1}, {1, 1, -1}, {-1, 1, -1}
        };

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.poll();
            result.add(current);

            for (int[] dir : directions) {
                Block neighbor = current.getRelative(dir[0], dir[1], dir[2]);
                long key = blockKey(neighbor);
                if (!visited.contains(key) && isLog(neighbor.getType())) {
                    visited.add(key);
                    queue.add(neighbor);
                }
            }
        }
        return result;
    }

    /**
     * Checks if there are leaf blocks within the given radius of the block.
     */
    private boolean hasNearbyLeaves(Block center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block check = center.getRelative(x, y, z);
                    if (isLeaf(check.getType())) return true;
                }
            }
        }
        return false;
    }

    /**
     * Processes the tree fell operation over multiple ticks:
     * 1. Break log blocks
     * 2. Destroy nearby leaves (leaf blower)
     * 3. Replant sapling
     */
    private void processTreeFell(List<Block> logs, ItemStack tool, Player player,
                                  int blocksPerTick, Block bottomLog, Material saplingType) {
        // Collect log positions for leaf blowing later
        Set<Long> logPositions = new HashSet<>();
        for (Block log : logs) {
            logPositions.add(blockKey(log));
        }

        new BukkitRunnable() {
            int phase = 0; // 0 = breaking logs, 1 = destroying leaves, 2 = replant
            int index = 0;
            List<Block> leaves = null;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                if (phase == 0) {
                    // Phase 0: Break logs
                    for (int i = 0; i < blocksPerTick && index < logs.size(); i++, index++) {
                        if (!BlockBreakHelper.isToolAlive(tool)) { cancel(); return; }

                        Block log = logs.get(index);
                        if (log.getType().isAir()) continue;
                        if (!BlockBreakHelper.canBreak(player, log)) continue;

                        BlockBreakHelper.BlockDropResult result = BlockBreakHelper.breakAndCollect(log, tool);
                        // Telepathy check (tree feller doesn't have auto smelt but does use telepathy if present)
                        boolean telepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);
                        BlockBreakHelper.processDrops(log, tool, player,
                                result.drops(), 0, false, telepathy, false);

                        if (!BlockBreakHelper.deductDurability(tool, player)) { cancel(); return; }
                    }

                    if (index >= logs.size()) {
                        // Move to leaf blower phase
                        phase = 1;
                        index = 0;
                        leaves = findNearbyLeaves(logs, 6);
                    }

                } else if (phase == 1) {
                    // Phase 1: Leaf Blower — destroy leaves near the broken logs
                    if (leaves == null || leaves.isEmpty()) {
                        phase = 2;
                    } else {
                        for (int i = 0; i < blocksPerTick * 3 && index < leaves.size(); i++, index++) {
                            Block leaf = leaves.get(index);
                            if (!isLeaf(leaf.getType())) continue;
                            if (!BlockBreakHelper.canBreak(player, leaf)) continue;

                            // Leaves drop their items naturally (saplings, sticks, apples)
                            Collection<ItemStack> drops = leaf.getDrops();
                            leaf.getWorld().playEffect(leaf.getLocation(),
                                    org.bukkit.Effect.STEP_SOUND, leaf.getBlockData());
                            leaf.setType(Material.AIR);

                            // Notify AntiXRAY to reveal surrounding fake ores
                            AntiXrayHook.notifyBlockBroken(leaf.getWorld(),
                                    leaf.getX(), leaf.getY(), leaf.getZ());

                            boolean telepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);
                            if (telepathy) {
                                TelepathyListener.giveItems(player, drops);
                            } else {
                                for (ItemStack drop : drops) {
                                    leaf.getWorld().dropItemNaturally(
                                            leaf.getLocation().add(0.5, 0.5, 0.5), drop);
                                }
                            }
                        }
                        if (index >= leaves.size()) {
                            phase = 2;
                        }
                    }

                } else {
                    // Phase 2: Auto Replant
                    if (saplingType != null && bottomLog != null) {
                        Block replantBlock = bottomLog.getWorld().getBlockAt(
                                bottomLog.getX(), bottomLog.getY(), bottomLog.getZ());
                        Block below = replantBlock.getRelative(BlockFace.DOWN);

                        // Only replant if the spot is air and the block below is solid
                        if (replantBlock.getType() == Material.AIR && below.getType().isSolid()) {
                            replantBlock.setType(saplingType);
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Finds all leaf blocks within a radius of any of the given log positions.
     */
    private List<Block> findNearbyLeaves(List<Block> logs, int radius) {
        Set<Long> seen = new HashSet<>();
        List<Block> leaves = new ArrayList<>();

        for (Block log : logs) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block check = log.getRelative(x, y, z);
                        long key = blockKey(check);
                        if (!seen.contains(key) && isLeaf(check.getType())) {
                            seen.add(key);
                            leaves.add(check);
                        }
                    }
                }
            }
        }
        return leaves;
    }

    private static boolean isLog(Material mat) {
        return LOG_MATERIALS.contains(mat) || Tag.LOGS.isTagged(mat);
    }

    private static boolean isLeaf(Material mat) {
        return Tag.LEAVES.isTagged(mat);
    }

    private static long blockKey(Block block) {
        return ((long) block.getX() & 0x3FFFFFF) << 38
                | ((long) block.getZ() & 0x3FFFFFF) << 12
                | ((long) block.getY() & 0xFFF);
    }
}

