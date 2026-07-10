package pt.henrique.toolmods.hook;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Optional PlayerSettings integration — retrieves per-player language preference
 * and registers ToolMods preferences in the PlayerSettings hub.
 * <p>
 * Uses reflection to avoid a hard dependency. If PlayerSettings is not installed,
 * returns {@code null} / defaults (callers fall back to the global config language).
 */
public final class PlayerSettingsHook {

    private static boolean available = false;
    private static boolean initialized = false;
    private static Object settingsProvider;
    private static Method getLanguageMethod;
    private static Method getBooleanMethod;
    private static Method getStringMethod;
    private static Method registerCategoryMethod;
    private static Method registerToggleMethod;
    private static Method registerEnumMethod;
    private static Method unregisterAllMethod;

    private PlayerSettingsHook() {}

    /**
     * Initializes the hook by looking for PlayerSettings on the server.
     */
    public static void init(Logger logger) {
        if (initialized) return;
        initialized = true;

        if (Bukkit.getPluginManager().getPlugin("PlayerSettings") == null) {
            logger.info("PlayerSettings not found — using global language setting.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName("pt.henrique.playersettings.api.PlayerSettingsApi");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null) {
                settingsProvider = registration.getProvider();
                getLanguageMethod = settingsProvider.getClass().getMethod("getLanguage", UUID.class);
                available = true;
                logger.info("PlayerSettings hook enabled — per-player language active.");

                // Try to get preference methods (may not exist on older PlayerSettings versions)
                try {
                    getBooleanMethod = settingsProvider.getClass().getMethod("getBoolean", UUID.class, String.class, boolean.class);
                    getStringMethod = settingsProvider.getClass().getMethod("getString", UUID.class, String.class, String.class);
                    registerCategoryMethod = settingsProvider.getClass().getMethod("registerCategory",
                            String.class, Material.class, int.class);
                    registerToggleMethod = settingsProvider.getClass().getMethod("registerToggle",
                            String.class, String.class, boolean.class, Material.class, int.class);
                    registerEnumMethod = settingsProvider.getClass().getMethod("registerEnum",
                            String.class, String.class, String.class, List.class, Material.class, int.class);
                    unregisterAllMethod = settingsProvider.getClass().getMethod("unregisterAll", String.class);
                    logger.info("PlayerSettings preferences API available — settings registration enabled.");
                } catch (NoSuchMethodException e) {
                    logger.info("PlayerSettings preferences API not available — using defaults.");
                }
            } else {
                logger.info("PlayerSettings plugin found but API not registered yet.");
            }
        } catch (ClassNotFoundException e) {
            logger.info("PlayerSettings API class not found — using global language.");
        } catch (Exception e) {
            logger.info("PlayerSettings hook failed — using global language.");
        }
    }

    // ========================
    // Settings Registration
    // ========================

    /**
     * Registers ToolMods settings in the PlayerSettings preferences hub.
     * Call once during plugin enable (after init).
     */
    public static void registerSettings() {
        if (!available || registerCategoryMethod == null) return;
        try {
            registerCategoryMethod.invoke(settingsProvider, "toolmods", Material.DIAMOND_PICKAXE, 20);
            registerToggleMethod.invoke(settingsProvider, "toolmods", "reduced-particles", false, Material.BLAZE_POWDER, 1);
            registerToggleMethod.invoke(settingsProvider, "toolmods", "sneak-to-activate", false, Material.LEATHER_BOOTS, 2);
            registerEnumMethod.invoke(settingsProvider, "toolmods", "cooldown-display", "actionbar",
                    List.of("actionbar", "chat", "none"), Material.CLOCK, 3);
            registerToggleMethod.invoke(settingsProvider, "toolmods", "auto-smelt-notify", true, Material.FURNACE, 4);
        } catch (Exception ignored) {}
    }

