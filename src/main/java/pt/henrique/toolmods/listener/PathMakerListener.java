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

import java.util.Set;

/**
 * Listener for Path Maker — converts a 3-wide strip of grass/dirt into a path.
 * <p>
 * Trigger: right-click on GRASS_BLOCK or DIRT with a shovel that has Path Maker.
 * Converts 3 blocks wide perpendicular to the player's facing direction.
 * Deducts 1 durability per block converted.
 */
public class PathMakerListener implements Listener {

    private final ToolMods plugin;

    private static final Set<Material> PATHABLE = Set.of(
            Material.GRASS_BLOCK, Material.DIRT
    );

    public PathMakerListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (!ToolCategory.SHOVEL.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.PATH_MAKER)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || !PATHABLE.contains(clicked.getType())) return;

        // Cancel the vanilla path creation (we handle it ourselves)
        event.setCancelled(true);

        // Determine the 3-wide strip perpendicular to the player's facing
        BlockFace facing = getCardinalFacing(player);
        BlockFace left = getLeftFace(facing);
        BlockFace right = getRightFace(facing);

        Block[] targets = {
                clicked.getRelative(left),
                clicked,
                clicked.getRelative(right)
        };

        for (Block target : targets) {
            if (!PATHABLE.contains(target.getType())) continue;
            if (!BlockBreakHelper.canBreak(player, target)) continue;

            // Check block above is air (paths need open space above)
            if (!target.getRelative(BlockFace.UP).getType().isAir()) continue;

            target.setType(Material.DIRT_PATH);

            if (!BlockBreakHelper.deductDurability(tool, player)) {
                break; // tool broke
            }
        }
    }

    /**
     * Gets the cardinal direction the player is facing (N/S/E/W).
     */
    private static BlockFace getCardinalFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private static BlockFace getLeftFace(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case WEST -> BlockFace.SOUTH;
            default -> BlockFace.WEST;
        };
    }

    private static BlockFace getRightFace(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case SOUTH -> BlockFace.WEST;
            case EAST -> BlockFace.SOUTH;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }
}

