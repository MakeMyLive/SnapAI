package make.my.snap.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.PacketUtil;

public class AimI extends Check implements PacketCheck {
    public AimI(APlayer aPlayer) {
        super("AimI", aPlayer);
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
        minDelta = Config.getDouble(getConfigPath() + ".min-delta", 0.35);
        attackWindowMs = Config.getInt(getConfigPath() + ".attack-window-ms", 650);
    }

    private int yawStreak;
    private int pitchStreak;
    private double buffer1;
    private double buffer2;
    private double maxBuffer;
    private double bufferDecrease;
    private double minDelta;
    private long attackWindowMs;
    private static final double DELTA_EPSILON = 0.0005;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation()) return;

        if (PacketUtil.isRotation(event)) {
            if (!aPlayer.actionData.hasAttackedSince(attackWindowMs)) {
                resetStreaks();
                decayBuffers();
                return;
            }

            float deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            float deltaPitch = Math.abs(aPlayer.rotationData.deltaPitch);
            float lastDeltaYaw = Math.abs(aPlayer.rotationData.lastDeltaYaw);
            float lastDeltaPitch = Math.abs(aPlayer.rotationData.lastDeltaPitch);

            boolean smallMotion = deltaYaw < minDelta && deltaPitch < minDelta;
            if (smallMotion) {
                resetStreaks();
                decayBuffers();
                return;
            }

            boolean stableYaw = deltaYaw >= minDelta && Math.abs(deltaYaw - lastDeltaYaw) <= DELTA_EPSILON;
            if (stableYaw) {
                yawStreak++;
                if (yawStreak > 2) {
                    buffer1++;
                    if (buffer1 > maxBuffer) {
                        flag(String.format("streakYaw=%d", yawStreak));
                        buffer1 = 0;
                    }
                } else {
                    if (buffer1 > 0) buffer1 -= bufferDecrease;
                }
            } else {
                yawStreak = 0;
            }

            boolean stablePitch = deltaPitch >= minDelta && Math.abs(deltaPitch - lastDeltaPitch) <= DELTA_EPSILON;
            if (stablePitch) {
                pitchStreak++;
                if (pitchStreak > 2) {
                    buffer2++;
                    if (buffer2 > maxBuffer) {
                        flag(String.format("streakPitch=%d", pitchStreak));
                        buffer2 = 0;
                    }
                    pitchStreak = 0;
                } else {
                    if (buffer2 > 0) buffer2 -= bufferDecrease;
                }
            } else {
                pitchStreak = 0;
            }
        }
    }

    @Override
    public void onReload() {
        maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.05);
        minDelta = Config.getDouble(getConfigPath() + ".min-delta", 0.35);
        attackWindowMs = Config.getInt(getConfigPath() + ".attack-window-ms", 650);
    }

    private void decayBuffers() {
        if (buffer1 > 0) buffer1 = Math.max(0, buffer1 - bufferDecrease);
        if (buffer2 > 0) buffer2 = Math.max(0, buffer2 - bufferDecrease);
    }

    private void resetStreaks() {
        yawStreak = 0;
        pitchStreak = 0;
    }
}
