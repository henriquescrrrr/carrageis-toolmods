package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.hook.AntiXrayHook;
import pt.henrique.toolmods.hook.BuildersDreamHook;
import pt.henrique.toolmods.hook.LandClaimHook;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared utility for pickaxe (and later shovel/hoe) mod block-breaking operations.
 * <p>
 * Handles: durability deduction with Unbreaking, tool tier checks,
 * area block calculation, single-block drop processing, and LandClaim hooks.
 */
public final class BlockBreakHelper {

    private BlockBreakHelper() {}

    // ========================
    // Durability
    // ========================

    /**
     * Deducts 1 durability from the tool, respecting the Unbreaking enchantment.
     * If the Unbreakable mod is on the tool, no durability is lost.
     *
     * @return true if the tool is still alive after deduction; false if it broke
     */
    public static boolean deductDurability(ItemStack tool, Player player) {
        if (tool == null || tool.getType().isAir()) return false;

        // Unbreakable mod → no durability loss
        if (ModUtils.isModEnabled(tool, ModType.UNBREAKABLE)) return true;

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) return true;

        // Unbreaking enchantment: chance to not lose durability = level / (level + 1)
        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0) {
            double skipChance = (double) unbreaking / (unbreaking + 1);
            if (ThreadLocalRandom.current().nextDouble() < skipChance) {
                return true; // no durability lost this time
            }
        }

        int newDamage = damageable.getDamage() + 1;
        int maxDurability = tool.getType().getMaxDurability();

        if (newDamage >= maxDurability) {
            // Tool broke
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return false;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
        return true;
    }

    /**
     * Checks if the tool still has durability remaining.
     */
    public static boolean isToolAlive(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) return false;
        if (!(tool.getItemMeta() instanceof Damageable damageable)) return true;
        return damageable.getDamage() < tool.getType().getMaxDurability();
    }

    // ========================
    // Tool Tier / Harvest Checks
    // ========================

    /**
     * Checks if the given pickaxe can harvest the block (correct tier).
     * Returns false for air, liquids, bedrock, and blocks requiring a higher tier.
     */
    public static boolean canPickaxeHarvest(ItemStack tool, Block block) {
        Material blockType = block.getType();
        if (blockType.isAir() || blockType == Material.BEDROCK || blockType == Material.BARRIER
                || !blockType.isSolid()) {
            return false;
        }

        // Check if the block requires a minimum tool tier
        int toolTier = getToolTier(tool.getType());
        int requiredTier = getRequiredTier(blockType);
        return toolTier >= requiredTier;
    }

    /**
     * Returns the mining tier of the tool material.
     * 0 = wood/gold, 1 = stone, 2 = iron, 3 = diamond/netherite
     */
    private static int getToolTier(Material toolMat) {
        String name = toolMat.name();
        if (name.startsWith("NETHERITE_") || name.startsWith("DIAMOND_")) return 3;
        if (name.startsWith("IRON_")) return 2;
        if (name.startsWith("STONE_")) return 1;
        return 0; // wooden, golden
    }

    /**
     * Returns the minimum tool tier required to harvest a block.
     */
    private static int getRequiredTier(Material blockType) {
        // Diamond tier required
        if (Tag.NEEDS_DIAMOND_TOOL.isTagged(blockType)) return 3;
        // Iron tier required
        if (Tag.NEEDS_IRON_TOOL.isTagged(blockType)) return 2;
        // Stone tier required
        if (Tag.NEEDS_STONE_TOOL.isTagged(blockType)) return 1;
        return 0;
    }

    // ========================
    // Area Block Calculation
    // ========================

    /**
     * Calculates the blocks in a square area based on the block face hit.
     * <p>
     * The plane is perpendicular to the face direction:
     * <ul>
     *   <li>UP/DOWN → XZ plane (horizontal)</li>
     *   <li>NORTH/SOUTH → XY plane (vertical NS wall)</li>
     *   <li>EAST/WEST → YZ plane (vertical EW wall)</li>
     * </ul>
     *
     * @param center the center block
     * @param face   the block face that was hit
     * @param radius 1 for 3×3, 2 for 5×5
     * @return list of blocks in the area (includes center)
     */
    public static List<Block> getAreaBlocks(Block center, BlockFace face, int radius) {
        List<Block> blocks = new ArrayList<>();
        World world = center.getWorld();
        int cx = center.getX(), cy = center.getY(), cz = center.getZ();

        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                int x = cx, y = cy, z = cz;
                switch (face) {
                    case UP, DOWN -> { x += a; z += b; }
                    case NORTH, SOUTH -> { x += a; y += b; }
                    case EAST, WEST -> { y += a; z += b; }
                    default -> { x += a; z += b; } // fallback to horizontal
                }
                blocks.add(world.getBlockAt(x, y, z));
            }
        }
        return blocks;
    }

    /**
     * Gets the block face the player is looking at via ray trace.
     * Falls back to UP if the ray trace fails.
     */
    public static BlockFace getTargetBlockFace(Player player) {
        var result = player.rayTraceBlocks(6);
        if (result != null && result.getHitBlockFace() != null) {
            return result.getHitBlockFace();
        }
        return BlockFace.UP; // fallback to horizontal plane
    }

    // ========================
    // Single Block Processing
    // ========================

    /**
     * Processes drops for a single block: applies Auto Smelt and Telepathy.
     * The block must already have been removed (set to AIR) before calling this.
     *
     * @param block    the block location (already broken)
     * @param tool     the pickaxe used
     * @param player   the player
     * @param drops    the raw drops from the block
     * @param xp       the XP to award
     * @param autoSmelt whether Auto Smelt should be applied
     * @param telepathy whether Telepathy should be applied
     * @param hasSilkTouch whether the tool has Silk Touch (disables Auto Smelt)
     */
    public static void processDrops(Block block, ItemStack tool, Player player,
                                     Collection<ItemStack> drops, int xp,
                                     boolean autoSmelt, boolean telepathy,
                                     boolean hasSilkTouch) {
        // Drop suppression (vanilla parity + anti-farm):
        //   - Creative / Spectator never drop items or XP from broken blocks.
        //   - While a BuildersDream builder service is active, every block the
        //     player breaks drops nothing. BuildersDream suppresses the directly
        //     broken block via BlockBreakEvent, but our multi-block mods break
        //     the extra blocks themselves (breakAndCollect → setType(AIR)),
        //     bypassing that event — so we must honour the protection here.
        // The block is already broken in breakAndCollect(); we just skip drops.
        if (player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || BuildersDreamHook.hasActiveService(player.getUniqueId())) {
            return;
        }

        List<ItemStack> processed;

        // Auto Smelt (only if no Silk Touch)
        if (autoSmelt && !hasSilkTouch) {
            processed = AutoSmeltListener.smelt(drops);
        } else {
            processed = new ArrayList<>(drops);
        }

        // Telepathy: give to player. Otherwise: drop on ground.
        if (telepathy) {
            TelepathyListener.giveItems(player, processed);
            TelepathyListener.giveXp(player, xp);
        } else {
            Location dropLoc = block.getLocation().add(0.5, 0.5, 0.5);
            for (ItemStack item : processed) {
                block.getWorld().dropItemNaturally(dropLoc, item);
            }
            if (xp > 0) {
                block.getWorld().spawn(dropLoc, org.bukkit.entity.ExperienceOrb.class,
                        orb -> orb.setExperience(xp));
            }
        }
    }

    /**
     * Breaks a block, plays the effect, and returns the drops + XP.
     * Sets the block to AIR. Does NOT deduct durability (caller handles that).
     */
    public static BlockDropResult breakAndCollect(Block block, ItemStack tool) {
        Collection<ItemStack> drops = block.getDrops(tool);
        boolean hasSilkTouch = tool.containsEnchantment(Enchantment.SILK_TOUCH);
        int xp = estimateExpDrop(block.getType(), hasSilkTouch);

        // Play break effect
        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getBlockData());
        block.setType(Material.AIR);

        // Notify AntiXRAY to reveal surrounding fake ores
        AntiXrayHook.notifyBlockBroken(block.getWorld(), block.getX(), block.getY(), block.getZ());

        return new BlockDropResult(drops, xp);
    }

    /**
     * Estimates the XP dropped by a block type (used for manual block breaking).
     * Silk Touch blocks do not drop XP.
     */
    private static int estimateExpDrop(Material blockType, boolean hasSilkTouch) {
        if (hasSilkTouch) return 0;
        return switch (blockType) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> ThreadLocalRandom.current().nextInt(0, 3);
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> ThreadLocalRandom.current().nextInt(3, 8);
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> ThreadLocalRandom.current().nextInt(3, 8);
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> ThreadLocalRandom.current().nextInt(2, 6);
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> ThreadLocalRandom.current().nextInt(1, 6);
            case NETHER_QUARTZ_ORE -> ThreadLocalRandom.current().nextInt(2, 6);
            case NETHER_GOLD_ORE -> ThreadLocalRandom.current().nextInt(0, 2);
            default -> 0;
        };
    }

    /**
     * Result of breaking a block: its drops and XP value.
     */
    public record BlockDropResult(Collection<ItemStack> drops, int xp) {}

    // ========================
    // Shovel Block Checks
    // ========================

    /** Blocks that the Excavator mod can break (shovel-appropriate). */
    private static final Set<Material> SHOVEL_BLOCKS = Set.of(
            Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.RED_SAND,
            Material.GRAVEL, Material.CLAY, Material.SOUL_SAND, Material.SOUL_SOIL,
            Material.SNOW_BLOCK, Material.MUD, Material.COARSE_DIRT, Material.ROOTED_DIRT,
            Material.PODZOL, Material.MYCELIUM, Material.FARMLAND, Material.DIRT_PATH,
            Material.POWDER_SNOW, Material.MUDDY_MANGROVE_ROOTS,
            // All concrete powder colors
            Material.WHITE_CONCRETE_POWDER, Material.ORANGE_CONCRETE_POWDER,
            Material.MAGENTA_CONCRETE_POWDER, Material.LIGHT_BLUE_CONCRETE_POWDER,
            Material.YELLOW_CONCRETE_POWDER, Material.LIME_CONCRETE_POWDER,
            Material.PINK_CONCRETE_POWDER, Material.GRAY_CONCRETE_POWDER,
            Material.LIGHT_GRAY_CONCRETE_POWDER, Material.CYAN_CONCRETE_POWDER,
            Material.PURPLE_CONCRETE_POWDER, Material.BLUE_CONCRETE_POWDER,
            Material.BROWN_CONCRETE_POWDER, Material.GREEN_CONCRETE_POWDER,
            Material.RED_CONCRETE_POWDER, Material.BLACK_CONCRETE_POWDER
    );

    /**
     * Checks if a block is a shovel-appropriate block for Excavator.
     */
    public static boolean isShovelBlock(Material material) {
        return SHOVEL_BLOCKS.contains(material);
    }

    // ========================
    // LandClaim Check (placeholder for Phase 6)
    // ========================

    /**
     * Checks if a player can build/break at a given block location.
     * Delegates to LandClaimHook if LandClaim is installed.
     *
     * @return true if the player can break the block
     */
    public static boolean canBreak(Player player, Block block) {
        return LandClaimHook.canBreak(player, block);
    }
}


