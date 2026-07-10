package pt.henrique.toolmods.config;

import pt.henrique.toolmods.ToolMods;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pt.henrique.toolmods.hook.PlayerSettingsHook;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Per-player language system.
 * <p>
 * Tries PlayerSettings API for per-player language; falls back to the global
 * language in config.yml.
 */
public class LangManager {

    private final ToolMods plugin;
    private final MiniMessage mini = MiniMessage.miniMessage();

    /** language code → loaded config */
    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();

    /** Fallback: the default language config (always loaded) */
    private FileConfiguration defaultConfig;
    private String defaultLanguage;

    public LangManager(ToolMods plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        languageConfigs.clear();
        defaultLanguage = plugin.getConfigManager().getLanguage();

        saveDefaultLanguageFiles();

        // Pre-load all language files in the lang/ folder
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (langFolder.isDirectory()) {
            File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String code = file.getName().replace(".yml", "");
                    FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    languageConfigs.put(code, cfg);
                }
            }
        }

        // Load jar defaults as fallback
        defaultConfig = languageConfigs.getOrDefault(defaultLanguage,
                languageConfigs.getOrDefault("en_US", new YamlConfiguration()));

        // Apply jar defaults to every loaded config
        InputStream jarDefault = plugin.getResource("lang/en_US.yml");
        if (jarDefault != null) {
            YamlConfiguration jarCfg = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(jarDefault, StandardCharsets.UTF_8));
            for (FileConfiguration cfg : languageConfigs.values()) {
                cfg.setDefaults(jarCfg);
            }
            if (!languageConfigs.containsKey("en_US")) {
                languageConfigs.put("en_US", jarCfg);
            }
            defaultConfig = languageConfigs.getOrDefault(defaultLanguage, jarCfg);
        }

        plugin.getLogger().info("Loaded languages: " + languageConfigs.keySet());
        plugin.getLogger().info("Default language: " + defaultLanguage);
    }

    private void saveDefaultLanguageFiles() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        for (String lang : new String[]{"en_US.yml", "pt_PT.yml", "fr_FR.yml"}) {
            File file = new File(langFolder, lang);
            if (!file.exists()) {
                plugin.saveResource("lang/" + lang, false);
            }
        }
    }

    // ========================
    // Language Resolution
    // ========================

    /**
     * Gets the language code for a player.
     * Tries PlayerSettings hook first, then falls back to config global.
     */
    public String getPlayerLanguage(Player player) {
        if (player == null) return defaultLanguage;

        // Try PlayerSettings hook
        try {
            String lang = PlayerSettingsHook.getLanguage(player);
            if (lang != null && languageConfigs.containsKey(lang)) {
                return lang;
            }
        } catch (NoClassDefFoundError | Exception ignored) {
            // PlayerSettings not installed or API not available
        }

        return defaultLanguage;
    }


    // ========================
    // Message Retrieval
    // ========================

    /**
     * Gets a FileConfiguration for the given language (or default).
     */
    private FileConfiguration getConfig(String language) {
        return languageConfigs.getOrDefault(language, defaultConfig);
    }

    /**
     * Gets a raw string from the language file using the default language.
     */
    public String getRaw(String path) {
        return getRaw(defaultLanguage, path);
    }

    /**
     * Gets a raw string from the language file.
     */
    public String getRaw(String language, String path) {
        FileConfiguration cfg = getConfig(language);
        return cfg.getString(path, defaultConfig.getString(path, "<red>Missing: " + path));
    }

    /**
     * Gets a list of strings from the language file.
     */
    public List<String> getRawList(String language, String path) {
        FileConfiguration cfg = getConfig(language);
        List<String> list = cfg.getStringList(path);
        if (list.isEmpty()) {
            list = defaultConfig.getStringList(path);
        }
        return list;
    }

    /**
     * Gets a Component message for a player (auto-resolves language).
     */
    public Component get(Player player, String path) {
        String lang = getPlayerLanguage(player);
        return mini.deserialize(getRaw(lang, path));
    }

    /**
     * Gets a Component message with placeholders for a player.
     */
    public Component get(Player player, String path, Map<String, String> placeholders) {
        String lang = getPlayerLanguage(player);
        String raw = getRaw(lang, path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return mini.deserialize(raw);
    }

    /**
     * Gets a Component message with a single placeholder.
     */
    public Component get(Player player, String path, String key, String value) {
        return get(player, path, Map.of(key, value));
    }

    /**
     * Gets a prefixed message for a player.
     */
    public Component getPrefixed(Player player, String path) {
        String lang = getPlayerLanguage(player);
        String prefix = getRaw(lang, "prefix");
        String message = getRaw(lang, path);
        return mini.deserialize(prefix + message);
    }

    /**
     * Gets a prefixed message with placeholders.
     */
    public Component getPrefixed(Player player, String path, Map<String, String> placeholders) {
        String lang = getPlayerLanguage(player);
        String prefix = getRaw(lang, "prefix");
        String message = getRaw(lang, path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return mini.deserialize(prefix + message);
    }

    /**
     * Gets a prefixed message with a single placeholder.
     */
    public Component getPrefixed(Player player, String path, String key, String value) {
        return getPrefixed(player, path, Map.of(key, value));
    }

    /**
     * Gets a list of components from the language file for a player.
     */
    public List<Component> getList(Player player, String path) {
        String lang = getPlayerLanguage(player);
        return getRawList(lang, path).stream()
                .map(mini::deserialize)
                .toList();
    }

    /**
     * Gets a raw list for the default language.
     */
    public List<String> getRawList(String path) {
        return getRawList(defaultLanguage, path);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}

