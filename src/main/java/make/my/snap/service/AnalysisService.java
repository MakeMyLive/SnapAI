package make.my.snap.service;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import okhttp3.*;
import org.bukkit.Bukkit;
import make.my.snap.SnapAI;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.entity.Frame;
import make.my.snap.util.MoshiFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalysisService {

    public static void analyze(final PlayerEntity entity) {
        if (!SnapAI.isServerActive() || entity.getFrames().isEmpty()) {
            return;
        }

        List<Frame> framesToSend = new ArrayList<>(entity.getFrames());
        entity.getFrames().clear();

        entity.setLastAnalyzedFrames(framesToSend);

        String json;
        try {
            Map<String, Object> dataToSend = new HashMap<>();
            dataToSend.put("name", entity.getName());
            dataToSend.put("frames", framesToSend);

            Moshi moshi = MoshiFactory.getInstance();
            Type mapType = Types.newParameterizedType(Map.class, String.class, Object.class);
            JsonAdapter<Map<String, Object>> adapter = moshi.adapter(mapType);
            json = adapter.toJson(dataToSend);
        } catch (Exception e) {
            SnapAI.getInstance().getLogger().warning("Failed to serialize frames for " + entity.getName());
            return;
        }

        try {
            TokenService tokenService = SnapAI.getInstance().getTokenService();
            if (tokenService != null && tokenService.hasToken()) {
                json = tokenService.addTokenToRequest(json);
            }
        } catch (Exception ignored) {}

        String url = SnapAI.getInstance().getServerUrl() + "/analyze";

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        SnapAI.getInstance().getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                SnapAI.getInstance().getLogger().severe("Network error during analysis: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful() || responseBody == null) {
                        return;
                    }

                    String bodyString = responseBody.string();
                    Map<String, Double> result = LazyHolder.MAP_ADAPTER.fromJson(bodyString);

                    if (result != null && result.containsKey("cheat_probability")) {
                        double probability = result.get("cheat_probability");

                        Bukkit.getScheduler().runTask(SnapAI.getInstance(), () -> {
                            handleAnalysisResult(entity, probability);
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private static void handleAnalysisResult(PlayerEntity entity, double probability) {
        SnapAI plugin = SnapAI.getInstance();
        entity.addVerdict(probability);

        if (plugin.getHologramManager() != null) {
            plugin.getHologramManager().updateTextFor(entity.getUuid());
        }
        double classificationThreshold = plugin.getConfig().getDouble("ml-check.classification-threshold", 0.8);

        if (probability <= classificationThreshold) {
            return;
        }
        plugin.getViolationManager().handleViolation(entity, probability);
    }

    private static class LazyHolder {
        private static final Moshi MOSHI = MoshiFactory.getInstance();
        private static final Type MAP_TYPE = Types.newParameterizedType(Map.class, String.class, Double.class);
        private static final JsonAdapter<Map<String, Double>> MAP_ADAPTER = MOSHI.adapter(MAP_TYPE);
    }
}