package pt.henrique.toolmods.gui;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ToolCategory;
import pt.henrique.toolmods.util.ItemBuilder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Main shop menu — category selection.
 * <p>
 * 6-row chest (54 slots):
 * <pre>
 * Row 0: [FILLER x4] [PLAYER_HEAD] [FILLER x4]
 * Row 1: [PICKAXE] [F] [AXE] [F] [SHOVEL] [F] [HOE] [F] [SWORD]
 * Row 2: [SPEAR] [F] [SHIELD] [F] [MACE] [F] [BOW] [F] [UNIVERSAL]
 * Row 3: [HELMET] [F] [CHESTPLATE] [F] [LEGGINGS] [F] [BOOTS] [F] [ELYTRA]
 * Row 4: [FILLER x4] [MY_MODS] [FILLER x4]
 * Row 5: [FILLER x4] [CLOSE] [FILLER x4]
 * </pre>
 */
public class MainMenuGui implements InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 54;

    // Slot constants
    private static final int SLOT_TITLE_HEAD = 4;
    // Row 1: categories
    private static final int SLOT_PICKAXE = 9;
    private static final int SLOT_AXE = 11;
    private static final int SLOT_SHOVEL = 13;
    private static final int SLOT_HOE = 15;
    private static final int SLOT_SWORD = 17;
    // Row 2: more categories
    private static final int SLOT_SPEAR = 18;
    private static final int SLOT_SHIELD = 20;
    private static final int SLOT_MACE = 22;
    private static final int SLOT_BOW = 24;
    private static final int SLOT_UNIVERSAL = 26;
    // Row 3: armor + elytra + my mods
    private static final int SLOT_HELMET = 27;
    private static final int SLOT_CHESTPLATE = 29;
    private static final int SLOT_LEGGINGS = 31;
    private static final int SLOT_BOOTS = 33;
    private static final int SLOT_ELYTRA = 35;
    // Row 4: my mods
    private static final int SLOT_MY_MODS = 40;
    // Row 5: close
    private static final int SLOT_CLOSE = 49;

    private final ToolMods plugin;
    private final Player player;
    private Inventory inventory;

    public MainMenuGui(ToolMods plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        String title = plugin.getLangManager().getRaw(lang, "shop.title");
        inventory = Bukkit.createInventory(this, SIZE, MINI.deserialize(title));

        // Fill with glass panes
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Title player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.displayName(MINI.deserialize(plugin.getLangManager().getRaw(lang, "gui.main-menu.title-item")));
        skullMeta.lore(plugin.getLangManager().getRawList(lang, "gui.main-menu.title-lore").stream()
                .map(MINI::deserialize).toList());
        head.setItemMeta(skullMeta);
        inventory.setItem(SLOT_TITLE_HEAD, head);

        // Category buttons — Row 1
        inventory.setItem(SLOT_PICKAXE, buildCategoryItem(lang, Material.DIAMOND_PICKAXE,
                "gui.main-menu.pickaxe", "gui.main-menu.pickaxe-lore"));
        inventory.setItem(SLOT_AXE, buildCategoryItem(lang, Material.DIAMOND_AXE,
                "gui.main-menu.axe", "gui.main-menu.axe-lore"));
        inventory.setItem(SLOT_SHOVEL, buildCategoryItem(lang, Material.DIAMOND_SHOVEL,
                "gui.main-menu.shovel", "gui.main-menu.shovel-lore"));
        inventory.setItem(SLOT_HOE, buildCategoryItem(lang, Material.DIAMOND_HOE,
                "gui.main-menu.hoe", "gui.main-menu.hoe-lore"));
        inventory.setItem(SLOT_SWORD, buildCategoryItem(lang, Material.DIAMOND_SWORD,
                "gui.main-menu.sword", "gui.main-menu.sword-lore"));

        // Category buttons — Row 2
        inventory.setItem(SLOT_SPEAR, buildCategoryItem(lang, Material.DIAMOND_SPEAR,
                "gui.main-menu.spear", "gui.main-menu.spear-lore"));
        inventory.setItem(SLOT_SHIELD, buildCategoryItem(lang, Material.SHIELD,
                "gui.main-menu.shield", "gui.main-menu.shield-lore"));
        inventory.setItem(SLOT_MACE, buildCategoryItem(lang, Material.MACE,
                "gui.main-menu.mace", "gui.main-menu.mace-lore"));
        inventory.setItem(SLOT_BOW, buildCategoryItem(lang, Material.BOW,
                "gui.main-menu.bow", "gui.main-menu.bow-lore"));
        inventory.setItem(SLOT_UNIVERSAL, buildCategoryItem(lang, Material.NETHER_STAR,
                "gui.main-menu.universal", "gui.main-menu.universal-lore"));

        // Category buttons — Row 3 (armor + my mods)
        inventory.setItem(SLOT_HELMET, buildCategoryItem(lang, Material.DIAMOND_HELMET,
                "gui.main-menu.helmet", "gui.main-menu.helmet-lore"));
        inventory.setItem(SLOT_CHESTPLATE, buildCategoryItem(lang, Material.DIAMOND_CHESTPLATE,
                "gui.main-menu.chestplate", "gui.main-menu.chestplate-lore"));
        inventory.setItem(SLOT_LEGGINGS, buildCategoryItem(lang, Material.DIAMOND_LEGGINGS,
                "gui.main-menu.leggings", "gui.main-menu.leggings-lore"));
        inventory.setItem(SLOT_BOOTS, buildCategoryItem(lang, Material.DIAMOND_BOOTS,
                "gui.main-menu.boots", "gui.main-menu.boots-lore"));

        // Elytra category
        inventory.setItem(SLOT_ELYTRA, buildCategoryItem(lang, Material.ELYTRA,
                "gui.main-menu.elytra", "gui.main-menu.elytra-lore"));

        // My Mods button
        inventory.setItem(SLOT_MY_MODS, new ItemBuilder(Material.BOOK)
                .name(plugin.getLangManager().getRaw(lang, "gui.main-menu.my-mods"))
                .lore(plugin.getLangManager().getRawList(lang, "gui.main-menu.my-mods-lore"))
                .glow()
                .build());

        // Close button
        inventory.setItem(SLOT_CLOSE, new ItemBuilder(Material.BARRIER)
                .name(plugin.getLangManager().getRaw(lang, "gui.main-menu.close"))
                .build());

        player.openInventory(inventory);
    }

    private ItemStack buildCategoryItem(String lang, Material material, String nameKey, String loreKey) {
        return new ItemBuilder(material)
                .name(plugin.getLangManager().getRaw(lang, nameKey))
                .lore(plugin.getLangManager().getRawList(lang, loreKey))
                .hideFlags()
                .build();
    }

    /**
     * Handles a click in this GUI. Returns true if the event should be cancelled.
     */
    public boolean handleClick(int slot) {
        switch (slot) {
            case SLOT_PICKAXE -> new CategoryGui(plugin, player, ToolCategory.PICKAXE).open();
            case SLOT_AXE -> new CategoryGui(plugin, player, ToolCategory.AXE).open();
            case SLOT_SHOVEL -> new CategoryGui(plugin, player, ToolCategory.SHOVEL).open();
            case SLOT_HOE -> new CategoryGui(plugin, player, ToolCategory.HOE).open();
            case SLOT_SWORD -> new CategoryGui(plugin, player, ToolCategory.SWORD).open();
            case SLOT_SPEAR -> new CategoryGui(plugin, player, ToolCategory.SPEAR).open();
            case SLOT_SHIELD -> new CategoryGui(plugin, player, ToolCategory.SHIELD).open();
            case SLOT_MACE -> new CategoryGui(plugin, player, ToolCategory.MACE).open();
            case SLOT_BOW -> new CategoryGui(plugin, player, ToolCategory.BOW).open();
            case SLOT_UNIVERSAL -> new CategoryGui(plugin, player, ToolCategory.UNIVERSAL).open();
            case SLOT_HELMET -> new CategoryGui(plugin, player, ToolCategory.HELMET).open();
            case SLOT_CHESTPLATE -> new CategoryGui(plugin, player, ToolCategory.CHESTPLATE).open();
            case SLOT_LEGGINGS -> new CategoryGui(plugin, player, ToolCategory.LEGGINGS).open();
            case SLOT_BOOTS -> new CategoryGui(plugin, player, ToolCategory.BOOTS).open();
            case SLOT_ELYTRA -> new CategoryGui(plugin, player, ToolCategory.ELYTRA).open();
            case SLOT_MY_MODS -> new MyModsGui(plugin, player).open();
            case SLOT_CLOSE -> player.closeInventory();
            default -> { return true; }
        }
        return true;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

