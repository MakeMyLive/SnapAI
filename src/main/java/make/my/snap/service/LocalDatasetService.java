package make.my.snap.service;

import com.squareup.moshi.JsonAdapter;
import make.my.snap.SnapAI;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.util.MoshiFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Сервис для локального сохранения собранных датасетов в файлы JSON.
 * Данные сохраняются в папку plugins/SnapAI/datasets/
 */
public class LocalDatasetService {

    private final SnapAI plugin;
    private final File datasetFolder;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public LocalDatasetService(SnapAI plugin) {
        this.plugin = plugin;
        this.datasetFolder = new File(plugin.getDataFolder(), "datasets");
        if (!datasetFolder.exists()) {
            if (datasetFolder.mkdirs()) {
                plugin.getLogger().info("[SnapAI] Created local datasets directory.");
            }
        }
    }

    public void saveDatasetLocally(PlayerEntity entity) {
        if (entity.getFrames() == null || entity.getFrames().isEmpty()) {
            return;
        }
        String typeLabel = (entity.getRecordingLabel() == 1) ? "CHEAT" : "LEGIT";
        String timestamp = dateFormat.format(new Date());
        String fileName = String.format("%s_%s_%s_%d.json",
                entity.getName(),
                typeLabel,
                timestamp,
                System.currentTimeMillis());

        File file = new File(datasetFolder, fileName);

        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("name", entity.getName());
            payload.put("is_cheater", entity.getRecordingLabel());
            payload.put("frame_count", entity.getFrames().size());
            payload.put("frames", entity.getFrames());
            JsonAdapter<Map<String, Object>> adapter = MoshiFactory.getAdapter();
            String json = adapter.indent("  ").toJson(payload);

            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(json);
            }

            plugin.getLogger().info("[SnapAI] Dataset successfully saved: " + fileName + " (Type: " + typeLabel + ")");

        } catch (IOException e) {
            plugin.getLogger().severe("[SnapAI] FAILED to save local dataset for " + entity.getName());
            plugin.getLogger().severe("Error details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getCollectedDatasetsCount() {
        if (!datasetFolder.exists()) return 0;
        File[] files = datasetFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        return (files != null) ? files.length : 0;
    }

    public void purgeLocalDatasets() {
        if (!datasetFolder.exists()) return;
        File[] files = datasetFolder.listFiles();
        if (files != null) {
            int count = 0;
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                    if (file.delete()) {
                        count++;
                    }
                }
            }
            plugin.getLogger().info("[SnapAI] Purged " + count + " local dataset files.");
        }
    }

    public File getDatasetFolder() {
        return datasetFolder;
    }
}