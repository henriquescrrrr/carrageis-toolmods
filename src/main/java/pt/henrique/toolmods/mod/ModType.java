package pt.henrique.toolmods.mod;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of every mod in the plugin.
 * <p>
 * Each entry knows its PDC key, default price, tool category, prerequisite,
 * config path, lang key, and GUI icon material.
 */
public enum ModType {

    // ===== Pickaxe Mods =====
    AREA_MINE_3X3("mod_area_mine_3x3", 15_000, ToolCategory.PICKAXE, null,
            "mods.pickaxe.area-mine-3x3", "area-mine-3x3", Material.IRON_PICKAXE),

    AREA_MINE_5X5("mod_area_mine_5x5", 50_000, ToolCategory.PICKAXE, "mod_area_mine_3x3",
            "mods.pickaxe.area-mine-5x5", "area-mine-5x5", Material.DIAMOND_PICKAXE),

    AUTO_SMELT("mod_auto_smelt", 8_000, ToolCategory.PICKAXE, null,
            "mods.pickaxe.auto-smelt", "auto-smelt", Material.FURNACE),

    TELEPATHY("mod_telepathy", 12_000, ToolCategory.PICKAXE, null,
            "mods.pickaxe.telepathy", "telepathy", Material.ENDER_PEARL),

    VEIN_MINER("mod_vein_miner", 1_600, ToolCategory.PICKAXE, null,
            "mods.pickaxe.vein-miner", "vein-miner", Material.DIAMOND_ORE),

    // ===== Axe Mods =====
    TREE_FELLER("mod_tree_feller", 800, ToolCategory.AXE, null,
            "mods.axe.tree-feller", "tree-feller", Material.OAK_LOG),

    // ===== Shovel Mods =====
    EXCAVATOR_3X3("mod_excavator_3x3", 12_000, ToolCategory.SHOVEL, null,
            "mods.shovel.excavator-3x3", "excavator-3x3", Material.IRON_SHOVEL),

    EXCAVATOR_5X5("mod_excavator_5x5", 40_000, ToolCategory.SHOVEL, "mod_excavator_3x3",
            "mods.shovel.excavator-5x5", "excavator-5x5", Material.DIAMOND_SHOVEL),

    PATH_MAKER("mod_path_maker", 5_000, ToolCategory.SHOVEL, null,
            "mods.shovel.path-maker", "path-maker", Material.DIRT_PATH),

    // ===== Hoe Mods =====
    HARVESTER_3X3("mod_harvester_3x3", 10_000, ToolCategory.HOE, null,
            "mods.hoe.harvester-3x3", "harvester-3x3", Material.IRON_HOE),

    HARVESTER_5X5("mod_harvester_5x5", 35_000, ToolCategory.HOE, "mod_harvester_3x3",
            "mods.hoe.harvester-5x5", "harvester-5x5", Material.DIAMOND_HOE),

    TILLER("mod_tiller", 4_000, ToolCategory.HOE, null,
            "mods.hoe.tiller", "tiller", Material.FARMLAND),

    // ===== Sword Mods =====
    LIFESTEAL("mod_lifesteal", 30_000, ToolCategory.SWORD, null,
            "mods.sword.lifesteal", "lifesteal", Material.GOLDEN_APPLE),

    THUNDERSTRIKE("mod_thunderstrike", 30_000, ToolCategory.SWORD, null,
            "mods.sword.thunderstrike", "thunderstrike", Material.LIGHTNING_ROD),

    DECAPITATOR("mod_decapitator", 15_000, ToolCategory.SWORD, null,
            "mods.sword.decapitator", "decapitator", Material.WITHER_SKELETON_SKULL),

    // ===== Bow / Crossbow Mods =====
    HOMING_ARROW("mod_homing_arrow", 25_000, ToolCategory.BOW, null,
            "mods.bow.homing-arrow", "homing-arrow", Material.SPECTRAL_ARROW),

    EXPLOSIVE_ARROW("mod_explosive_arrow", 18_000, ToolCategory.BOW, null,
            "mods.bow.explosive-arrow", "explosive-arrow", Material.TNT),

    // ===== Universal Mods =====
    SOULBOUND("mod_soulbound", 100_000, ToolCategory.UNIVERSAL, null,
            "mods.universal.soulbound", "soulbound", Material.TOTEM_OF_UNDYING),

    EXPERIENCED("mod_experienced", 6_000, ToolCategory.UNIVERSAL, null,
            "mods.universal.experienced", "experienced", Material.EXPERIENCE_BOTTLE),

    UNBREAKABLE("mod_unbreakable", 500_000, ToolCategory.UNIVERSAL, null,
            "mods.universal.unbreakable", "unbreakable", Material.NETHERITE_INGOT),

    // ===== Sword Mods (new v1.1) =====
    EVOKER_FANGS("mod_evoker_fangs", 25_000, ToolCategory.SWORD, null,
            "mods.sword.evoker-fangs", "evoker-fangs", Material.EVOKER_SPAWN_EGG),

