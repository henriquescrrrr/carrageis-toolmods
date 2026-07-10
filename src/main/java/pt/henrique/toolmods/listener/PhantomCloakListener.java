package pt.henrique.toolmods.listener;

import pt.henrique.toolmods.ToolMods;
import pt.henrique.toolmods.mod.ModType;
import pt.henrique.toolmods.mod.ModUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phantom Cloak — after 3 seconds of continuous gliding, become invisible.
 * <p>
 * Applies Invisibility potion effect (no particles) and hides nametag via
 * scoreboard team. Decloaks on land, damage, or attack.
 */
public class PhantomCloakListener implements Listener {

    private static final String TEAM_NAME = "tm_phantom";

    private final ToolMods plugin;

    /** Tracks when each player started gliding continuously. */
    private final Map<UUID, Long> glideStartTime = new ConcurrentHashMap<>();

    /** Tracks which players are currently cloaked. */
    private final Map<UUID, Boolean> cloakedPlayers = new ConcurrentHashMap<>();

    private BukkitTask checkTask;

    public PhantomCloakListener(ToolMods plugin) {
        this.plugin = plugin;
        ensureTeamExists();
        startCheckTask();
    }

    private void ensureTeamExists() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team == null) {
            team = board.registerNewTeam(TEAM_NAME);
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    private void startCheckTask() {
        checkTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            int activationDelay = plugin.getConfigManager().getPhantomCloakActivationDelaySeconds();
            long now = System.currentTimeMillis();

            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();

                if (!player.isGliding()) {
                    // Player stopped gliding — decloak if cloaked
                    if (glideStartTime.remove(playerId) != null && cloakedPlayers.containsKey(playerId)) {
                        decloak(player);
                    }
                    continue;
                }

                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate == null || chestplate.getType() != Material.ELYTRA
                        || !ModUtils.isModEnabled(chestplate, ModType.PHANTOM_CLOAK)) {
                    if (cloakedPlayers.containsKey(playerId)) {
                        decloak(player);
                    }
                    glideStartTime.remove(playerId);
                    continue;
                }

                // Track glide start
                glideStartTime.putIfAbsent(playerId, now);

                long glidingMs = now - glideStartTime.get(playerId);
                boolean isCloaked = cloakedPlayers.containsKey(playerId);

                if (!isCloaked && glidingMs >= activationDelay * 1000L) {
                    // Activate cloak
                    cloak(player);
                }
            }
        }, 10L, 10L); // Every 10 ticks (0.5s)
    }

    private void cloak(Player player) {
        UUID playerId = player.getUniqueId();
        cloakedPlayers.put(playerId, true);

        // Invisibility (no particles)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE, 0, false, false, false));

        // Hide nametag
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team != null) {
            team.addEntry(player.getName());
        }

        // Fade-out particles
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 1.0, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation(), 15, 0.5, 1.0, 0.5, 0.01);
    }

    private void decloak(Player player) {
        UUID playerId = player.getUniqueId();
        if (!cloakedPlayers.containsKey(playerId)) return;
        cloakedPlayers.remove(playerId);

        // Remove invisibility
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        // Restore nametag
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(TEAM_NAME);
        if (team != null) {
            team.removeEntry(player.getName());
        }

        // Fade-in particles
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 1.0, 0.5, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 15, 0.5, 1.0, 0.5, 0.05);
    }

    // Decloak on taking damage
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (cloakedPlayers.containsKey(player.getUniqueId())) {
            decloak(player);
            glideStartTime.remove(player.getUniqueId());
        }
    }

    // Decloak on attacking someone
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (cloakedPlayers.containsKey(player.getUniqueId())) {
            decloak(player);
            glideStartTime.remove(player.getUniqueId());
        }
    }

    // Decloak on quit — the Invisibility effect is set with Integer.MAX_VALUE
    // duration, so if a cloaked player logs off it would be saved to their
    // player data and, after an in-memory state reset (plugin reload/server
    // restart), leave them permanently invisible with a hidden nametag.
    // Removing it here (before the player data is persisted) prevents that.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (cloakedPlayers.containsKey(player.getUniqueId())) {
            decloak(player);
        }
        glideStartTime.remove(player.getUniqueId());
    }

    public void shutdown() {
        if (checkTask != null) {
            checkTask.cancel();
            checkTask = null;
        }
        // Decloak all players
        for (UUID playerId : cloakedPlayers.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                decloak(player);
            }
        }
        cloakedPlayers.clear();
        glideStartTime.clear();
    }
}

