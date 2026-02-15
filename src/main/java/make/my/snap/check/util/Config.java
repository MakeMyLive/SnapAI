package make.my.snap.check.util;

import org.bukkit.configuration.file.FileConfiguration;
import make.my.snap.SnapAI;

public class Config {
    private static SnapAI plugin;

    public static void init(SnapAI snapAI) {
        plugin = snapAI;
    }

    private static SnapAI getPlugin() {
        if (plugin == null) {
            plugin = SnapAI.getInstance();
        }
        return plugin;
    }

    private static String wrapPath(String path) {
        return "legacy-checks." + path;
    }

    private static FileConfiguration cfg() {
        return getPlugin().getConfig();
    }

    public static String getString(String path, String def) {
        String full = wrapPath(path);
        String str = cfg().getString(full);
        return str != null ? str : def;
    }

    public static boolean getBoolean(String path, boolean def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getBoolean(full);
        }
        return def;
    }

    public static int getInt(String path, int def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getInt(full);
        }
        return def;
    }

    public static double getDouble(String path, double def) {
        String full = wrapPath(path);
        if (cfg().contains(full)) {
            return cfg().getDouble(full);
        }
        return def;
    }
}

