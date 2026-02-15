package make.my.snap.service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import make.my.snap.SnapAI;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.util.MoshiFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiscordWebhookService {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final SnapAI plugin;
    private final OkHttpClient client;
    private boolean enabled;
    private String webhookUrl;
    private String username;
    private String avatarUrl;
    private int color;

    public DiscordWebhookService(SnapAI plugin) {
        this.plugin = plugin;
        this.client = plugin.getHttpClient();
        reload();
    }

    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("alerts.discord.enabled", false);
        this.webhookUrl = plugin.getConfig().getString("alerts.discord.webhook-url", "");
        this.username = plugin.getConfig().getString("alerts.discord.username", "SnapAI");
        this.avatarUrl = plugin.getConfig().getString("alerts.discord.avatar-url", "");
        this.color = plugin.getConfig().getInt("alerts.discord.color", 3834837);
    }

    public void sendLegacyAlert(PlayerEntity entity, String checkName, String vlText) {
        if (!ready()) return;

        String description = format("webhook.description-check")
                .replace("%player%", entity.getName())
                .replace("%check%", checkName)
                .replace("%vl%", vlText);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("webhook.fields.player", entity.getName()));
        fields.add(field("webhook.fields.detection", checkName));
        fields.add(field("webhook.fields.vl", vlText));

        sendEmbed(description, fields);
    }

    public void sendMlAlert(PlayerEntity entity, String probabilityText, String vlText) {
        if (!ready()) return;

        String description = format("webhook.description-ml")
                .replace("%player%", entity.getName())
                .replace("%probability%", probabilityText)
                .replace("%vl%", vlText);

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("webhook.fields.player", entity.getName()));
        fields.add(field("webhook.fields.detection", format("webhook.detection-ml")));
        fields.add(field("webhook.fields.probability", probabilityText));
        fields.add(field("webhook.fields.vl", vlText));

        sendEmbed(description, fields);
    }

    private void sendEmbed(String description, List<Map<String, Object>> fields) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", format("webhook.title"));
        embed.put("description", description);
        embed.put("color", color);
        embed.put("fields", fields);
        embed.put("timestamp", Instant.now().toString());
        String footer = format("webhook.footer");
        if (!footer.isEmpty()) {
            embed.put("footer", Collections.singletonMap("text", footer));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("embeds", Collections.singletonList(embed));
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            payload.put("avatar_url", avatarUrl);
        }

        String json = MoshiFactory.getAdapter().toJson(payload);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RequestBody body = RequestBody.create(json, JSON);
            Request request = new Request.Builder().url(webhookUrl).post(body).build();
            try (Response ignored = client.newCall(request).execute()) {
            } catch (Exception ignored) {
            }
        });
    }

    private boolean ready() {
        return enabled && webhookUrl != null && !webhookUrl.isEmpty();
    }

    private String format(String key) {
        String raw = ChatColor.stripColor(plugin.getLocaleManager().getMessage(key));
        if (raw == null) return "";
        if (raw.startsWith("Error: Message not found")) return "";
        return raw;
    }

    private Map<String, Object> field(String nameKey, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", format(nameKey));
        map.put("value", value);
        map.put("inline", false);
        return map;
    }
}
