package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Vitality (Chestplate) — +2 extra hearts (4 HP) while chestplate equipped.
 * <p>
 * Uses an {@link AttributeModifier} on {@link Attribute#MAX_HEALTH}.
 * A repeating task every 40 ticks adds or removes the modifier as needed.
 * When removing, current health is capped to the new max.
 */
public class VitalityListener implements Listener {

    private final NamespacedKey VITALITY_KEY;
    private final ToolMods plugin;

    public VitalityListener(ToolMods plugin) {
        this.plugin = plugin;
        this.VITALITY_KEY = new NamespacedKey(plugin, "toolmods_vitality");
        startTask();
    }

    private void startTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double extraHealth = plugin.getConfigManager().getVitalityExtraHealth();

            for (Player player : Bukkit.getOnlinePlayers()) {
                ItemStack chestplate = player.getInventory().getChestplate();
                boolean hasVitality = chestplate != null
                        && ModUtils.isModEnabled(chestplate, ModType.VITALITY);

                AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
                if (attr == null) continue;

                boolean hasModifier = hasOurModifier(attr);

                if (hasVitality && !hasModifier) {
                    // Add modifier
                    attr.addModifier(new AttributeModifier(
                            VITALITY_KEY, extraHealth,
                            AttributeModifier.Operation.ADD_NUMBER));
                } else if (!hasVitality && hasModifier) {
                    // Remove modifier
                    removeOurModifier(attr);
                    // Cap health to new max
                    double max = attr.getValue();
                    if (player.getHealth() > max) {
                        player.setHealth(max);
                    }
                }
            }
        }, 40L, 40L);
    }

    private boolean hasOurModifier(AttributeInstance attr) {
        for (AttributeModifier m : attr.getModifiers()) {
            if (m.getKey().equals(VITALITY_KEY)) return true;
        }
        return false;
    }

    private void removeOurModifier(AttributeInstance attr) {
        for (AttributeModifier m : new java.util.ArrayList<>(attr.getModifiers())) {
            if (m.getKey().equals(VITALITY_KEY)) {
                attr.removeModifier(m);
                break;
            }
        }
    }
}

