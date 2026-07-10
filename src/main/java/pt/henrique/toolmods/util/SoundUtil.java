package pt.henrique.toolmods.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Utility for playing sounds. Handles Paper 1.21+ Sound registry API.
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = findSound(soundName);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (Exception ignored) {
        }
    }

    public static void playSound(Player player, String soundName) {
        playSound(player, soundName, 0.5f, 1.0f);
    }

    public static void success(Player player) {
        playSound(player, "entity.experience_orb.pickup", 0.7f, 1.2f);
    }

    public static void error(Player player) {
        playSound(player, "entity.villager.no", 0.7f, 1.0f);
    }

    public static void click(Player player) {
        playSound(player, "ui.button.click", 0.5f, 1.0f);
    }

    public static void purchase(Player player) {
        playSound(player, "block.note_block.pling", 0.8f, 1.5f);
    }

    public static void toggle(Player player) {
        playSound(player, "block.lever.click", 0.6f, 1.2f);
    }

    private static Sound findSound(String name) {
        if (name == null) return null;
        String key = name.toLowerCase().replace("_", ".");
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key));
        if (sound != null) return sound;
        // Try original key
        return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
    }
}

