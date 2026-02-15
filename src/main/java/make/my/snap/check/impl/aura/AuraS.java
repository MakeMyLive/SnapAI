package make.my.snap.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.PlayerIdRegistry;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.PacketUtil;

public class AuraS extends Check implements PacketCheck {
    public AuraS(APlayer aPlayer) {
        super("AuraS", aPlayer);
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 6);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.6);
        this.fovThreshold = Config.getDouble(getConfigPath() + ".fov-threshold", 155.0);
        this.nearFovThreshold = Config.getDouble(getConfigPath() + ".near-fov-threshold", 110.0);
        this.rotationStaleMs = Config.getInt(getConfigPath() + ".rotation-stale-ms", 350);
        this.minDeltaYaw = Config.getDouble(getConfigPath() + ".min-delta-yaw", 0.4D);
        this.minTargetMovement = Config.getDouble(getConfigPath() + ".min-target-movement", 0.08);
        this.consecutiveRequired = Config.getInt(getConfigPath() + ".consecutive-required", 4);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;
    private double fovThreshold;
    private double nearFovThreshold;
    private long rotationStaleMs;
    private double minDeltaYaw;
    private double minTargetMovement;
    private int consecutiveRequired;

    private long lastRotationAt;
    private Location lastTargetLocation;
    private int consecutiveSuspicious;
    private int lastTargetId = -1;
    private long lastAttackTime;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (PacketUtil.isRotation(event)) {
            lastRotationAt = System.currentTimeMillis();
            if (Math.abs(aPlayer.rotationData.deltaYaw) > minDeltaYaw * 2) {
                consecutiveSuspicious = Math.max(0, consecutiveSuspicious - 2);
            }
            return;
        }

        if (PacketUtil.isAttack(event)) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            Entity target = resolveTarget(wrapper.getEntityId());
            if (!(target instanceof LivingEntity)) return;

            LivingEntity livingTarget = (LivingEntity) target;
            Location currentTargetLoc = livingTarget.getLocation();
            long now = System.currentTimeMillis();

            if (lastTargetId != wrapper.getEntityId()) {
                lastTargetId = wrapper.getEntityId();
                lastTargetLocation = currentTargetLoc.clone();
                consecutiveSuspicious = 0;
                lastAttackTime = now;
                return;
            }

            long attackInterval = now - lastAttackTime;
            lastAttackTime = now;

            if (attackInterval > 1500) {
                consecutiveSuspicious = 0;
                lastTargetLocation = currentTargetLoc.clone();
                return;
            }

            double targetMovement = 0;
            if (lastTargetLocation != null && lastTargetLocation.getWorld() == currentTargetLoc.getWorld()) {
                targetMovement = lastTargetLocation.distance(currentTargetLoc);
            }
            lastTargetLocation = currentTargetLoc.clone();

            if (targetMovement < minTargetMovement) {
                buffer = Math.max(0, buffer - bufferDecrease * 0.5);
                consecutiveSuspicious = Math.max(0, consecutiveSuspicious - 1);
                return;
            }

            double distance = aPlayer.bukkitPlayer.getLocation().distance(currentTargetLoc);
            if (distance > 6.0 || distance < 0.5) {
                return;
            }

            double yawDiff = calculateYawDifference(livingTarget);
            long sinceRot = lastRotationAt == 0 ? Long.MAX_VALUE : now - lastRotationAt;
            double deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);

            double adjustedFovThreshold = fovThreshold;
            double adjustedNearThreshold = nearFovThreshold;
            if (distance < 1.5) {
                adjustedFovThreshold = 175.0;
                adjustedNearThreshold = 140.0;
            } else if (distance < 2.5) {
                adjustedFovThreshold = 165.0;
                adjustedNearThreshold = 125.0;
            } else if (distance < 3.5) {
                adjustedFovThreshold = 160.0;
                adjustedNearThreshold = 115.0;
            }

            boolean staleRotation = sinceRot > rotationStaleMs && deltaYaw < minDeltaYaw;
            boolean veryStaleRotation = sinceRot > rotationStaleMs * 2 && deltaYaw < minDeltaYaw * 0.5;

            if (yawDiff > adjustedFovThreshold && staleRotation && targetMovement > minTargetMovement * 1.5) {
                consecutiveSuspicious++;
                if (consecutiveSuspicious >= consecutiveRequired) {
                    double weight = Math.min(2.5, (yawDiff - adjustedFovThreshold) / 25.0);
                    buffer += 0.4 + weight;
                    if (buffer > maxBuffer) {
                        flag(String.format("diff=%.2f rot=%.0fms dy=%.2f cons=%d dist=%.2f tm=%.3f",
                                yawDiff, (double) sinceRot, deltaYaw, consecutiveSuspicious, distance, targetMovement));
                        buffer = maxBuffer * 0.4;
                        consecutiveSuspicious = consecutiveRequired / 2;
                    }
                }
            } else if (yawDiff > adjustedNearThreshold && veryStaleRotation && targetMovement > minTargetMovement * 2) {
                consecutiveSuspicious++;
                if (consecutiveSuspicious >= consecutiveRequired + 2) {
                    double weight = Math.min(1.5, (yawDiff - adjustedNearThreshold) / 20.0);
                    buffer += 0.25 + weight;
                    if (buffer > maxBuffer) {
                        flag(String.format("ndiff=%.2f rot=%.0fms dy=%.2f cons=%d dist=%.2f tm=%.3f",
                                yawDiff, (double) sinceRot, deltaYaw, consecutiveSuspicious, distance, targetMovement));
                        buffer = maxBuffer * 0.4;
                        consecutiveSuspicious = consecutiveRequired / 2;
                    }
                }
            } else {
                consecutiveSuspicious = Math.max(0, consecutiveSuspicious - 1);
                if (buffer > 0) {
                    buffer = Math.max(0, buffer - bufferDecrease);
                }
            }
        }
    }

    private Entity resolveTarget(int entityId) {
        return PlayerIdRegistry.get(entityId);
    }

    private double calculateYawDifference(LivingEntity target) {
        Location eye = aPlayer.bukkitPlayer.getEyeLocation();
        Vector toTarget = target.getLocation().add(0, target.getHeight() * 0.5, 0).toVector().subtract(eye.toVector());
        double yawToTarget = Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX())) - 90.0;
        double yaw = aPlayer.rotationData.yaw;
        double diff = yawToTarget - yaw;
        diff = (diff + 540.0) % 360.0 - 180.0;
        return Math.abs(diff);
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 6);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.6);
        this.fovThreshold = Config.getDouble(getConfigPath() + ".fov-threshold", 155.0);
        this.nearFovThreshold = Config.getDouble(getConfigPath() + ".near-fov-threshold", 110.0);
        this.rotationStaleMs = Config.getInt(getConfigPath() + ".rotation-stale-ms", 350);
        this.minDeltaYaw = Config.getDouble(getConfigPath() + ".min-delta-yaw", 0.4D);
        this.minTargetMovement = Config.getDouble(getConfigPath() + ".min-target-movement", 0.08);
        this.consecutiveRequired = Config.getInt(getConfigPath() + ".consecutive-required", 4);
    }
}