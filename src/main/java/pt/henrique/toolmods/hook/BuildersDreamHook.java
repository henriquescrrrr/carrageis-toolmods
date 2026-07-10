package pt.henrique.toolmods.hook;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional BuildersDream integration — checks whether a player currently has an
 * active BuildersDream service (the timed "creative-lite" builder mode).
 * <p>
 * Used to suppress tool-mod block drops while a player is in a builder service.
 * BuildersDream's own "suppress all drops" anti-farm protection only covers the
 * directly-broken block (via {@code BlockBreakEvent#setDropItems(false)}); our
 * multi-block mods (Vein Miner, Tree Feller, Area Mine, Excavator) break the
 * extra blocks themselves and would otherwise bypass it, letting builders farm
 * resources with fly + haste. This hook lets us honour that protection.
 * <p>
 * Uses reflection to avoid a hard dependency on BuildersDream. If BuildersDream
 * is not installed, {@link #hasActiveService(UUID)} returns {@code false}
 * (drops allowed as normal).
 */
public final class BuildersDreamHook {

    private static boolean available = false;
    private static boolean initialized = false;
    private static Object api;
    private static Method hasActiveServiceMethod;

    private BuildersDreamHook() {}

    /**
     * Initializes the hook by looking for the BuildersDream API on the server.
     * Call once during plugin enable.
     */
    public static void init(Logger logger) {
        if (initialized) return;
        initialized = true;

        if (Bukkit.getPluginManager().getPlugin("BuildersDream") == null) {
            logger.info("BuildersDream not found — builder-service drop suppression disabled.");
            return;
        }

        try {
            Class<?> apiClass = Class.forName("pt.henrique.buildersdream.api.BuildersDreamApi");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null && registration.getProvider() != null) {
                api = registration.getProvider();
                hasActiveServiceMethod = api.getClass().getMethod("hasActiveService", UUID.class);
                available = true;
                logger.info("BuildersDream hook enabled — builder-service drop suppression active.");
            } else {
                logger.info("BuildersDream plugin found but API not registered yet.");
            }
        } catch (ClassNotFoundException e) {
            logger.info("BuildersDream API class not found — drop suppression disabled.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error initializing BuildersDream hook", e);
        }
    }

    /**
     * Checks whether the player currently has an active BuildersDream service,
     * in which case tool-mod drops should be suppressed (anti-farm).
     *
     * @return {@code true} if a service is active; {@code false} if BuildersDream
     *         is absent or the player has no active service.
     */
    public static boolean hasActiveService(UUID uuid) {
        if (!available || api == null || hasActiveServiceMethod == null) {
            return false;
        }
        try {
            Object result = hasActiveServiceMethod.invoke(api, uuid);
            return result instanceof Boolean b && b;
        } catch (Exception e) {
            // On error, do not suppress — avoid silently breaking normal mining.
            return false;
        }
    }

    /**
     * Whether BuildersDream integration is active.
     */
    public static boolean isAvailable() {
        return available;
    }
}
