package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.hook.AntiXrayHook;
import pt.henrique.toolmods.hook.BuildersDreamHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Listener for Harvester 3×3 / 5×5 — harvests and replants mature crops.
 * <p>
 * Trigger: player breaks a fully mature crop with a hoe that has Harvester.
 * Only mature crops are harvested. Immature crops are left alone.
 * One seed from the drops is consumed for auto-replant.
 * Fortune affects drop amounts.
 */
public class HarvesterListener implements Listener {

    private final ToolMods plugin;

    /** Crops and their max age. */
    private static final Map<Material, Integer> CROP_MAX_AGE = Map.of(
            Material.WHEAT, 7,
            Material.CARROTS, 7,
            Material.POTATOES, 7,
            Material.BEETROOTS, 3,
            Material.NETHER_WART, 3
    );

    /** Crop → seed material used for replanting. */
    private static final Map<Material, Material> CROP_SEED = Map.of(
            Material.WHEAT, Material.WHEAT_SEEDS,
            Material.CARROTS, Material.CARROT,
            Material.POTATOES, Material.POTATO,
            Material.BEETROOTS, Material.BEETROOT_SEEDS,
            Material.NETHER_WART, Material.NETHER_WART
    );

    public HarvesterListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolCategory.HOE.matches(tool)) return;

        boolean hasHarv3 = ModUtils.isModEnabled(tool, ModType.HARVESTER_3X3);
        boolean hasHarv5 = ModUtils.isModEnabled(tool, ModType.HARVESTER_5X5);

        if (!hasHarv3 && !hasHarv5) return;

        Block brokenBlock = event.getBlock();
        if (!isMatureCrop(brokenBlock)) return;

        // Cancel the vanilla break — we handle the harvest
        event.setCancelled(true);

        int radius = hasHarv5 ? 2 : 1;
        boolean hasTelepathy = ModUtils.isModEnabled(tool, ModType.TELEPATHY);

        // Get the area (horizontal plane around the crop)
        List<Block> areaBlocks = BlockBreakHelper.getAreaBlocks(brokenBlock, BlockFace.UP, radius);

        for (Block block : areaBlocks) {
            if (!isMatureCrop(block)) continue;
            if (!BlockBreakHelper.canBreak(player, block)) continue;
            if (!BlockBreakHelper.isToolAlive(tool)) break;

            harvestAndReplant(block, tool, player, hasTelepathy);

            BlockBreakHelper.deductDurability(tool, player);
        }
    }

    /**
     * Harvests a single mature crop, gives drops, and replants.
     */
    private void harvestAndReplant(Block block, ItemStack tool, Player player, boolean telepathy) {
        Material cropType = block.getType();
        Material seedType = CROP_SEED.get(cropType);

        // Get drops (Fortune is applied)
        Collection<ItemStack> drops = block.getDrops(tool);
        List<ItemStack> dropList = new ArrayList<>(drops);

        // Remove one seed for replanting
        boolean seedRemoved = false;
        if (seedType != null) {
            for (int i = 0; i < dropList.size(); i++) {
                ItemStack drop = dropList.get(i);
                if (drop.getType() == seedType) {
                    if (drop.getAmount() > 1) {
                        drop.setAmount(drop.getAmount() - 1);
                    } else {
                        dropList.remove(i);
                    }
                    seedRemoved = true;
                    break;
                }
            }
        }

        // Play break effect and reset crop
        block.getWorld().playEffect(block.getLocation(), org.bukkit.Effect.STEP_SOUND, block.getBlockData());

        // Replant: reset to age 0
        if (seedRemoved && block.getBlockData() instanceof Ageable) {
            Ageable ageable = (Ageable) block.getBlockData();
            ageable.setAge(0);
            block.setBlockData(ageable);
        } else {
            // No seed available — just break the crop
            block.setType(Material.AIR);

            // Notify AntiXRAY to reveal surrounding fake ores
            AntiXrayHook.notifyBlockBroken(block.getWorld(), block.getX(), block.getY(), block.getZ());
        }

        // Drop suppression (vanilla parity + anti-farm): Creative / Spectator,
        // or an active BuildersDream service. Crop is still harvested + replanted;
        // only the drops are skipped. Same rationale as BlockBreakHelper.processDrops.
        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || BuildersDreamHook.hasActiveService(player.getUniqueId())) {
            return;
        }

        // Give drops
        if (telepathy) {
            TelepathyListener.giveItems(player, dropList);
        } else {
            for (ItemStack drop : dropList) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
            }
        }
    }

    /**
     * Checks if a block is a fully mature crop.
     */
    private static boolean isMatureCrop(Block block) {
        Integer maxAge = CROP_MAX_AGE.get(block.getType());
        if (maxAge == null) return false;
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        return ageable.getAge() >= maxAge;
    }
}

