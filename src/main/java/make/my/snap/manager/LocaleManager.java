package make.my.snap.manager;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import make.my.snap.SnapAI;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class LocaleManager {

    private final SnapAI plugin;
    private FileConfiguration langConfig;
    private String langCode;

    public LocaleManager(SnapAI plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    /**
     * Основная логика загрузки языкового файла
     */
    public void loadMessages() {
        this.langCode = plugin.getConfig().getString("language", "en").toLowerCase();
        File langFile = new File(plugin.getDataFolder(), "lang_" + langCode + ".yml");
        if (!langFile.exists()) {
            try {
                plugin.saveResource("lang_" + langCode + ".yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Language file lang_" + langCode + ".yml not found in resources. Fallback to English.");
                langCode = "en";
                langFile = new File(plugin.getDataFolder(), "lang_en.yml");
                if (!langFile.exists()) {
                    plugin.saveResource("lang_en.yml", false);
                }
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
        try {
            InputStreamReader defConfigStream = new InputStreamReader(plugin.getResource("lang_" + langCode + ".yml"), StandardCharsets.UTF_8);
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                langConfig.setDefaults(defConfig);
                defConfigStream.close();
            }
        } catch (Exception ignored) {
        }
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        if (langConfig == null) {
            return ChatColor.RED + "Error: Lang config not loaded";
        }

        String message = langConfig.getString(path);
        if (message == null) {
            if (langConfig.getDefaults() != null) {
                message = langConfig.getDefaults().getString(path);
            }
        }

        if (message == null) {
            return ChatColor.RED + "Error: Message not found for key '" + path + "'";
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getLangCode() {
        return langCode;
    }
}