package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Decapitator — chance to drop a head when killing an entity.
 * <p>
 * Players: 100% chance to drop a player head with their skin.
 * Mobs: configurable chance (default 5%) — only mobs with vanilla heads.
 */
public class DecapitatorListener implements Listener {

    private final ToolMods plugin;

    /** Mob entity types that have vanilla head items. */
    private static final Map<EntityType, Material> MOB_HEADS = Map.of(
            EntityType.SKELETON, Material.SKELETON_SKULL,
            EntityType.ZOMBIE, Material.ZOMBIE_HEAD,
            EntityType.CREEPER, Material.CREEPER_HEAD,
            EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL,
            EntityType.ENDER_DRAGON, Material.DRAGON_HEAD,
            EntityType.PIGLIN, Material.PIGLIN_HEAD
    );

    public DecapitatorListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (!ToolCategory.SWORD.matches(tool)) return;
        if (!ModUtils.isModEnabled(tool, ModType.DECAPITATOR)) return;

        // Player kill → 100% player head
        if (entity instanceof Player victim) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(victim);
            head.setItemMeta(meta);
            event.getDrops().add(head);
            return;
        }

        // Mob kill → chance-based, only for mobs with vanilla heads
        Material headMaterial = MOB_HEADS.get(entity.getType());
        if (headMaterial == null) return;

        double chance = plugin.getConfigManager().getDecapitatorMobHeadChance();
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            event.getDrops().add(new ItemStack(headMaterial));
        }
    }
}

