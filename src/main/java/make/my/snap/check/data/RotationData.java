package make.my.snap.check.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import make.my.snap.SnapAI;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.stats.Statistics;
import make.my.snap.check.util.GraphUtil;
import make.my.snap.entity.Frame;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;
import make.my.snap.util.MouseCalculator;

import java.util.*;

public class RotationData extends Check implements PacketCheck {

    public float yaw, pitch, lastYaw, lastPitch;
    public float deltaYaw, deltaPitch, lastDeltaYaw, lastDeltaPitch;
    public float lastLastDeltaYaw, lastLastDeltaPitch;

    private long lastSmooth = 0L, lastHighRate = 0L;
    private double lastDeltaXRot = 0.0, lastDeltaYRot = 0.0;
    private final List<Double> yawSamples = new ArrayList<>();
    private final List<Double> pitchSamples = new ArrayList<>();
    private boolean cinematicRotation = false;
    private int isTotallyNotCinematic = 0;

    public RotationData(APlayer aPlayer) {
        super("RotationData", aPlayer);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {
            WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);

            float nextYaw;
            float nextPitch;

            if (wrapper.hasRotationChanged()) {
                nextYaw = wrapper.getLocation().getYaw();
                nextPitch = wrapper.getLocation().getPitch();
            } else {
                nextYaw = this.yaw;
                nextPitch = this.pitch;
            }

            updateRotation(nextYaw, nextPitch);
        }
    }

    private void updateRotation(float newYaw, float newPitch) {
        lastYaw = yaw;
        lastPitch = pitch;

        yaw = MouseCalculator.normalizeAngle(newYaw);
        pitch = newPitch;

        lastLastDeltaYaw = lastDeltaYaw;
        lastLastDeltaPitch = lastDeltaPitch;

        lastDeltaYaw = deltaYaw;
        lastDeltaPitch = deltaPitch;

        deltaYaw = yaw - lastYaw;
        deltaPitch = pitch - lastPitch;

        processAISampling();
        processCinematic();
    }

    private void processAISampling() {
        PlayerEntity entity = PlayerRegistry.getPlayer(aPlayer.getBukkitPlayer().getUniqueId());
        if (entity == null || !entity.isRecording()) return;

        float finalDeltaX = deltaYaw;
        float finalDeltaY = deltaPitch;

        float accelX = MouseCalculator.calculateAcceleration(finalDeltaX, lastDeltaYaw);
        float accelY = MouseCalculator.calculateAcceleration(finalDeltaY, lastDeltaPitch);

        float entityLastAccelX = entity.getLastAccelYaw();
        float entityLastAccelY = entity.getLastAccelPitch();

        float finalJerkX = MouseCalculator.calculateJerk(accelX, entityLastAccelX);
        float finalJerkY = MouseCalculator.calculateJerk(accelY, entityLastAccelY);

        entity.setLastAccelYaw(accelX);
        entity.setLastAccelPitch(accelY);

        float gcdX = MouseCalculator.calculateGCDError(finalDeltaX);
        float gcdY = MouseCalculator.calculateGCDError(finalDeltaY);

        float relX = yaw - entity.getStartYaw();
        float relY = pitch - entity.getStartPitch();

        Frame currentFrame = new Frame(relX, relY, finalDeltaX, finalDeltaY, finalJerkX, finalJerkY, gcdX, gcdY);
        entity.getFrames().add(currentFrame);

        int currentSize = entity.getFrames().size();

        if (entity.getRecorderUUID() != null) {
            Player recorder = Bukkit.getPlayer(entity.getRecorderUUID());
            if (recorder != null && recorder.isOnline()) {
                String msg = String.format("§aЗапись §f%s§7: §e%d§7/150 §8| §bОсталось сетов: %d",
                        entity.getName(), currentSize, entity.getSetsToCollect());
                recorder.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            }
        }

        if (currentSize >= 150) {
            SnapAI.getInstance().getLocalDatasetService().saveDatasetLocally(entity);
            entity.getFrames().clear();
            entity.decrementSets();

            if (entity.getSetsToCollect() <= 0) {
                entity.stopRecording();
                if (entity.getRecorderUUID() != null) {
                    Player rec = Bukkit.getPlayer(entity.getRecorderUUID());
                    if (rec != null) {
                        rec.sendMessage("§6[SnapAI] Сбор всех сетов для " + entity.getName() + " успешно завершен!");
                    }
                }
            }
        }
    }

    private void processCinematic() {
        long now = System.currentTimeMillis();

        double differenceYaw = Math.abs(deltaYaw - lastDeltaXRot);
        double differencePitch = Math.abs(deltaPitch - lastDeltaYRot);

        double joltYaw = Math.abs(differenceYaw - deltaYaw);
        double joltPitch = Math.abs(differencePitch - deltaPitch);

        boolean cinematic = (now - lastHighRate > 250L) || (now - lastSmooth < 9000L);

        if (joltYaw > 1.0 && joltPitch > 1.0) {
            lastHighRate = now;
        }

        yawSamples.add((double) deltaYaw);
        pitchSamples.add((double) deltaPitch);

        if (yawSamples.size() >= 20 && pitchSamples.size() >= 20) {
            Set<Double> shannonYaw = new HashSet<>();
            Set<Double> shannonPitch = new HashSet<>();
            List<Double> stackYaw = new ArrayList<>();
            List<Double> stackPitch = new ArrayList<>();

            for (Double yawSample : yawSamples) {
                stackYaw.add(yawSample);
                if (stackYaw.size() >= 10) {
                    shannonYaw.add(Statistics.getShannonEntropy(stackYaw));
                    stackYaw.clear();
                }
            }

            for (Double pitchSample : pitchSamples) {
                stackPitch.add(pitchSample);
                if (stackPitch.size() >= 10) {
                    shannonPitch.add(Statistics.getShannonEntropy(stackPitch));
                    stackPitch.clear();
                }
            }

            if (shannonYaw.size() != 1 || shannonPitch.size() != 1 ||
                    !shannonYaw.toArray()[0].equals(shannonPitch.toArray()[0])) {
                isTotallyNotCinematic = 20;
            }

            GraphUtil.GraphResult resultsYaw = GraphUtil.getGraph(yawSamples);
            GraphUtil.GraphResult resultsPitch = GraphUtil.getGraph(pitchSamples);

            int positivesYaw = resultsYaw.getPositives();
            int negativesYaw = resultsYaw.getNegatives();
            int positivesPitch = resultsPitch.getPositives();
            int negativesPitch = resultsPitch.getNegatives();

            if (positivesYaw > negativesYaw || positivesPitch > negativesPitch) {
                lastSmooth = now;
            }

            yawSamples.clear();
            pitchSamples.clear();
        }

        if (isTotallyNotCinematic > 0) {
            isTotallyNotCinematic--;
            cinematicRotation = false;
        } else {
            cinematicRotation = cinematic;
        }

        lastDeltaXRot = deltaYaw;
        lastDeltaYRot = deltaPitch;
    }

    public boolean isCinematicRotation() {
        return cinematicRotation;
    }
}