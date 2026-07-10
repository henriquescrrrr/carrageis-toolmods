package pt.henrique.toolmods;

import pt.henrique.toolmods.command.ModsCommand;
import pt.henrique.toolmods.config.ConfigManager;
import pt.henrique.toolmods.config.LangManager;
import pt.henrique.toolmods.economy.EconomyManager;
import pt.henrique.toolmods.gui.GuiListener;
import pt.henrique.toolmods.hook.AntiXrayHook;
import pt.henrique.toolmods.hook.BuildersDreamHook;
import pt.henrique.toolmods.hook.LandClaimHook;
import pt.henrique.toolmods.hook.PlayerSettingsHook;
import pt.henrique.toolmods.listener.*;
import pt.henrique.toolmods.util.CooldownManager;
import pt.henrique.toolmods.util.SpearChargeTracker;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ToolMods — Server-side tool modifications plugin for Paper 1.21.11.
 * <p>
 * Players can buy permanent modifications for their tools/weapons via a GUI shop.
 * Mods are applied to the item the player holds and stored in the item's
 * PersistentDataContainer (PDC). Each mod can be toggled on/off.
 */
public final class ToolMods extends JavaPlugin {

    private static ToolMods instance;

    private ConfigManager configManager;
    private LangManager langManager;
    private EconomyManager economyManager;
    private CooldownManager cooldownManager;
    private SpearChargeTracker spearChargeTracker;
    private SoulboundListener soulboundListener;
    private AerodynamicListener aerodynamicListener;
    private PhantomCloakListener phantomCloakListener;
    private SonicBoomListener sonicBoomListener;
    private SafeLandingListener safeLandingListener;

