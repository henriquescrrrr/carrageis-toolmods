package pt.henrique.toolmods.gui;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.util.ItemBuilder;
import pt.henrique.toolmods.util.SoundUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * My Mods GUI — shows all mods on the currently held item.
 * Each mod is shown as green wool (enabled) or red wool (disabled).
 * Click to toggle.
 * <p>
 * Layout (45 slots / 5 rows):
 * <pre>
 * Row 0: [FILLER x4] [HELD_ITEM_ICON] [FILLER x4]
 * Row 1: mod toggle items centered
 * Row 2: overflow mods if needed
 * Row 3: [FILLER x9]
 * Row 4: [BACK] [FILLER x7] [CLOSE]
 * </pre>
 */
public class MyModsGui implements InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 45;

    private static final int SLOT_HELD_ITEM = 4;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CLOSE = 44;

    /** PDC key used to tag mod toggle items for identification. */
    static final NamespacedKey GUI_MOD_KEY = new NamespacedKey("toolmods", "gui_mod_key");

    private final ToolMods plugin;
    private final Player player;
    private Inventory inventory;

    public MyModsGui(ToolMods plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Check: player is holding an item
        if (heldItem.getType().isAir()) {
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.hold-item"));
            SoundUtil.error(player);
            return;
        }

        // Check: item has mods
        List<ModType> mods = ModUtils.getMods(heldItem);
        if (mods.isEmpty()) {
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.no-mods-on-item"));
            SoundUtil.error(player);
            return;
        }

        String title = plugin.getLangManager().getRaw(lang, "shop.my-mods-title");
        inventory = Bukkit.createInventory(this, SIZE, MINI.deserialize(title));

        buildGui(lang, heldItem, mods);
        player.openInventory(inventory);
    }

    /**
     * Rebuilds the GUI contents.
     */
    void buildGui(String lang, ItemStack heldItem, List<ModType> mods) {
        // Fill everything
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Display the held item in top center (clone for display only)
        inventory.setItem(SLOT_HELD_ITEM, heldItem.clone());

        // Mod toggle items — rows 1 and 2
        int[] row1Slots = centerSlots(9, 9, Math.min(mods.size(), 9));
        int[] row2Slots = mods.size() > 9 ? centerSlots(18, 9, mods.size() - 9) : new int[0];

        for (int i = 0; i < mods.size(); i++) {
            ModType mod = mods.get(i);
            int slot;
            if (i < row1Slots.length) {
                slot = row1Slots[i];
            } else if (i - 9 < row2Slots.length) {
                slot = row2Slots[i - 9];
            } else {
                break;
            }
            inventory.setItem(slot, buildToggleItem(lang, mod, heldItem));
        }

        // Back button
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name(plugin.getLangManager().getRaw(lang, "gui.navigation.back"))
                .build());

        // Close button
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .name(plugin.getLangManager().getRaw(lang, "gui.main-menu.close"))
                .build());
    }

    /**
     * Builds a toggle item for a mod (green wool = enabled, red wool = disabled).
     */
    private ItemStack buildToggleItem(String lang, ModType mod, ItemStack heldItem) {
        boolean enabled = ModUtils.isModEnabled(heldItem, mod);
        String modName = plugin.getLangManager().getRaw(lang, mod.getNameLangPath());

        Material material = enabled ? Material.LIME_WOOL : Material.RED_WOOL;
        String statusLine = enabled
                ? plugin.getLangManager().getRaw(lang, "gui.toggle.enabled")
                : plugin.getLangManager().getRaw(lang, "gui.toggle.disabled");

        List<String> lore = new ArrayList<>();
        List<String> descLines = plugin.getLangManager().getRawList(lang, mod.getDescriptionLangPath());
        lore.addAll(descLines);
        lore.add("");
        lore.add(statusLine);

        return new ItemBuilder(material)
                .name(modName)
                .lore(lore)
                .pdc(GUI_MOD_KEY, mod.getKey())
                .build();
    }

    /**
     * Handles a click in this GUI.
     */
    public void handleClick(int slot) {
        if (slot == SLOT_BACK) {
            new MainMenuGui(plugin, player).open();
            return;
        }
        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        // Check if a mod toggle item was clicked
        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String modKey = clicked.getItemMeta().getPersistentDataContainer()
                .get(GUI_MOD_KEY, PersistentDataType.STRING);
        if (modKey == null) return;

        ModType mod = ModType.byKey(modKey);
        if (mod == null) return;

        // Toggle the mod on the held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType().isAir() || !ModUtils.hasMod(heldItem, mod)) return;

        boolean currentlyEnabled = ModUtils.isModEnabled(heldItem, mod);
        ModUtils.setModEnabled(heldItem, mod, !currentlyEnabled);

        String lang = plugin.getLangManager().getPlayerLanguage(player);
        String modName = plugin.getLangManager().getRaw(lang, mod.getNameLangPath());

        if (currentlyEnabled) {
            // Was enabled → now disabled
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.mod-toggled-off",
                    "mod", modName));
        } else {
            // Was disabled → now enabled
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.mod-toggled-on",
                    "mod", modName));
        }
        SoundUtil.toggle(player);

        // Rebuild GUI with updated state
        List<ModType> mods = ModUtils.getMods(heldItem);
        buildGui(lang, heldItem, mods);
    }

    // ==================== Helpers ====================

    private int[] centerSlots(int rowStart, int rowSize, int count) {
        if (count <= 0) return new int[0];
        int[] slots = new int[count];
        int offset = (rowSize - count) / 2;
        for (int i = 0; i < count; i++) {
            slots[i] = rowStart + offset + i;
        }
        return slots;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

