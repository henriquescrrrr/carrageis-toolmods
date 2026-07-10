package pt.henrique.toolmods.economy;

import pt.henrique.toolmods.ToolMods;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Manages economy operations using a dual-provider system.
 * <p>
 * Providers are tried in the order specified by {@code economy.priority} in config.
 * Supported providers:
 * <ul>
 *   <li><b>MultiBank</b> — native async API, long cents (preferred)</li>
 *   <li><b>Vault</b> — synchronous double-based API, adapted to async interface</li>
 * </ul>
 * <p>
 * All monetary values are internally in <b>cents</b> ({@code long}).
 */
public class EconomyManager {

    private final ToolMods plugin;
    private EconomyProvider provider;
    private String providerName = "None";

    public EconomyManager(ToolMods plugin) {
        this.plugin = plugin;
    }

    private boolean isDebug() {
        return plugin.getConfigManager().isDebug();
    }

    private void debug(String message) {
        if (isDebug()) {
            plugin.getLogger().info("[DEBUG-Economy] " + message);
        }
    }

    // ==================== SETUP ====================

    /**
     * Attempts to set up an economy provider based on config priority.
     *
     * @return true if an economy provider was found and loaded
     */
    public boolean setupEconomy() {
        plugin.getLogger().info("=== Economy Detection ===");

        String mode = plugin.getConfigManager().getEconomyMode();
        List<String> priority = plugin.getConfigManager().getEconomyPriority();

        debug("Economy mode: " + mode);
        debug("Economy priority: " + priority);

        // Build available providers
        Map<String, EconomyProvider> providers = new LinkedHashMap<>();
        providers.put("multibank", new MultiBankProvider(plugin));
        providers.put("vault", new VaultProvider(plugin));

        if (mode.equals("auto")) {
            for (String name : priority) {
                String key = name.toLowerCase().trim();
                EconomyProvider candidate = providers.get(key);
                if (candidate == null) {
                    plugin.getLogger().warning("Unknown economy provider in priority list: " + name);
                    continue;
                }
                plugin.getLogger().info("Trying economy provider: " + candidate.getName() + "...");
                try {
                    if (candidate.isAvailable()) {
                        provider = candidate;
                        providerName = candidate.getName();
                        plugin.getLogger().info("Economy provider selected: " + providerName);
                        break;
                    } else {
                        plugin.getLogger().info("  -> " + candidate.getName() + " not available, trying next...");
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                            "Error checking provider " + candidate.getName(), e);
                }
            }
        } else {
            // Force a specific provider
            EconomyProvider candidate = providers.get(mode);
            if (candidate == null) {
                plugin.getLogger().severe("Unknown economy mode: " + mode);
                plugin.getLogger().severe("Valid values: auto, multibank, vault");
                return false;
            }
            plugin.getLogger().info("Economy mode FORCE: " + candidate.getName());
            try {
                if (candidate.isAvailable()) {
                    provider = candidate;
                    providerName = candidate.getName();
                    plugin.getLogger().info("Economy provider selected: " + providerName);
                } else {
                    plugin.getLogger().severe(candidate.getName()
                            + " is not available but was forced via economy.mode!");
                    return false;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Error loading forced provider " + candidate.getName(), e);
                return false;
            }
        }

        if (provider == null) {
            plugin.getLogger().severe("=== Economy Setup FAILED ===");
            plugin.getLogger().severe("No economy provider available!");
            plugin.getLogger().severe("Install MultiBank (preferred) or Vault with an economy plugin.");
            plugin.getLogger().severe("Priority tried: " + priority);
            return false;
        }

        plugin.getLogger().info("Debug mode: " + (isDebug() ? "ENABLED" : "disabled"));
        plugin.getLogger().info("=== Economy Setup Complete ===");
        return true;
    }

    public String getProviderName() {
        return providerName;
    }

    public boolean isReady() {
        return provider != null;
    }

    // ==================== ASYNC OPERATIONS (cents) ====================

    public CompletableFuture<Long> getBalanceCents(UUID playerUuid) {
        if (provider == null) return CompletableFuture.completedFuture(0L);
        return provider.getBalanceCents(playerUuid);
    }

    public CompletableFuture<Boolean> hasCents(UUID playerUuid, long amountCents) {
        return getBalanceCents(playerUuid).thenApply(bal -> bal >= amountCents);
    }

    public CompletableFuture<Boolean> withdrawCents(UUID playerUuid, long amountCents, String reason) {
        if (amountCents <= 0) return CompletableFuture.completedFuture(true);
        if (provider == null) return CompletableFuture.completedFuture(false);
        debug("withdrawCents: player=" + playerUuid + ", cents=" + amountCents + ", reason=" + reason);
        return provider.withdraw(playerUuid, amountCents, reason);
    }

    public CompletableFuture<Boolean> depositCents(UUID playerUuid, long amountCents, String reason) {
        if (amountCents <= 0) return CompletableFuture.completedFuture(true);
        if (provider == null) return CompletableFuture.completedFuture(false);
        debug("depositCents: player=" + playerUuid + ", cents=" + amountCents + ", reason=" + reason);
        return provider.deposit(playerUuid, amountCents, reason);
    }


    // ==================== FORMATTING ====================

    public String format(double amount) {
        return plugin.getConfigManager().formatCurrency(amount);
    }

    public String formatCents(long cents) {
        return format(toDouble(cents));
    }

    // ==================== CONVERSION UTILITIES ====================

    public static long toCents(double amount) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static double toDouble(long cents) {
        return cents / 100.0;
    }
}