    @Override
    public void onEnable() {
        instance = this;

        // Load configuration
        configManager = new ConfigManager(this);

        // Initialize optional hooks (before LangManager so it can use PlayerSettingsHook)
        PlayerSettingsHook.init(getLogger());
        PlayerSettingsHook.registerSettings();
        LandClaimHook.init(getLogger());
        AntiXrayHook.init(getLogger());
        BuildersDreamHook.init(getLogger());

        langManager = new LangManager(this);

        // Initialize economy
        economyManager = new EconomyManager(this);
        if (!economyManager.setupEconomy()) {
            getLogger().severe("Disabling ToolMods due to missing economy provider.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands (Brigadier)
        ModsCommand.register(this);

        // Initialize CooldownManager
        cooldownManager = new CooldownManager(this);
        cooldownManager.startActionBarTask();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        // Pickaxe mods
        getServer().getPluginManager().registerEvents(new VeinMinerListener(this), this);
        getServer().getPluginManager().registerEvents(new AreaMineListener(this), this);
        // Axe mods
        getServer().getPluginManager().registerEvents(new TreeFellerListener(this), this);
        // Shovel mods
        getServer().getPluginManager().registerEvents(new ExcavatorListener(this), this);
        getServer().getPluginManager().registerEvents(new PathMakerListener(this), this);
        // Hoe mods
        getServer().getPluginManager().registerEvents(new HarvesterListener(this), this);
        getServer().getPluginManager().registerEvents(new TillerListener(this), this);
        // Sword mods
        getServer().getPluginManager().registerEvents(new LifestealListener(this), this);
        getServer().getPluginManager().registerEvents(new ThunderstrikeListener(this), this);
        getServer().getPluginManager().registerEvents(new DecapitatorListener(this), this);
        getServer().getPluginManager().registerEvents(new EvokerFangsListener(this), this);
        // Spear mods
        spearChargeTracker = new SpearChargeTracker(this);
        getServer().getPluginManager().registerEvents(spearChargeTracker, this);
        getServer().getPluginManager().registerEvents(new CrippleListener(this), this);
        getServer().getPluginManager().registerEvents(new BleedListener(this), this);
        getServer().getPluginManager().registerEvents(new DashImpactListener(this), this);
        getServer().getPluginManager().registerEvents(new SpearSweepListener(this), this);
        getServer().getPluginManager().registerEvents(new MomentumListener(this), this);
        getServer().getPluginManager().registerEvents(new SpearEvokerFangsListener(this), this);
        // Trident mods (Spear category)
        getServer().getPluginManager().registerEvents(new RiptideListener(this), this);
        getServer().getPluginManager().registerEvents(new LoyaltyPlusListener(this), this);
        getServer().getPluginManager().registerEvents(new ChainLightningListener(this), this);
        getServer().getPluginManager().registerEvents(new AnchorListener(this), this);
        getServer().getPluginManager().registerEvents(new PoseidonsCallListener(this), this);
        // Shield mods
        getServer().getPluginManager().registerEvents(new MagneticShieldListener(this), this);
        getServer().getPluginManager().registerEvents(new ShieldBashListener(this), this);
        getServer().getPluginManager().registerEvents(new PhalanxListener(this), this);
        getServer().getPluginManager().registerEvents(new ReflectiveShieldListener(this), this);
        // Mace mods
        getServer().getPluginManager().registerEvents(new TremorListener(this), this);
        getServer().getPluginManager().registerEvents(new SeismicSlamListener(this), this);
        getServer().getPluginManager().registerEvents(new GravitonPulseListener(this), this);
        getServer().getPluginManager().registerEvents(new MeteorStrikeListener(this), this);
        // Bow / Crossbow mods
        getServer().getPluginManager().registerEvents(new HomingArrowListener(this), this);
        getServer().getPluginManager().registerEvents(new ExplosiveArrowListener(this), this);
        // Helmet mods
        getServer().getPluginManager().registerEvents(new NightOwlListener(this), this);
        getServer().getPluginManager().registerEvents(new AquaLungListener(this), this);
        getServer().getPluginManager().registerEvents(new HuntersSightListener(this), this);
        // Chestplate mods
        getServer().getPluginManager().registerEvents(new VitalityListener(this), this);
        getServer().getPluginManager().registerEvents(new LastStandListener(this), this);
        getServer().getPluginManager().registerEvents(new ThornsAuraListener(this), this);
        // Leggings mods
        getServer().getPluginManager().registerEvents(new SwiftListener(this), this);
        getServer().getPluginManager().registerEvents(new DodgerListener(this), this);
        // Boots mods
        getServer().getPluginManager().registerEvents(new FeatherweightListener(this), this);
        getServer().getPluginManager().registerEvents(new DoubleJumpListener(this), this);
        getServer().getPluginManager().registerEvents(new FrostWalkerPlusListener(this), this);
        // Elytra mods
        getServer().getPluginManager().registerEvents(new FireworkEfficiencyListener(this), this);
        safeLandingListener = new SafeLandingListener(this);
        getServer().getPluginManager().registerEvents(safeLandingListener, this);
        aerodynamicListener = new AerodynamicListener(this);
        getServer().getPluginManager().registerEvents(new BoostListener(this), this);
        phantomCloakListener = new PhantomCloakListener(this);
        getServer().getPluginManager().registerEvents(phantomCloakListener, this);
        sonicBoomListener = new SonicBoomListener(this);
        // Universal mods
        getServer().getPluginManager().registerEvents(new UnbreakableListener(this), this);
        getServer().getPluginManager().registerEvents(new ExperiencedListener(this), this);
        soulboundListener = new SoulboundListener(this);
        getServer().getPluginManager().registerEvents(soulboundListener, this);

        getLogger().info("ToolMods v" + getPluginMeta().getVersion() + " enabled!");
        getLogger().info("Economy provider: " + economyManager.getProviderName());
    }

    @Override
    public void onDisable() {
        PlayerSettingsHook.unregisterSettings();
        if (cooldownManager != null) {
            cooldownManager.shutdown();
        }
        if (soulboundListener != null) {
            soulboundListener.shutdown();
        }
        if (aerodynamicListener != null) {
            aerodynamicListener.shutdown();
        }
        if (phantomCloakListener != null) {
            phantomCloakListener.shutdown();
        }
        if (sonicBoomListener != null) {
            sonicBoomListener.shutdown();
        }
        if (safeLandingListener != null) {
            safeLandingListener.shutdown();
        }
        getLogger().info("ToolMods disabled.");
        instance = null;
    }

    /**
     * Reloads all configuration and language files.
     */
    public void reloadPlugin() {
        configManager.reload();
        langManager.reload();
        getLogger().info("ToolMods configuration reloaded.");
    }

    // ==================== GETTERS ====================

    public static ToolMods getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LangManager getLangManager() {
        return langManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public SpearChargeTracker getSpearChargeTracker() {
        return spearChargeTracker;
    }
}