    // ===== Spear Mods =====
    CRIPPLE("mod_cripple", 6_000, ToolCategory.SPEAR, null,
            "mods.spear.cripple", "cripple", Material.COBWEB),

    BLEED("mod_bleed", 8_000, ToolCategory.SPEAR, null,
            "mods.spear.bleed", "bleed", Material.RED_DYE),

    DASH_IMPACT("mod_dash_impact", 10_000, ToolCategory.SPEAR, null,
            "mods.spear.dash-impact", "dash-impact", Material.FIREWORK_STAR),

    SPEAR_SWEEP("mod_spear_sweep", 12_000, ToolCategory.SPEAR, null,
            "mods.spear.spear-sweep", "spear-sweep", Material.DIAMOND_SPEAR),

    MOMENTUM("mod_momentum", 15_000, ToolCategory.SPEAR, null,
            "mods.spear.momentum", "momentum", Material.FEATHER),

    SPEAR_EVOKER_FANGS("mod_spear_evoker_fangs", 25_000, ToolCategory.SPEAR, null,
            "mods.spear.evoker-fangs", "spear-evoker-fangs", Material.EVOKER_SPAWN_EGG),

    RIPTIDE("mod_riptide", 20_000, ToolCategory.SPEAR, null,
            "mods.spear.riptide", "riptide", Material.HEART_OF_THE_SEA),

    LOYALTY_PLUS("mod_loyalty_plus", 15_000, ToolCategory.SPEAR, null,
            "mods.spear.loyalty-plus", "loyalty-plus", Material.TRIDENT),

    CHAIN_LIGHTNING("mod_chain_lightning", 25_000, ToolCategory.SPEAR, null,
            "mods.spear.chain-lightning", "chain-lightning", Material.LIGHTNING_ROD),

    ANCHOR("mod_anchor", 18_000, ToolCategory.SPEAR, null,
            "mods.spear.anchor", "anchor", Material.ANVIL),

    POSEIDONS_CALL("mod_poseidons_call", 30_000, ToolCategory.SPEAR, null,
            "mods.spear.poseidons-call", "poseidons-call", Material.CONDUIT),

    // ===== Shield Mods =====
    MAGNETIC_SHIELD("mod_magnetic_shield", 8_000, ToolCategory.SHIELD, null,
            "mods.shield.magnetic-shield", "magnetic-shield", Material.IRON_INGOT),

    SHIELD_BASH("mod_shield_bash", 10_000, ToolCategory.SHIELD, null,
            "mods.shield.shield-bash", "shield-bash", Material.SHIELD),

    PHALANX("mod_phalanx", 12_000, ToolCategory.SHIELD, null,
            "mods.shield.phalanx", "phalanx", Material.IRON_CHESTPLATE),

    REFLECTIVE_SHIELD("mod_reflective_shield", 18_000, ToolCategory.SHIELD, null,
            "mods.shield.reflective-shield", "reflective-shield", Material.GLASS),

    // ===== Mace Mods =====
    TREMOR("mod_tremor", 20_000, ToolCategory.MACE, null,
            "mods.mace.tremor", "tremor", Material.SCULK_SENSOR),

    SEISMIC_SLAM("mod_seismic_slam", 35_000, ToolCategory.MACE, null,
            "mods.mace.seismic-slam", "seismic-slam", Material.HEAVY_CORE),

    GRAVITON_PULSE("mod_graviton_pulse", 40_000, ToolCategory.MACE, null,
            "mods.mace.graviton-pulse", "graviton-pulse", Material.ENDER_EYE),

    METEOR_STRIKE("mod_meteor_strike", 50_000, ToolCategory.MACE, null,
            "mods.mace.meteor-strike", "meteor-strike", Material.FIRE_CHARGE),

    // ===== Helmet Mods =====
    NIGHT_OWL("mod_night_owl", 30_000, ToolCategory.HELMET, null,
            "mods.armor.helmet.night-owl", "night-owl", Material.ENDER_EYE),

    AQUA_LUNG("mod_aqua_lung", 25_000, ToolCategory.HELMET, null,
            "mods.armor.helmet.aqua-lung", "aqua-lung", Material.CONDUIT),

    HUNTERS_SIGHT("mod_hunters_sight", 20_000, ToolCategory.HELMET, null,
            "mods.armor.helmet.hunters-sight", "hunters-sight", Material.SPYGLASS),

    // ===== Chestplate Mods =====
    VITALITY("mod_vitality", 30_000, ToolCategory.CHESTPLATE, null,
            "mods.armor.chestplate.vitality", "vitality", Material.GLISTERING_MELON_SLICE),

    LAST_STAND("mod_last_stand", 30_000, ToolCategory.CHESTPLATE, null,
            "mods.armor.chestplate.last-stand", "last-stand", Material.ENCHANTED_GOLDEN_APPLE),

    THORNS_AURA("mod_thorns_aura", 18_000, ToolCategory.CHESTPLATE, null,
            "mods.armor.chestplate.thorns-aura", "thorns-aura", Material.CACTUS),

