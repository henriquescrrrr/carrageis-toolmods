package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Listener for Tiller — tills a 3×3 area of dirt/grass into farmland.
 * <p>
 * Trigger: right-click on DIRT or GRASS_BLOCK with a hoe that has Tiller.
 * Converts a 3×3 horizontal area to FARMLAND.
 * Deducts 1 durability per block tilled.
 */
public class TillerListener implements Listener {

    private final ToolMods plugin;

    private static final Set<Material> TILLABLE = Set.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT, Material.ROOTED_DIRT,
            Material.DIRT_PATH
    );

    public TillerListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolCategory.HOE.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.TILLER)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !TILLABLE.contains(clicked.getType())) return;

        // Cancel the vanilla tilling (we handle it ourselves)
        event.setCancelled(true);

        // Get a 3×3 horizontal area centered on the clicked block
        List<Block> area = BlockBreakHelper.getAreaBlocks(clicked, BlockFace.UP, 1);

        for (Block block : area) {
            if (!TILLABLE.contains(block.getType())) continue;
            if (!BlockBreakHelper.canBreak(player, block)) continue;

            // Check block above is air (farmland needs open space)
            if (!block.getRelative(BlockFace.UP).getType().isAir()) continue;

            block.setType(Material.FARMLAND);

            if (!BlockBreakHelper.deductDurability(tool, player)) {
                break; // tool broke
            }
        }
    }
}

