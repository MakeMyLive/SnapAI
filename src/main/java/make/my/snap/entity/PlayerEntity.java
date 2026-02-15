package make.my.snap.entity;

import org.bukkit.scheduler.BukkitTask;
import make.my.snap.SnapAI;
import make.my.snap.service.AnalysisService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class PlayerEntity {
    private final UUID uuid;
    private final String name;
    private float lastYaw = 0.0F;
    private float lastPitch = 0.0F;
    private float lastDeltaYaw = 0.0F;
    private float lastDeltaPitch = 0.0F;
    private float lastAccelYaw = 0.0F;
    private float lastAccelPitch = 0.0F;
    private float startYaw = 0.0F;
    private float startPitch = 0.0F;
    private final List<Frame> frames = new LinkedList<>();
    private List<Frame> lastAnalyzedFrames = null;
    private volatile boolean isProcessingFlag = false;
    private long combatTagUntil = 0L;
    private BukkitTask postCombatAnalysisTask = null;

    private volatile boolean recording = false;
    private int recordingLabel = 0;
    private UUID recorderUUID = null;
    private int setsToCollect = 0;
    public static class Verdict {
        public final double probability;
        public final long timestamp;

        public Verdict(double probability) {
            this.probability = probability;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private final LinkedList<Verdict> recentVerdicts = new LinkedList<>();

    public PlayerEntity(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public float getLastYaw() {
        return lastYaw;
    }

    public void setLastYaw(float lastYaw) {
        this.lastYaw = lastYaw;
    }

    public float getLastPitch() {
        return lastPitch;
    }

    public void setLastPitch(float lastPitch) {
        this.lastPitch = lastPitch;
    }

    public float getLastDeltaYaw() {
        return lastDeltaYaw;
    }

    public void setLastDeltaYaw(float lastDeltaYaw) {
        this.lastDeltaYaw = lastDeltaYaw;
    }

    public float getLastDeltaPitch() {
        return lastDeltaPitch;
    }

    public void setLastDeltaPitch(float lastDeltaPitch) {
        this.lastDeltaPitch = lastDeltaPitch;
    }

    public float getLastAccelYaw() {
        return lastAccelYaw;
    }

    public void setLastAccelYaw(float lastAccelYaw) {
        this.lastAccelYaw = lastAccelYaw;
    }

    public float getLastAccelPitch() {
        return lastAccelPitch;
    }

    public void setLastAccelPitch(float lastAccelPitch) {
        this.lastAccelPitch = lastAccelPitch;
    }

    public float getStartYaw() {
        return startYaw;
    }

    public float getStartPitch() {
        return startPitch;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public List<Frame> getLastAnalyzedFrames() {
        return lastAnalyzedFrames;
    }

    public void setLastAnalyzedFrames(List<Frame> frames) {
        this.lastAnalyzedFrames = new ArrayList<>(frames);
    }

    public boolean isProcessingFlag() {
        return isProcessingFlag;
    }

    public void setProcessingFlag(boolean processingFlag) {
        isProcessingFlag = processingFlag;
    }

    public boolean isInCombat() {
        return System.currentTimeMillis() < combatTagUntil;
    }

    public boolean isRecording() {
        return recording;
    }

    public int getRecordingLabel() {
        return recordingLabel;
    }

    public UUID getRecorderUUID() {
        return recorderUUID;
    }

    public int getSetsToCollect() {
        return setsToCollect;
    }

    public void decrementSets() {
        if (this.setsToCollect > 0) {
            this.setsToCollect--;
        }
    }

    public void startRecording(int label, UUID recorder, int amount, float curYaw, float curPitch) {
        this.recording = true;
        this.recordingLabel = label;
        this.recorderUUID = recorder;
        this.setsToCollect = amount;
        this.startYaw = curYaw;
        this.startPitch = curPitch;
        this.frames.clear();
    }

    public void stopRecording() {
        this.recording = false;
        this.recorderUUID = null;
        this.setsToCollect = 0;
        this.startYaw = 0.0F;
        this.startPitch = 0.0F;
    }

    public void tagCombat(long durationTicks) {
        this.combatTagUntil = System.currentTimeMillis() + (durationTicks * 50);

        if (postCombatAnalysisTask != null) {
            postCombatAnalysisTask.cancel();
        }

        postCombatAnalysisTask = SnapAI.getInstance().getServer().getScheduler().runTaskLaterAsynchronously(
                SnapAI.getInstance(),
                () -> {
                    if (this.isRecording()) return;

                    int framesToAnalyze = SnapAI.getInstance().getConfig().getInt("ml-check.frames-to-analyze", 150);
                    if (this.getFrames().size() >= framesToAnalyze) {
                        AnalysisService.analyze(this);
                    }
                    this.getFrames().clear();
                },
                durationTicks
        );
    }

    public void addVerdict(double probability) {
        synchronized (recentVerdicts) {
            recentVerdicts.add(new Verdict(probability));
            if (recentVerdicts.size() > 5) {
                recentVerdicts.removeFirst();
            }
        }
    }

    public List<Verdict> getRecentVerdicts() {
        synchronized (recentVerdicts) {
            return new ArrayList<>(recentVerdicts);
        }
    }
}