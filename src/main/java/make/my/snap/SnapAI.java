package make.my.snap;

import com.github.retrooper.packetevents.PacketEvents;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import make.my.snap.service.*;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import make.my.snap.check.util.Config;
import make.my.snap.command.CommandManager;
import make.my.snap.listener.ConnectionListener;
import make.my.snap.listener.MitigationListener;
import make.my.snap.listener.PacketListener;
import make.my.snap.listener.PlayerCheckListener;
import make.my.snap.manager.LocaleManager;
import make.my.snap.mitigation.MitigationManager;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SnapAI extends JavaPlugin {
    private static SnapAI instance;
    private static boolean isServerConnected = false;

    private int heartbeatTaskID = -1;
    private String serverUrl = null;
    private final Set<UUID> alertsDisabledAdmins = ConcurrentHashMap.newKeySet();

    private DatabaseService databaseService;
    private HologramManager hologramManager;
    private TokenService tokenService;
    private OkHttpClient httpClient;
    private ViolationManager violationManager;
    private LocaleManager localeManager;
    private MitigationManager mitigationManager;
    private CheckManager checkManager;
    private DiscordWebhookService discordWebhookService;
    private LocalDatasetService localDatasetService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.serverUrl = getConfig().getString("server.url", "http://api.snapai.tech");

        getLogger().info("SnapAI Anti-Cheat starting...");

        this.localeManager = new LocaleManager(this);
        Config.init(this);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        this.databaseService = new DatabaseService(this);
        this.databaseService.init();

        this.tokenService = new TokenService(this);
        this.discordWebhookService = new DiscordWebhookService(this);
        this.violationManager = new ViolationManager(this);
        this.mitigationManager = new MitigationManager(this);
        this.checkManager = new CheckManager(this);

        this.localDatasetService = new LocalDatasetService(this);

        this.hologramManager = new HologramManager(this);

        checkServerConnection();

        CommandManager commandManager = new CommandManager(this);
        if (getCommand("snapai") != null) {
            getCommand("snapai").setExecutor(commandManager);
            getCommand("snapai").setTabCompleter(commandManager);
        }

        getServer().getPluginManager().registerEvents(new MitigationListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerCheckListener(this), this);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("SnapAI Plugin fully enabled. Recording mode: LOCAL_JSON");
        }, 20L);
    }

    private void checkServerConnection() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Request healthRequest = new Request.Builder().url(serverUrl + "/health").build();
                try (Response healthResponse = this.httpClient.newCall(healthRequest).execute()) {
                    if (healthResponse.isSuccessful() && healthResponse.body() != null) {
                        String responseBody = healthResponse.body().string();
                        Map<String, Object> result = LazyHolder.RESPONSE_ADAPTER.fromJson(responseBody);

                        if (result != null && "healthy".equals(result.get("status"))) {
                            isServerConnected = true;
                            Bukkit.getScheduler().runTask(this, this::initializePluginServices);
                            getLogger().info("Successfully connected to SnapAI Server: " + serverUrl);
                        } else {
                            getLogger().severe("SnapAI Server returned unhealthy status. Disabling plugin...");
                            disablePlugin();
                        }
                    } else {
                        getLogger().severe("SnapAI Server is unreachable. Status Code: " + healthResponse.code());
                        disablePlugin();
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Connection error to SnapAI Server: " + e.getMessage());
                disablePlugin();
            }
        });
    }

    private void disablePlugin() {
        Bukkit.getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
    }

    private void initializePluginServices() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false).reEncodeByDefault(false);
        PacketEvents.getAPI().load();

        PacketEvents.getAPI().getEventManager().registerListeners(
                new ConnectionListener(),
                new PacketListener()
        );

        PacketEvents.getAPI().init();
        long interval = 20L * 60 * 5;
        heartbeatTaskID = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                HeartbeatService::sendHeartbeat,
                100L,
                interval
        ).getTaskId();
    }

    @Override
    public void onDisable() {
        if (databaseService != null) {
            databaseService.close();
        }
        if (heartbeatTaskID != -1) {
            Bukkit.getScheduler().cancelTask(heartbeatTaskID);
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
        try {
            if (PacketEvents.getAPI() != null && PacketEvents.getAPI().isLoaded()) {
                PacketEvents.getAPI().terminate();
            }
        } catch (NoClassDefFoundError ignored) {
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        localeManager.loadMessages();
        discordWebhookService.reload();
    }

    public boolean toggleAlerts(UUID uuid) {
        if (alertsDisabledAdmins.contains(uuid)) {
            alertsDisabledAdmins.remove(uuid);
            return true;
        } else {
            alertsDisabledAdmins.add(uuid);
            return false;
        }
    }

    public boolean areAlertsEnabledFor(UUID uuid) {
        return !alertsDisabledAdmins.contains(uuid);
    }

    public static SnapAI getInstance() {
        return instance;
    }

    public static boolean isServerActive() {
        return isServerConnected;
    }

    public String getServerUrl() {
        return this.serverUrl;
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public LocalDatasetService getLocalDatasetService() {
        return localDatasetService;
    }

    public OkHttpClient getHttpClient() {
        return this.httpClient;
    }

    public ViolationManager getViolationManager() {
        return this.violationManager;
    }

    public HologramManager getHologramManager() {
        return this.hologramManager;
    }

    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    public MitigationManager getMitigationManager() {
        return mitigationManager;
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }

    public DiscordWebhookService getDiscordWebhookService() {
        return discordWebhookService;
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = new Moshi.Builder().build();
        private static final Type MAP_STRING_OBJECT_TYPE = Types.newParameterizedType(Map.class, String.class, Object.class);
        private static final JsonAdapter<Map<String, Object>> RESPONSE_ADAPTER = MOSHI.adapter(MAP_STRING_OBJECT_TYPE);
    }
}