package pt.henrique.toolmods.hook;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * Hook into AntiXRAY plugin to notify it when blocks are broken
 * by area-effect mods (AreaMine, VeinMiner, Excavator, TreeFeller).
 * <p>
 * Without this hook, fake ores from AntiXRAY appear around area-mined regions
 * because ToolMods uses setType(AIR) which doesn't fire BlockBreakEvent.
 */
public final class AntiXrayHook {

    private static boolean available = false;
    private static boolean initialized = false;
    private static Object apiInstance;
    private static Method notifyBlockBrokenMethod;   // (World, int, int, int)
    private static Method notifyBlocksBrokenMethod;   // (World, List<int[]>)

    private AntiXrayHook() {}

    /**
     * Initializes the hook by looking for AntiXRAY on the server.
     * Call once during plugin enable.
     */
    public static void init(Logger logger) {
        if (initialized) return;
        initialized = true;

        if (Bukkit.getPluginManager().getPlugin("AntiXRAY") == null) {
            logger.info("AntiXRAY not found — area mining will not reveal fake ores.");
            return;
        }

        try {
            // Try to find the API via ServicesManager
            // The API class could be in pt.henrique.antiXRAY.api or pt.henrique.antixray.api
            Class<?> apiClass = null;
            for (String pkg : new String[]{
                    "pt.henrique.antiXRAY.api.AntiXrayApi",
                    "pt.henrique.antixray.api.AntiXrayApi"
            }) {
                try {
                    apiClass = Class.forName(pkg);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }

            if (apiClass == null) {
                logger.warning("AntiXRAY found but API class not available. Update AntiXRAY to enable area-mine compatibility.");
                return;
            }

            var reg = Bukkit.getServicesManager().getRegistration(apiClass);
            if (reg == null || reg.getProvider() == null) {
                logger.warning("AntiXRAY API not registered in ServicesManager. Update AntiXRAY.");
                return;
            }

            apiInstance = reg.getProvider();
            notifyBlockBrokenMethod = apiInstance.getClass().getMethod(
                    "notifyBlockBroken", World.class, int.class, int.class, int.class);
            notifyBlocksBrokenMethod = apiInstance.getClass().getMethod(
                    "notifyBlocksBroken", World.class, List.class);
            available = true;
            logger.info("AntiXRAY hook enabled — area mining will correctly reveal fake ores.");

        } catch (Exception e) {
            logger.warning("AntiXRAY hook failed: " + e.getMessage());
        }
    }

    /**
     * Notify AntiXRAY that a single block was broken.
     * Call AFTER setting the block to AIR.
     */
    public static void notifyBlockBroken(World world, int x, int y, int z) {
        if (!available) return;
        try {
            notifyBlockBrokenMethod.invoke(apiInstance, world, x, y, z);
        } catch (Exception ignored) {
            // Silently ignore — don't spam console
        }
    }

    /**
     * Notify AntiXRAY that multiple blocks were broken.
     * More efficient for area mining.
     */
    public static void notifyBlocksBroken(World world, List<int[]> positions) {
        if (!available) return;
        try {
            notifyBlocksBrokenMethod.invoke(apiInstance, world, positions);
        } catch (Exception ignored) {
            // Silently ignore
        }
    }

    public static boolean isAvailable() {
        return available;
    }
}

