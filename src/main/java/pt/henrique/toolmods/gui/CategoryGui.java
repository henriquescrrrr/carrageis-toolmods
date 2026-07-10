package pt.henrique.toolmods.gui;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.economy.EconomyManager;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import pt.henrique.toolmods.mod.ToolCategory;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Category sub-menu — shows all mods in a tool category with buy/status.
 * <p>
 * Layout (45 slots / 5 rows):
 * <pre>
 * Row 0: [FILLER x4] [CATEGORY_ICON] [FILLER x4]
 * Row 1: mods centered
 * Row 2: overflow mods if needed
 * Row 3: [FILLER x9]
 * Row 4: [BACK] [FILLER x7] [CLOSE]
 * </pre>
 */
public class CategoryGui implements InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final int SIZE = 45;

    private static final int SLOT_CATEGORY_ICON = 4;
    private static final int SLOT_BACK = 36;
    private static final int SLOT_CLOSE = 44;

    /** PDC key used to tag mod items in the GUI for identification. */
    static final NamespacedKey GUI_MOD_KEY = new NamespacedKey("toolmods", "gui_mod_key");

    /** Tracks players with an in-flight async purchase to prevent double-clicks. */
    private static final Set<UUID> pendingPurchases = Collections.synchronizedSet(new HashSet<>());

    private final ToolMods plugin;
    private final Player player;
    private final ToolCategory category;
    private Inventory inventory;

    public CategoryGui(ToolMods plugin, Player player, ToolCategory category) {
        this.plugin = plugin;
        this.player = player;
        this.category = category;
    }

    public void open() {
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        String categoryName = plugin.getLangManager().getRaw(lang, category.langKey());
        String title = plugin.getLangManager().getRaw(lang, "shop.category-title")
                .replace("{category}", categoryName);
        inventory = Bukkit.createInventory(this, SIZE, MINI.deserialize(title));

        buildGui(lang);
        player.openInventory(inventory);
    }

    /**
     * Rebuilds the GUI contents (called on open and after a purchase).
     */
    void buildGui(String lang) {
        // Fill everything
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Category icon in the top center
        Material iconMat = getCategoryIconMaterial();
        inventory.setItem(SLOT_CATEGORY_ICON, new ItemBuilder(iconMat)
                .name(plugin.getLangManager().getRaw(lang, category.langKey()))
                .hideFlags()
                .build());

        // Mod items — rows 1 and 2 (slots 9–26)
        List<ModType> mods = ModType.byCategory(category);
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Center mods in row 1 (slots 9-17)
        int[] row1Slots = centerSlots(9, 9, mods.size() > 9 ? 9 : mods.size());
        int[] row2Slots = mods.size() > 9 ? centerSlots(18, 9, mods.size() - 9) : new int[0];

        for (int i = 0; i < mods.size(); i++) {
            ModType mod = mods.get(i);
            int slot;
            if (i < row1Slots.length) {
                slot = row1Slots[i];
            } else if (i - 9 < row2Slots.length) {
                slot = row2Slots[i - 9];
            } else {
                break; // no room
            }
            inventory.setItem(slot, buildModItem(lang, mod, heldItem));
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
     * Builds a single mod item for display in the category GUI.
     */
    private ItemStack buildModItem(String lang, ModType mod, ItemStack heldItem) {
        double price = plugin.getConfigManager().getModPrice(mod);
        String formattedPrice = plugin.getConfigManager().formatCurrency(price);
        String modName = plugin.getLangManager().getRaw(lang, mod.getNameLangPath());
        boolean serverEnabled = plugin.getConfigManager().isModEnabled(mod);
        boolean alreadyOwned = ModUtils.hasMod(heldItem, mod);
        boolean hasPrerequisite = true;

        ModType prereq = mod.getPrerequisite();
        if (prereq != null) {
            hasPrerequisite = ModUtils.hasMod(heldItem, prereq);
        }

        // Build lore
        List<String> lore = new ArrayList<>();
        // Description lines
        List<String> descLines = plugin.getLangManager().getRawList(lang, mod.getDescriptionLangPath());
        lore.addAll(descLines);
        lore.add("");
        // Price line
        lore.add(plugin.getLangManager().getRaw(lang, "gui.status.price").replace("{price}", formattedPrice));
        lore.add("");

        // Status line
        if (!serverEnabled) {
            lore.add(plugin.getLangManager().getRaw(lang, "gui.status.disabled"));
        } else if (alreadyOwned) {
            lore.add(plugin.getLangManager().getRaw(lang, "gui.status.owned"));
        } else if (!hasPrerequisite) {
            String prereqName = plugin.getLangManager().getRaw(lang, prereq.getNameLangPath());
            lore.add(plugin.getLangManager().getRaw(lang, "gui.status.locked")
                    .replace("{prerequisite}", prereqName));
        } else {
            lore.add(plugin.getLangManager().getRaw(lang, "gui.status.click-to-buy"));
        }

        // Choose material glow
        ItemBuilder builder = new ItemBuilder(mod.getIcon())
                .name(modName)
                .lore(lore)
                .hideFlags()
                .pdc(GUI_MOD_KEY, mod.getKey());

        if (alreadyOwned) {
            builder.glow();
        }

        return builder.build();
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

        // Check if a mod item was clicked
        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || !clicked.hasItemMeta()) return;

        String modKey = clicked.getItemMeta().getPersistentDataContainer()
                .get(GUI_MOD_KEY, PersistentDataType.STRING);
        if (modKey == null) return;

        ModType mod = ModType.byKey(modKey);
        if (mod == null) return;

        // Open confirmation GUI instead of buying directly
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        boolean serverEnabled = plugin.getConfigManager().isModEnabled(mod);
        boolean alreadyOwned = ModUtils.hasMod(heldItem, mod);

        // Only open confirmation for buyable mods
        if (!serverEnabled || alreadyOwned) return;

        ModType prereq = mod.getPrerequisite();
        if (prereq != null && !ModUtils.hasMod(heldItem, prereq)) return;

        new ConfirmationGui(plugin, player, mod, category).open();
    }

    /**
     * Executes a purchase (called from ConfirmationGui after confirmation).
     */
    public void executePurchase(ModType mod) {
        attemptPurchase(mod);
    }

    /**
     * Attempts to purchase a mod for the player's held item.
     * Protected against double-clicks via {@link #pendingPurchases}.
     */
    private void attemptPurchase(ModType mod) {
        UUID uuid = player.getUniqueId();
        String lang = plugin.getLangManager().getPlayerLanguage(player);
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        String modName = plugin.getLangManager().getRaw(lang, mod.getNameLangPath());

        // Double-click guard
        if (pendingPurchases.contains(uuid)) return;

        // Check: mod enabled on server
        if (!plugin.getConfigManager().isModEnabled(mod)) {
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-disabled"));
            SoundUtil.error(player);
            return;
        }

        // Check: correct tool type
        if (!ModUtils.isApplicableTool(heldItem, mod)) {
            String toolName = plugin.getLangManager().getRaw(lang, mod.getCategory().langKey());
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-tool",
                    Map.of("tool", toolName)));
            SoundUtil.error(player);
            return;
        }

        // Check: already has mod
        if (ModUtils.hasMod(heldItem, mod)) {
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-already",
                    Map.of("mod", modName)));
            SoundUtil.error(player);
            return;
        }

        // Check: prerequisite
        ModType prereq = mod.getPrerequisite();
        if (prereq != null && !ModUtils.hasMod(heldItem, prereq)) {
            String prereqName = plugin.getLangManager().getRaw(lang, prereq.getNameLangPath());
            player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-prerequisite",
                    Map.of("prerequisite", prereqName)));
            SoundUtil.error(player);
            return;
        }

        // Economy check + withdraw (async)
        double price = plugin.getConfigManager().getModPrice(mod);
        long priceCents = EconomyManager.toCents(price);
        String formattedPrice = plugin.getConfigManager().formatCurrency(price);

        // Lock player and close GUI to prevent double-clicks while waiting
        pendingPurchases.add(uuid);
        player.closeInventory();

        plugin.getEconomyManager().withdrawCents(player.getUniqueId(), priceCents,
                "ToolMods: Purchase " + mod.getKey()
        ).thenAccept(success -> {
            // Back to main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                pendingPurchases.remove(uuid);
                if (!player.isOnline()) return;

                if (!success) {
                    player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-money",
                            Map.of("price", formattedPrice)));
                    SoundUtil.error(player);
                    return;
                }

                // Re-get held item (may have changed during async)
                ItemStack currentHeld = player.getInventory().getItemInMainHand();
                if (!ModUtils.isApplicableTool(currentHeld, mod) || ModUtils.hasMod(currentHeld, mod)) {
                    // Refund — money was already withdrawn, so the deposit MUST be
                    // verified. If it fails the player is short the price with no mod;
                    // log it so the loss is detectable/recoverable (never swallow it).
                    plugin.getEconomyManager().depositCents(player.getUniqueId(), priceCents,
                            "ToolMods: Refund " + mod.getKey()
                    ).thenAccept(refunded -> {
                        if (!Boolean.TRUE.equals(refunded)) {
                            plugin.getLogger().warning("Refund FAILED for " + uuid + " (" + priceCents
                                    + " cents, mod=" + mod.getKey() + ") — player charged without receiving the mod.");
                        }
                    });
                    player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-tool",
                            Map.of("tool", plugin.getLangManager().getRaw(lang, mod.getCategory().langKey()))));
                    SoundUtil.error(player);
                    return;
                }

                // Apply the mod
                ModUtils.addMod(currentHeld, mod);

                // Success message + sound
                player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-success",
                        Map.of("mod", modName, "price", formattedPrice)));
                SoundUtil.purchase(player);

                // Reopen category GUI to reflect new state
                new CategoryGui(plugin, player, category).open();
            });
        }).exceptionally(e -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                pendingPurchases.remove(uuid);
                if (player.isOnline()) {
                    player.sendMessage(plugin.getLangManager().getPrefixed(player, "shop.purchase-fail-disabled"));
                    SoundUtil.error(player);
                }
            });
            plugin.getLogger().warning("Economy error during purchase: " + e.getMessage());
            return null;
        });
    }

    // ==================== Helpers ====================

    /**
     * Returns the centered slot indices for placing items in a row.
     */
    private int[] centerSlots(int rowStart, int rowSize, int count) {
        if (count <= 0) return new int[0];
        int[] slots = new int[count];
        int offset = (rowSize - count) / 2;
        for (int i = 0; i < count; i++) {
            slots[i] = rowStart + offset + i;
        }
        return slots;
    }

    private Material getCategoryIconMaterial() {
        return switch (category) {
            case PICKAXE -> Material.DIAMOND_PICKAXE;
            case AXE -> Material.DIAMOND_AXE;
            case SHOVEL -> Material.DIAMOND_SHOVEL;
            case HOE -> Material.DIAMOND_HOE;
            case SWORD -> Material.DIAMOND_SWORD;
            case SPEAR -> Material.DIAMOND_SPEAR;
            case SHIELD -> Material.SHIELD;
            case MACE -> Material.MACE;
            case BOW -> Material.BOW;
            case HELMET -> Material.DIAMOND_HELMET;
            case CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
            case LEGGINGS -> Material.DIAMOND_LEGGINGS;
            case BOOTS -> Material.DIAMOND_BOOTS;
            case ELYTRA -> Material.ELYTRA;
            case UNIVERSAL -> Material.NETHER_STAR;
        };
    }

    public Player getPlayer() {
        return player;
    }

    public ToolCategory getCategory() {
        return category;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}

