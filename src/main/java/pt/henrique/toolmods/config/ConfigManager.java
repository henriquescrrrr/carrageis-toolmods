package pt.henrique.toolmods.config;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Reads and caches values from config.yml.
 */
public class ConfigManager {

    private final ToolMods plugin;

    public ConfigManager(ToolMods plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    // ========================
    // General
    // ========================

    public String getLanguage() {
        return plugin.getConfig().getString("language", "en_US");
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    // ========================
    // Economy
    // ========================

    public String getEconomyMode() {
        return plugin.getConfig().getString("economy.mode", "auto").toLowerCase();
    }

    public List<String> getEconomyPriority() {
        List<String> list = plugin.getConfig().getStringList("economy.priority");
        return list.isEmpty() ? List.of("MultiBank", "Vault") : list;
    }

    public String getCurrencySymbol() {
        return plugin.getConfig().getString("economy.currency-symbol", "$");
    }

    public String getCurrencyFormat() {
        return plugin.getConfig().getString("economy.currency-format", "#,##0.00");
    }

    // ========================
    // Mod Prices & Settings
    // ========================

    /**
     * Whether a mod is enabled in config (server-wide toggle).
     */
    public boolean isModEnabled(ModType mod) {
        return plugin.getConfig().getBoolean(mod.getConfigPath() + ".enabled", true);
    }

    /**
     * Gets the price for a mod from config (in currency units, e.g. 15000.0).
     * <p>
     * A misconfigured negative price is clamped to 0 and a non-finite price
     * (NaN/Infinity) falls back to the mod's default: a negative/zero price would
     * otherwise let {@code withdrawCents} short-circuit and grant the mod without
     * checking the player's balance. Price validation lives here so every caller
     * (shop, confirmation, sell-bonus) is protected in one place.
     */
    public double getModPrice(ModType mod) {
        double price = plugin.getConfig().getDouble(mod.getConfigPath() + ".price", mod.getDefaultPrice());
        if (!Double.isFinite(price)) {
            plugin.getLogger().warning("Non-finite price for " + mod.getConfigPath()
                    + " — using default " + mod.getDefaultPrice());
            return mod.getDefaultPrice();
        }
        if (price < 0) {
            plugin.getLogger().warning("Negative price for " + mod.getConfigPath()
                    + " (" + price + ") — clamping to 0.");
            return 0;
        }
        return price;
    }

    /**
     * Gets the mod sell bonus multiplier for Server Shop integration.
     */
    public double getModSellBonusMultiplier() {
        double multiplier = plugin.getConfig().getDouble("mods.mod-sell-bonus-multiplier", 0.10);
        // A negative/non-finite multiplier would feed a negative sell bonus to the
        // Server Shop hook (calculateModSellBonus). Clamp to a sane, non-negative value.
        if (!Double.isFinite(multiplier) || multiplier < 0) {
            return 0.10;
        }
        return multiplier;
    }

    // ========================
    // Mod-specific settings
    // ========================

    public int getVeinMinerMaxBlocks() {
        return plugin.getConfig().getInt("mods.pickaxe.vein-miner.max-blocks", 64);
    }

    public int getTreeFellerMaxBlocks() {
        return plugin.getConfig().getInt("mods.axe.tree-feller.max-blocks", 128);
    }

    public int getTreeFellerBlocksPerTick() {
        return plugin.getConfig().getInt("mods.axe.tree-feller.blocks-per-tick", 10);
    }

    public double getLifestealHealPercent() {
        return plugin.getConfig().getDouble("mods.sword.lifesteal.heal-percent", 0.15);
    }

    public double getLifestealMaxHealPerSecond() {
        return plugin.getConfig().getDouble("mods.sword.lifesteal.max-heal-per-second", 4.0);
    }

    public int getThunderstrikeHitsRequired() {
        return plugin.getConfig().getInt("mods.sword.thunderstrike.hits-required", 7);
    }

    public double getThunderstrikeExtraDamage() {
        return plugin.getConfig().getDouble("mods.sword.thunderstrike.extra-damage", 4.0);
    }

    public int getThunderstrikeCooldownSeconds() {
        return plugin.getConfig().getInt("mods.sword.thunderstrike.cooldown-seconds", 10);
    }

    public double getDecapitatorMobHeadChance() {
        return plugin.getConfig().getDouble("mods.sword.decapitator.mob-head-chance", 0.05);
    }

    public double getHomingArrowTrackingRadius() {
        return plugin.getConfig().getDouble("mods.bow.homing-arrow.tracking-radius", 3.0);
    }

    public double getHomingArrowCorrectionStrength() {
        return plugin.getConfig().getDouble("mods.bow.homing-arrow.correction-strength", 0.15);
    }

    public double getHomingArrowMaxTrackingSeconds() {
        return plugin.getConfig().getDouble("mods.bow.homing-arrow.max-tracking-seconds", 3.0);
    }

    public double getExplosiveArrowRadius() {
        return plugin.getConfig().getDouble("mods.bow.explosive-arrow.explosion-radius", 2.0);
    }

    public double getExplosiveArrowCenterDamage() {
        return plugin.getConfig().getDouble("mods.bow.explosive-arrow.center-damage", 6.0);
    }

    public int getExplosiveArrowCooldownSeconds() {
        return plugin.getConfig().getInt("mods.bow.explosive-arrow.cooldown-seconds", 3);
    }

    public double getExperiencedXpMultiplier() {
        return plugin.getConfig().getDouble("mods.universal.experienced.xp-multiplier", 1.5);
    }

    // ========================
    // Evoker Fangs (Sword) settings
    // ========================

    public int getEvokerFangsHitsRequired() {
        return plugin.getConfig().getInt("mods.sword.evoker-fangs.hits-required", 5);
    }

    public int getEvokerFangsFangCount() {
        return plugin.getConfig().getInt("mods.sword.evoker-fangs.fang-count", 5);
    }

    public double getEvokerFangsFangDamage() {
        return plugin.getConfig().getDouble("mods.sword.evoker-fangs.fang-damage", 3.0);
    }

    public int getEvokerFangsCooldownSeconds() {
        return plugin.getConfig().getInt("mods.sword.evoker-fangs.cooldown-seconds", 8);
    }

    // ========================
    // Spear Mod settings
    // ========================

    // --- Cripple ---

    public int getCrippleJabSlownessLevel() {
        return plugin.getConfig().getInt("mods.spear.cripple.jab-slowness-level", 0);
    }

    public int getCrippleJabDurationSeconds() {
        return plugin.getConfig().getInt("mods.spear.cripple.jab-duration-seconds", 3);
    }

    public int getCrippleChargeSlownessLevel() {
        return plugin.getConfig().getInt("mods.spear.cripple.charge-slowness-level", 1);
    }

    public int getCrippleChargeDurationSeconds() {
        return plugin.getConfig().getInt("mods.spear.cripple.charge-duration-seconds", 4);
    }

    // --- Bleed ---

    public double getBleedDamagePerTick() {
        return plugin.getConfig().getDouble("mods.spear.bleed.damage-per-tick", 2.0);
    }

    public int getBleedTickIntervalSeconds() {
        return plugin.getConfig().getInt("mods.spear.bleed.tick-interval-seconds", 2);
    }

    public int getBleedDurationSeconds() {
        return plugin.getConfig().getInt("mods.spear.bleed.duration-seconds", 6);
    }

    // --- Dash Impact ---

    public double getDashImpactAoeRadius() {
        return plugin.getConfig().getDouble("mods.spear.dash-impact.aoe-radius", 2.0);
    }

    public double getDashImpactAoeDamagePercent() {
        return plugin.getConfig().getDouble("mods.spear.dash-impact.aoe-damage-percent", 0.40);
    }

    // --- Spear Sweep ---

    public double getSpearSweepRadius() {
        return plugin.getConfig().getDouble("mods.spear.spear-sweep.radius", 3.0);
    }

    public double getSpearSweepDamagePercent() {
        return plugin.getConfig().getDouble("mods.spear.spear-sweep.damage-percent", 0.60);
    }

    // --- Momentum ---

    public double getMomentumPercentPerBlock() {
        return plugin.getConfig().getDouble("mods.spear.momentum.percent-per-block", 0.10);
    }

    public double getMomentumMaxBonusPercent() {
        return plugin.getConfig().getDouble("mods.spear.momentum.max-bonus-percent", 1.0);
    }

    // --- Spear Evoker Fangs ---

    public int getSpearEvokerFangsFangCount() {
        return plugin.getConfig().getInt("mods.spear.evoker-fangs.fang-count", 5);
    }

    public double getSpearEvokerFangsFangDamage() {
        return plugin.getConfig().getDouble("mods.spear.evoker-fangs.fang-damage", 3.0);
    }

    public int getSpearEvokerFangsCooldownSeconds() {
        return plugin.getConfig().getInt("mods.spear.evoker-fangs.cooldown-seconds", 8);
    }

    // --- Riptide ---

    public double getRiptideLaunchPower() {
        return plugin.getConfig().getDouble("mods.spear.riptide.launch-power", 2.5);
    }

    public int getRiptideCooldownSeconds() {
        return plugin.getConfig().getInt("mods.spear.riptide.cooldown-seconds", 8);
    }

    public boolean isRiptideFreeInRain() {
        return plugin.getConfig().getBoolean("mods.spear.riptide.free-in-rain", true);
    }

    public int getRiptideFallImmunitySeconds() {
        return plugin.getConfig().getInt("mods.spear.riptide.fall-immunity-seconds", 5);
    }

    // --- Loyalty+ ---

    public double getLoyaltyPlusReturnSpeedMultiplier() {
        return plugin.getConfig().getDouble("mods.spear.loyalty-plus.return-speed-multiplier", 2.0);
    }

    public double getLoyaltyPlusReturnDamage() {
        return plugin.getConfig().getDouble("mods.spear.loyalty-plus.return-damage", 4.0);
    }

    public double getLoyaltyPlusReturnHitRadius() {
        return plugin.getConfig().getDouble("mods.spear.loyalty-plus.return-hit-radius", 1.5);
    }

    // --- Chain Lightning ---

    public int getChainLightningMaxChains() {
        return plugin.getConfig().getInt("mods.spear.chain-lightning.max-chains", 3);
    }

    public double getChainLightningRadius() {
        return plugin.getConfig().getDouble("mods.spear.chain-lightning.chain-radius", 5.0);
    }

    public double getChainLightningStartMultiplier() {
        return plugin.getConfig().getDouble("mods.spear.chain-lightning.chain-start-multiplier", 0.70);
    }

    public double getChainLightningReductionPerJump() {
        return plugin.getConfig().getDouble("mods.spear.chain-lightning.chain-reduction-per-jump", 0.70);
    }

    public int getChainLightningCooldownSeconds() {
        return plugin.getConfig().getInt("mods.spear.chain-lightning.cooldown-seconds", 6);
    }

    // --- Anchor ---

    public int getAnchorDurationSeconds() {
        return plugin.getConfig().getInt("mods.spear.anchor.duration-seconds", 2);
    }

    public int getAnchorSlownessLevel() {
        return plugin.getConfig().getInt("mods.spear.anchor.slowness-level", 4);
    }

    public int getAnchorCooldownSeconds() {
        return plugin.getConfig().getInt("mods.spear.anchor.cooldown-seconds", 10);
    }

    // --- Poseidon's Call ---

    public double getPoseidonsCallPullRadius() {
        return plugin.getConfig().getDouble("mods.spear.poseidons-call.pull-radius", 6.0);
    }

    public int getPoseidonsCallPullDurationSeconds() {
        return plugin.getConfig().getInt("mods.spear.poseidons-call.pull-duration-seconds", 3);
    }

    public double getPoseidonsCallPullStrength() {
        return plugin.getConfig().getDouble("mods.spear.poseidons-call.pull-strength", 0.3);
    }

    public int getPoseidonsCallCooldownSeconds() {
        return plugin.getConfig().getInt("mods.spear.poseidons-call.cooldown-seconds", 12);
    }

    // ========================
    // Shield Mod settings
    // ========================

    // --- Magnetic Shield ---

    public double getMagneticShieldRadius() {
        return plugin.getConfig().getDouble("mods.shield.magnetic-shield.radius", 2.0);
    }

    // --- Shield Bash ---

    public double getShieldBashDamage() {
        return plugin.getConfig().getDouble("mods.shield.shield-bash.damage", 6.0);
    }

    public double getShieldBashKnockbackStrength() {
        return plugin.getConfig().getDouble("mods.shield.shield-bash.knockback-strength", 5.0);
    }

    public int getShieldBashSlownessDurationSeconds() {
        return plugin.getConfig().getInt("mods.shield.shield-bash.slowness-duration-seconds", 2);
    }

    public int getShieldBashCooldownSeconds() {
        return plugin.getConfig().getInt("mods.shield.shield-bash.cooldown-seconds", 5);
    }

    public double getShieldBashCounterWindowSeconds() {
        return plugin.getConfig().getDouble("mods.shield.shield-bash.counter-window-seconds", 0.5);
    }

    // --- Phalanx ---

    public double getPhalanxRadius() {
        return plugin.getConfig().getDouble("mods.shield.phalanx.radius", 3.0);
    }

    public int getPhalanxResistanceLevel() {
        return plugin.getConfig().getInt("mods.shield.phalanx.resistance-level", 0);
    }

    // --- Reflective Shield ---

    public double getReflectiveShieldReflectPercent() {
        return plugin.getConfig().getDouble("mods.shield.reflective-shield.reflect-percent", 0.25);
    }

    // ========================
    // Mace Mod settings
    // ========================

    // --- Tremor ---

    public int getTremorSlownessLevel() {
        return plugin.getConfig().getInt("mods.mace.tremor.slowness-level", 1);
    }

    public int getTremorNauseaDurationSeconds() {
        return plugin.getConfig().getInt("mods.mace.tremor.nausea-duration-seconds", 3);
    }

    public int getTremorSlownessDurationSeconds() {
        return plugin.getConfig().getInt("mods.mace.tremor.slowness-duration-seconds", 3);
    }

    // --- Seismic Slam ---

    public double getSeismicSlamRadius() {
        return plugin.getConfig().getDouble("mods.mace.seismic-slam.radius", 5.0);
    }

    public double getSeismicSlamAoeDamagePercent() {
        return plugin.getConfig().getDouble("mods.mace.seismic-slam.aoe-damage-percent", 0.50);
    }

    // --- Graviton Pulse ---

    public double getGravitonPulseRadius() {
        return plugin.getConfig().getDouble("mods.mace.graviton-pulse.radius", 6.0);
    }

    public double getGravitonPulsePullDurationSeconds() {
        return plugin.getConfig().getDouble("mods.mace.graviton-pulse.pull-duration-seconds", 1.5);
    }

    public int getGravitonPulseCooldownSeconds() {
        return plugin.getConfig().getInt("mods.mace.graviton-pulse.cooldown-seconds", 15);
    }

    // --- Meteor Strike ---

    public double getMeteorStrikeMinFallBlocks() {
        return plugin.getConfig().getDouble("mods.mace.meteor-strike.min-fall-blocks", 15);
    }

    public int getMeteorStrikeFireDurationSeconds() {
        return plugin.getConfig().getInt("mods.mace.meteor-strike.fire-duration-seconds", 5);
    }

    // ========================
    // Armor Mod settings
    // ========================

    // --- Hunter's Sight ---

    public double getHuntersSightRadius() {
        return plugin.getConfig().getDouble("mods.armor.helmet.hunters-sight.radius", 15);
    }

    public int getHuntersSightUpdateIntervalTicks() {
        return plugin.getConfig().getInt("mods.armor.helmet.hunters-sight.update-interval-ticks", 40);
    }

    // --- Vitality ---

    public double getVitalityExtraHealth() {
        return plugin.getConfig().getDouble("mods.armor.chestplate.vitality.extra-health", 4.0);
    }

    // --- Last Stand ---

    public double getLastStandHealthThreshold() {
        return plugin.getConfig().getDouble("mods.armor.chestplate.last-stand.health-threshold", 6.0);
    }

    public int getLastStandDurationSeconds() {
        return plugin.getConfig().getInt("mods.armor.chestplate.last-stand.duration-seconds", 5);
    }

    public int getLastStandCooldownSeconds() {
        return plugin.getConfig().getInt("mods.armor.chestplate.last-stand.cooldown-seconds", 60);
    }

    // --- Thorns Aura ---

    public double getThornsAuraReflectDamage() {
        return plugin.getConfig().getDouble("mods.armor.chestplate.thorns-aura.reflect-damage", 2.0);
    }

    // --- Dodger ---

    public double getDodgerDodgeChance() {
        return plugin.getConfig().getDouble("mods.armor.leggings.dodger.dodge-chance", 0.20);
    }

    public int getDodgerInternalCooldownSeconds() {
        return plugin.getConfig().getInt("mods.armor.leggings.dodger.internal-cooldown-seconds", 1);
    }

    // --- Featherweight ---

    public double getFeatherweightReductionPercent() {
        return plugin.getConfig().getDouble("mods.armor.boots.featherweight.reduction-percent", 0.50);
    }

    // --- Double Jump ---

    public int getDoubleJumpCooldownSeconds() {
        return plugin.getConfig().getInt("mods.armor.boots.double-jump.cooldown-seconds", 5);
    }

    public double getDoubleJumpVelocityMultiplier() {
        return plugin.getConfig().getDouble("mods.armor.boots.double-jump.jump-velocity-multiplier", 0.80);
    }

    // --- Frost Walker Plus ---

    public int getFrostWalkerPlusRadius() {
        return plugin.getConfig().getInt("mods.armor.boots.frost-walker-plus.radius", 4);
    }

    public int getFrostWalkerPlusIceDurationSeconds() {
        return plugin.getConfig().getInt("mods.armor.boots.frost-walker-plus.ice-duration-seconds", 10);
    }

    // ========================
    // Elytra Mod settings
    // ========================

    // --- Firework Efficiency ---

    public double getFireworkEfficiencyMultiplier() {
        return plugin.getConfig().getDouble("mods.elytra.firework-efficiency.efficiency-multiplier", 1.5);
    }

    // --- Safe Landing ---

    public double getSafeLandingDamageReduction() {
        return plugin.getConfig().getDouble("mods.elytra.safe-landing.damage-reduction", 0.75);
    }

    public int getSafeLandingAutoSlowFallDurationSeconds() {
        return plugin.getConfig().getInt("mods.elytra.safe-landing.auto-slow-fall-duration-seconds", 2);
    }

    public int getSafeLandingAutoSlowFallCooldownSeconds() {
        return plugin.getConfig().getInt("mods.elytra.safe-landing.auto-slow-fall-cooldown-seconds", 30);
    }

    public int getSafeLandingGroundDetectDistance() {
        return plugin.getConfig().getInt("mods.elytra.safe-landing.ground-detect-distance", 10);
    }

    // --- Aerodynamic ---

    public double getAerodynamicDragReduction() {
        return plugin.getConfig().getDouble("mods.elytra.aerodynamic.drag-reduction", 0.30);
    }

    // --- Boost ---

    public double getBoostSpeedMultiplier() {
        return plugin.getConfig().getDouble("mods.elytra.boost.speed-multiplier", 1.5);
    }

    public int getBoostCooldownSeconds() {
        return plugin.getConfig().getInt("mods.elytra.boost.cooldown-seconds", 18);
    }

    public boolean isBoostTrailEnabled() {
        return plugin.getConfig().getBoolean("mods.elytra.boost.trail-enabled", true);
    }

    public boolean isBoostSoundEnabled() {
        return plugin.getConfig().getBoolean("mods.elytra.boost.sound-enabled", true);
    }

    public float getBoostSoundVolume() {
        return (float) plugin.getConfig().getDouble("mods.elytra.boost.sound-volume", 1.0);
    }

    public float getBoostSoundPitch() {
        return (float) plugin.getConfig().getDouble("mods.elytra.boost.sound-pitch", 0.8);
    }

    // --- Phantom Cloak ---

    public int getPhantomCloakActivationDelaySeconds() {
        return plugin.getConfig().getInt("mods.elytra.phantom-cloak.activation-delay-seconds", 3);
    }

    // --- Sonic Boom ---

    public double getSonicBoomSpeedThreshold() {
        return plugin.getConfig().getDouble("mods.elytra.sonic-boom.speed-threshold", 1.5);
    }

    public double getSonicBoomDamage() {
        return plugin.getConfig().getDouble("mods.elytra.sonic-boom.damage", 4.0);
    }

    public double getSonicBoomRadius() {
        return plugin.getConfig().getDouble("mods.elytra.sonic-boom.radius", 3.0);
    }

    // ========================
    // Formatting
    // ========================

    /**
     * Formats a currency amount as a display string.
     */
    public String formatCurrency(double amount) {
        String format = getCurrencyFormat();
        try {
            DecimalFormat df = new DecimalFormat(format);
            return getCurrencySymbol() + df.format(amount);
        } catch (Exception e) {
            return getCurrencySymbol() + String.format("%.2f", amount);
        }
    }
}

