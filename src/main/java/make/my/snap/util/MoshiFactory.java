package make.my.snap.util;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Types;
import make.my.snap.entity.Frame;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class MoshiFactory {

    private static final Moshi INSTANCE;
    private static final JsonAdapter<Map<String, Object>> ADAPTER;

    static {
        Moshi moshi = null;
        JsonAdapter<Map<String, Object>> adapter = null;
        try {
            moshi = new Moshi.Builder()
                    .add(new UUIDAdapter())
                    .add(Frame.class, new FrameAdapter())
                    .build();

            Type mapType = Types.newParameterizedType(Map.class, String.class, Object.class);
            adapter = moshi.adapter(mapType);

        } catch (Throwable t) {
            System.err.println("[SnapAI] CRITICAL: Failed to initialize MoshiFactory!");
            t.printStackTrace();
        }
        INSTANCE = moshi;
        ADAPTER = adapter;
    }

    public static Moshi getInstance() {
        return INSTANCE;
    }

    public static JsonAdapter<Map<String, Object>> getAdapter() {
        return ADAPTER;
    }

    public static class UUIDAdapter {
        @ToJson
        public String toJson(UUID uuid) {
            return uuid != null ? uuid.toString() : null;
        }

        @FromJson
        public UUID fromJson(String json) {
            return json != null ? UUID.fromString(json) : null;
        }
    }

    public static class FrameAdapter extends JsonAdapter<Frame> {
        @Override
        public void toJson(JsonWriter writer, Frame frame) throws IOException {
            if (frame == null) {
                writer.nullValue();
                return;
            }
            writer.beginObject();
            writer.name("x").value(frame.x);
            writer.name("y").value(frame.y);
            writer.name("deltaX").value(frame.deltaX);
            writer.name("deltaY").value(frame.deltaY);
            writer.name("jerkX").value(frame.jerkX);
            writer.name("jerkY").value(frame.jerkY);
            writer.name("gcdErrorX").value(frame.gcdErrorX);
            writer.name("gcdErrorY").value(frame.gcdErrorY);
            writer.endObject();
        }

        @Override
        public Frame fromJson(JsonReader reader) throws IOException {
            float x=0, y=0, dx=0, dy=0, jx=0, jy=0, gx=0, gy=0;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "x": x = (float) reader.nextDouble(); break;
                    case "y": y = (float) reader.nextDouble(); break;
                    case "deltaX": dx = (float) reader.nextDouble(); break;
                    case "deltaY": dy = (float) reader.nextDouble(); break;
                    case "jerkX": jx = (float) reader.nextDouble(); break;
                    case "jerkY": jy = (float) reader.nextDouble(); break;
                    case "gcdErrorX": gx = (float) reader.nextDouble(); break;
                    case "gcdErrorY": gy = (float) reader.nextDouble(); break;
                    default: reader.skipValue(); break;
                }
            }
            reader.endObject();
            return new Frame(x, y, dx, dy, jx, jy, gx, gy);
        }
    }
}