    /**
     * Unregisters all ToolMods settings from PlayerSettings.
     * Call during plugin disable.
     */
    public static void unregisterSettings() {
        if (!available || unregisterAllMethod == null) return;
        try {
            unregisterAllMethod.invoke(settingsProvider, "toolmods");
        } catch (Exception ignored) {}
    }

    // ========================
    // Language
    // ========================

    /**
     * Gets the preferred language for a player.
     *
     * @return the language code (e.g. "en_US") or {@code null} if not available
     */
    public static String getLanguage(Player player) {
        return getLanguage(player.getUniqueId());
    }

    /**
     * Gets the preferred language for a player UUID.
     *
     * @return the language code or {@code null} if not available
     */
    public static String getLanguage(UUID uuid) {
        if (!available || settingsProvider == null || getLanguageMethod == null) {
            return null;
        }
        try {
            Object result = getLanguageMethod.invoke(settingsProvider, uuid);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ========================
    // Setting Getters
    // ========================

    /**
     * Reads a boolean setting from PlayerSettings for a player.
     *
     * @param uuid         the player UUID
     * @param fullKey      the fully qualified setting key (e.g. "toolmods.reduced-particles")
     * @param defaultValue fallback if PlayerSettings is unavailable or the key isn't set
     * @return the setting value
     */
    public static boolean getSetting(UUID uuid, String fullKey, boolean defaultValue) {
        if (!available || getBooleanMethod == null) return defaultValue;
        try {
            return (boolean) getBooleanMethod.invoke(settingsProvider, uuid, fullKey, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Reads a string/enum setting from PlayerSettings for a player.
     *
     * @param uuid         the player UUID
     * @param fullKey      the fully qualified setting key (e.g. "toolmods.cooldown-display")
     * @param defaultValue fallback value
     * @return the setting value
     */
    public static String getStringSetting(UUID uuid, String fullKey, String defaultValue) {
        if (!available || getStringMethod == null) return defaultValue;
        try {
            Object result = getStringMethod.invoke(settingsProvider, uuid, fullKey, defaultValue);
            return result != null ? result.toString() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ========================
    // Global Setting Helpers
    // ========================

    /**
     * Checks if global particles are enabled for a player.
     * Respects the "global.particles" setting in PlayerSettings.
     */
    public static boolean areParticlesEnabled(UUID uuid) {
        return getSetting(uuid, "global.particles", true);
    }

    /**
     * Checks if global sounds are enabled for a player.
     * Respects the "global.sounds" setting in PlayerSettings.
     */
    public static boolean areSoundsEnabled(UUID uuid) {
        return getSetting(uuid, "global.sounds", true);
    }

    /**
     * Checks if global action bar messages are enabled for a player.
     * Respects the "global.action-bar-messages" setting in PlayerSettings.
     */
    public static boolean isActionBarEnabled(UUID uuid) {
        return getSetting(uuid, "global.action-bar-messages", true);
    }

    /**
     * Checks if the player has reduced particles enabled for ToolMods.
     */
    public static boolean hasReducedParticles(UUID uuid) {
        return getSetting(uuid, "toolmods.reduced-particles", false);
    }

    /**
     * Checks if the player requires sneak to activate area mods.
     * When true, area mods only activate when sneaking (inverted from default behavior).
     */
    public static boolean requiresSneakToActivate(UUID uuid) {
        return getSetting(uuid, "toolmods.sneak-to-activate", false);
    }

    /**
     * Gets the player's preferred cooldown display mode.
     *
     * @return "actionbar", "chat", or "none"
     */
    public static String getCooldownDisplay(UUID uuid) {
        return getStringSetting(uuid, "toolmods.cooldown-display", "actionbar");
    }

    /**
     * Checks if auto-smelt notifications are enabled for the player.
     */
    public static boolean isAutoSmeltNotifyEnabled(UUID uuid) {
        return getSetting(uuid, "toolmods.auto-smelt-notify", true);
    }

    public static boolean isAvailable() {
        return available;
    }
}

