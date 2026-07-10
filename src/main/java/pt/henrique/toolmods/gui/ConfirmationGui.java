package pt.henrique.toolmods.gui;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ToolCategory;
import pt.henrique.toolmods.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Confirmation GUI — 3-row (27 slots) confirmation step before purchasing a mod.
 * <pre>
 * Middle row: [GREEN_WOOL @ 12] [MOD_PREVIEW @ 13] [RED_WOOL @ 14]
 * All other slots are filler glass panes.
 * </pre>
 */
public class ConfirmationGui implements InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 27;
    private static final int SLOT_CONFIRM = 12;
    private static final int SLOT_PREVIEW = 13;
    private static final int SLOT_CANCEL  = 14;

    private final ToolMods plugin;
    private final Player player;
    private final ModType mod;
    private final ToolCategory sourceCategory;
    private Inventory inventory;

    public ConfirmationGui(ToolMods plugin, Player player, ModType mod, ToolCategory sourceCategory) {
        this.plugin = plugin;
        this.player = player;
        this.mod = mod;
        this.sourceCategory = sourceCategory;
    }

    public void open() {
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        String title = plugin.getLangManager().getRaw(lang, "confirm-title");
        inventory = Bukkit.createInventory(this, SIZE, MINI.deserialize(title));

        double price = plugin.getConfigManager().getModPrice(mod);
        String formattedPrice = plugin.getConfigManager().formatCurrency(price);
        String modName = plugin.getLangManager().getRaw(lang, mod.getNameLangPath());

        // Fill all slots with filler glass panes
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 13: Mod preview item (center of middle row)
        List<String> previewLore = new ArrayList<>();
        List<String> descLines = plugin.getLangManager().getRawList(lang, mod.getDescriptionLangPath());
        previewLore.addAll(descLines);
        previewLore.add("");
        previewLore.add(plugin.getLangManager().getRaw(lang, "gui.status.price").replace("{price}", formattedPrice));
        ItemStack previewItem = new ItemBuilder(mod.getIcon())
                .name(modName)
                .lore(previewLore)
                .hideFlags()
                .build();
        inventory.setItem(SLOT_PREVIEW, previewItem);

        // Slot 12: Green wool — Confirm Purchase (one left of center)
        String confirmName = plugin.getLangManager().getRaw(lang, "confirm-buy");
        String confirmLore = plugin.getLangManager().getRaw(lang, "confirm-buy-lore")
                .replace("{mod}", modName)
                .replace("{price}", formattedPrice);
        ItemStack confirmItem = new ItemBuilder(Material.GREEN_WOOL)
                .name(confirmName)
                .lore(List.of(confirmLore))
                .build();
        inventory.setItem(SLOT_CONFIRM, confirmItem);

        // Slot 14: Red wool — Cancel (one right of center)
        String cancelName = plugin.getLangManager().getRaw(lang, "confirm-cancel");
        ItemStack cancelItem = new ItemBuilder(Material.RED_WOOL)
                .name(cancelName)
                .build();
        inventory.setItem(SLOT_CANCEL, cancelItem);

        player.openInventory(inventory);
    }

    /**
     * Handles a click in this GUI.
     */
    public void handleClick(int slot) {
        if (slot == SLOT_CANCEL) {
            // Cancel — go back to category GUI
            new CategoryGui(plugin, player, sourceCategory).open();
        } else if (slot == SLOT_CONFIRM) {
            // Confirm — execute purchase via CategoryGui
            CategoryGui categoryGui = new CategoryGui(plugin, player, sourceCategory);
            categoryGui.executePurchase(mod);
        }
        // Preview and fillers — do nothing
    }

    public ModType getMod() {
        return mod;
    }

    public ToolCategory getSourceCategory() {
        return sourceCategory;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

