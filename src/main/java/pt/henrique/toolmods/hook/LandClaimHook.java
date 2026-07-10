package pt.henrique.toolmods.hook;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Optional LandClaim integration — checks whether a player can build/break at a location.
 * <p>
 * Uses reflection to avoid a hard dependency on LandClaim. If LandClaim is not
 * installed, all checks return {@code true} (allowed).
 * <p>
 * Looks for a ClaimManager service or API method that checks block protection.
 */
public final class LandClaimHook {

    private static boolean available = false;
    private static boolean initialized = false;
    private static Object claimApi;
    private static Method canBuildMethod;

    private LandClaimHook() {}

    /**
     * Initializes the hook by looking for LandClaim on the server.
     * Call once during plugin enable.
     */
    public static void init(Logger logger) {
        if (initialized) return;
        initialized = true;

        if (Bukkit.getPluginManager().getPlugin("LandClaim") == null) {
            logger.info("LandClaim not found — claim protection checks disabled.");
            return;
        }

        try {
            // Try to find the ClaimManager API via the services manager
            Class<?> apiClass = Class.forName("pt.henrique.landclaim.api.LandClaimApi");
            var registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration != null) {
                claimApi = registration.getProvider();
                // Look for canBuild(Player, Block) or canBuild(UUID, Location)
                try {
                    canBuildMethod = claimApi.getClass().getMethod("canBuild",
                            Player.class, Block.class);
                } catch (NoSuchMethodException e) {
                    try {
                        canBuildMethod = claimApi.getClass().getMethod("canBuild",
                                java.util.UUID.class, org.bukkit.Location.class);
                    } catch (NoSuchMethodException e2) {
                        // Try alternative method name
                        try {
                            canBuildMethod = claimApi.getClass().getMethod("isAllowed",
                                    Player.class, Block.class);
                        } catch (NoSuchMethodException e3) {
                            logger.warning("LandClaim found but no compatible API method detected.");
                            return;
                        }
                    }
                }
                available = true;
                logger.info("LandClaim hook enabled — claim protection active.");
            } else {
                logger.info("LandClaim plugin found but API not registered yet.");
            }
        } catch (ClassNotFoundException e) {
            logger.info("LandClaim API class not found — claim protection checks disabled.");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error initializing LandClaim hook", e);
        }
    }

    /**
     * Checks if the player is allowed to break a block at the given location.
     *
     * @return {@code true} if allowed (or LandClaim not installed)
     */
    public static boolean canBreak(Player player, Block block) {
        if (!available || claimApi == null || canBuildMethod == null) {
            return true; // no LandClaim — allow
        }

        try {
            Object result;
            Class<?>[] params = canBuildMethod.getParameterTypes();
            if (params.length == 2 && params[0] == Player.class && params[1] == Block.class) {
                result = canBuildMethod.invoke(claimApi, player, block);
            } else if (params.length == 2 && params[0] == java.util.UUID.class) {
                result = canBuildMethod.invoke(claimApi, player.getUniqueId(), block.getLocation());
            } else {
                return true;
            }

            if (result instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Exception e) {
            // On error, allow the action to avoid blocking gameplay
            return true;
        }
    }

    /**
     * Whether LandClaim integration is active.
     */
    public static boolean isAvailable() {
        return available;
    }
}

