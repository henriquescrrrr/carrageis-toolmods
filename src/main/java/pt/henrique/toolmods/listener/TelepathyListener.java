package pt.henrique.toolmods.listener;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;

/**
 * Telepathy utility — adds drops directly to the player's inventory.
 * <p>
 * If the inventory is full, leftover items are dropped on the ground.
 * XP orbs are given directly to the player.
 */
public final class TelepathyListener {

    private TelepathyListener() {}

    /**
     * Gives items directly to the player's inventory.
     * Items that don't fit are dropped at the player's location.
     *
     * @param player the player to receive items
     * @param drops  the items to give
     */
    public static void giveItems(Player player, Collection<ItemStack> drops) {
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType().isAir()) continue;
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            // Drop any items that didn't fit
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    /**
     * Gives XP directly to the player (no orb entity spawned).
     *
     * @param player the player to receive XP
     * @param amount the XP amount
     */
    public static void giveXp(Player player, int amount) {
        if (amount > 0) {
            player.giveExp(amount);
        }
    }
}