    // ===== Leggings Mods =====
    SWIFT("mod_swift", 20_000, ToolCategory.LEGGINGS, null,
            "mods.armor.leggings.swift", "swift", Material.SUGAR),

    DODGER("mod_dodger", 35_000, ToolCategory.LEGGINGS, null,
            "mods.armor.leggings.dodger", "dodger", Material.PHANTOM_MEMBRANE),

    // ===== Boots Mods =====
    FEATHERWEIGHT("mod_featherweight", 10_000, ToolCategory.BOOTS, null,
            "mods.armor.boots.featherweight", "featherweight", Material.FEATHER),

    DOUBLE_JUMP("mod_double_jump", 55_000, ToolCategory.BOOTS, null,
            "mods.armor.boots.double-jump", "double-jump", Material.RABBIT_FOOT),

    FROST_WALKER_PLUS("mod_frost_walker_plus", 20_000, ToolCategory.BOOTS, null,
            "mods.armor.boots.frost-walker-plus", "frost-walker-plus", Material.BLUE_ICE),

    // ===== Elytra Mods =====
    FIREWORK_EFFICIENCY("mod_firework_efficiency", 8_000, ToolCategory.ELYTRA, null,
            "mods.elytra.firework-efficiency", "firework-efficiency", Material.FIREWORK_ROCKET),

    SAFE_LANDING("mod_safe_landing", 12_000, ToolCategory.ELYTRA, null,
            "mods.elytra.safe-landing", "safe-landing", Material.HAY_BLOCK),

    AERODYNAMIC("mod_aerodynamic", 15_000, ToolCategory.ELYTRA, null,
            "mods.elytra.aerodynamic", "aerodynamic", Material.PHANTOM_MEMBRANE),

    BOOST("mod_boost", 20_000, ToolCategory.ELYTRA, null,
            "mods.elytra.boost", "boost", Material.FIREWORK_STAR),

    PHANTOM_CLOAK("mod_phantom_cloak", 25_000, ToolCategory.ELYTRA, null,
            "mods.elytra.phantom-cloak", "phantom-cloak", Material.GLASS),

    SONIC_BOOM("mod_sonic_boom", 35_000, ToolCategory.ELYTRA, null,
            "mods.elytra.sonic-boom", "sonic-boom", Material.ECHO_SHARD);

    // ===== Static Lookup =====

    private static final Map<String, ModType> BY_KEY = new HashMap<>();

    static {
        for (ModType mod : values()) {
            BY_KEY.put(mod.key, mod);
        }
    }

    // ===== Fields =====

    private final String key;               // PDC key suffix, e.g. "mod_area_mine_3x3"
    private final double defaultPrice;       // default price in currency units
    private final ToolCategory category;     // tool category
    private final String prerequisiteKey;    // PDC key of prerequisite mod, or null
    private final String configPath;         // config path, e.g. "mods.pickaxe.area-mine-3x3"
    private final String langKey;            // lang key suffix, e.g. "area-mine-3x3"
    private final Material icon;             // GUI icon material

    ModType(String key, double defaultPrice, ToolCategory category, String prerequisiteKey,
            String configPath, String langKey, Material icon) {
        this.key = key;
        this.defaultPrice = defaultPrice;
        this.category = category;
        this.prerequisiteKey = prerequisiteKey;
        this.configPath = configPath;
        this.langKey = langKey;
        this.icon = icon;
    }

    // ===== Getters =====

    public String getKey() {
        return key;
    }

    /**
     * Returns the PDC key for the disabled toggle: key + "_disabled".
     */
    public String getDisabledKey() {
        return key + "_disabled";
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public ToolCategory getCategory() {
        return category;
    }

    /**
     * Returns the prerequisite ModType, or null if none.
     */
    public ModType getPrerequisite() {
        if (prerequisiteKey == null) return null;
        return BY_KEY.get(prerequisiteKey);
    }

    public String getPrerequisiteKey() {
        return prerequisiteKey;
    }

    public String getConfigPath() {
        return configPath;
    }

    public String getLangKey() {
        return langKey;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Returns the full lang path for this mod's display name, e.g. "mod.area-mine-3x3.name".
     */
    public String getNameLangPath() {
        return "mod." + langKey + ".name";
    }

    /**
     * Returns the full lang path for this mod's description, e.g. "mod.area-mine-3x3.description".
     */
    public String getDescriptionLangPath() {
        return "mod." + langKey + ".description";
    }

    // ===== Static Helpers =====

    /**
     * Looks up a ModType by its PDC key.
     */
    public static ModType byKey(String key) {
        return BY_KEY.get(key);
    }

    /**
     * Returns all mods for a given tool category.
     */
    public static java.util.List<ModType> byCategory(ToolCategory category) {
        java.util.List<ModType> result = new java.util.ArrayList<>();
        for (ModType mod : values()) {
            if (mod.category == category) {
                result.add(mod);
            }
        }
        return result;
    }
}

