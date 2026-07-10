package pt.henrique.toolmods.economy;

import pt.henrique.toolmods.ToolMods;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Economy provider backed by Vault (reflection-based, no compile-time dependency).
 * <p>
 * Vault uses synchronous {@code double}-based API. This provider converts
 * between internal {@code long} cents and Vault's doubles using HALF_UP rounding.
 */
public class VaultProvider implements EconomyProvider {

    private final ToolMods plugin;

    private Object vaultEconomy;
    private Method getBalanceMethod;
    private Method withdrawMethod;
    private Method depositMethod;
    private Method getNameMethod;
    private Method txSuccessMethod;

    public VaultProvider(ToolMods plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Vault";
    }

    @Override
    public boolean isAvailable() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
                debug("Vault plugin not found");
                return false;
            }

            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");

            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) {
                debug("No Vault Economy provider registered");
                return false;
            }

            vaultEconomy = rsp.getProvider();
            if (vaultEconomy == null) {
                debug("Vault Economy registration found but provider is null");
                return false;
            }

            // Cache reflection methods
            getBalanceMethod = vaultEconomy.getClass().getMethod("getBalance", OfflinePlayer.class);
            withdrawMethod = vaultEconomy.getClass().getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            depositMethod = vaultEconomy.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class);
            getNameMethod = vaultEconomy.getClass().getMethod("getName");

            Class<?> responseClass = Class.forName("net.milkbowl.vault.economy.EconomyResponse");
            txSuccessMethod = responseClass.getMethod("transactionSuccess");

            String name = (String) getNameMethod.invoke(vaultEconomy);
            debug("Vault Economy loaded: " + name);
            return true;

        } catch (ClassNotFoundException e) {
            debug("Vault classes not on classpath: " + e.getMessage());
            return false;
        } catch (NoClassDefFoundError e) {
            debug("Vault class dependency missing: " + e.getMessage());
            return false;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking Vault availability", e);
            return false;
        }
    }

    @Override
    public CompletableFuture<Long> getBalanceCents(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
                double balance = (double) getBalanceMethod.invoke(vaultEconomy, player);
                long cents = doubleToCents(balance);
                debug("getBalanceCents() player=" + playerUuid + ", vault=" + balance + ", cents=" + cents);
                return cents;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Vault getBalance failed for " + playerUuid, e);
                return 0L;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerUuid, long amountCents, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
                double amount = centsToDouble(amountCents);
                debug("withdraw() player=" + playerUuid + ", cents=" + amountCents + ", double=" + amount);

                Object response = withdrawMethod.invoke(vaultEconomy, player, amount);
                boolean success = (boolean) txSuccessMethod.invoke(response);

                debug("withdraw() result: success=" + success);
                if (!success) {
                    plugin.getLogger().warning("Vault withdraw failed for " + playerUuid);
                }
                return success;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Vault withdraw exception for " + playerUuid, e);
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deposit(UUID playerUuid, long amountCents, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
                double amount = centsToDouble(amountCents);
                debug("deposit() player=" + playerUuid + ", cents=" + amountCents + ", double=" + amount);

                Object response = depositMethod.invoke(vaultEconomy, player, amount);
                boolean success = (boolean) txSuccessMethod.invoke(response);

                debug("deposit() result: success=" + success);
                if (!success) {
                    plugin.getLogger().warning("Vault deposit failed for " + playerUuid);
                }
                return success;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Vault deposit exception for " + playerUuid, e);
                return false;
            }
        });
    }

    // ==================== CONVERSION ====================

    private static long doubleToCents(double amount) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private static double centsToDouble(long cents) {
        return cents / 100.0;
    }

    // ==================== DEBUG ====================

    private boolean isDebug() {
        return plugin.getConfigManager().isDebug();
    }

    private void debug(String message) {
        if (isDebug()) {
            plugin.getLogger().info("[DEBUG-Vault] " + message);
        }
    }
}

