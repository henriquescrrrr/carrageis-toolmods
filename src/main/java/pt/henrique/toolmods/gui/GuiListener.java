package pt.henrique.toolmods.gui;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles all GUI click events for ToolMods menus.
 * <p>
 * Uses {@link EventPriority#HIGHEST} and {@code ignoreCancelled = false} so that
 * other plugins (e.g. anti-cheat, protection plugins) that cancel
 * {@link InventoryClickEvent} at lower priorities cannot silently swallow our clicks.
 * We exit immediately for inventories that are NOT ours, so we never interfere
 * with other plugins' GUIs.
 */
public class GuiListener implements Listener {

    private final ToolMods plugin;

    public GuiListener(ToolMods plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns {@code true} if the given holder belongs to a ToolMods GUI.
     */
    private static boolean isToolModsGui(InventoryHolder holder) {
        return holder instanceof MainMenuGui
                || holder instanceof CategoryGui
                || holder instanceof MyModsGui
                || holder instanceof ConfirmationGui;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        // Only process ToolMods GUIs — exit immediately otherwise
        if (holder == null || !isToolModsGui(holder)) return;

        // Always cancel clicks in our GUIs (prevents item theft)
        event.setCancelled(true);

        // Only process clicks in the top inventory
        if (event.getClickedInventory() != topInventory) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        SoundUtil.click(player);

        if (holder instanceof MainMenuGui gui) {
            gui.handleClick(slot);
        } else if (holder instanceof CategoryGui gui) {
            gui.handleClick(slot);
        } else if (holder instanceof MyModsGui gui) {
            gui.handleClick(slot);
        } else if (holder instanceof ConfirmationGui gui) {
            gui.handleClick(slot);
        }
    }

    /**
     * Prevent dragging items in our GUIs.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder == null || !isToolModsGui(holder)) return;

        // Cancel if any dragged slots are in the top inventory
        for (int slot : event.getRawSlots()) {
            if (slot < topInventory.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Clean-up hook when a ToolMods GUI is closed (e.g. by the player or programmatically).
     * Currently a no-op but provides a central place for future state clean-up.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder == null || !isToolModsGui(holder)) return;

        // Future: clean up any pending state for this player if needed
    }
}

