package make.my.snap.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;

public class AuraN extends Check implements PacketCheck {

    private double maxBuffer;
    private double bufferDecrease;
    private double maxAngle;
    private double minDistance;
    private boolean debug;
    private double buffer = 0;

    public AuraN(APlayer aPlayer) {
        super("AuraN", aPlayer);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {

            if (!isEnabled()) return;

            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            int targetId = wrapper.getEntityId();

            runSync(() -> {
                performCheck(targetId);
            });
        }
    }

    private void performCheck(int targetId) {
        if (!aPlayer.bukkitPlayer.isOnline()) return;

        if (aPlayer.bukkitPlayer.getGameMode() == GameMode.CREATIVE ||
                aPlayer.bukkitPlayer.getGameMode() == GameMode.SPECTATOR) return;

        Entity target = null;

        for (Entity e : aPlayer.bukkitPlayer.getNearbyEntities(10, 10, 10)) {
            if (e.getEntityId() == targetId) {
                target = e;
                break;
            }
        }

        if (target == null) {
            if (debug) System.out.println("[AuraN] Target not found (Async/Sync issue?) ID: " + targetId);
            return;
        }

        checkSilentAura(target);
    }

    private void checkSilentAura(Entity target) {
        Location eyeLoc = aPlayer.bukkitPlayer.getEyeLocation();
        Vector direction = eyeLoc.getDirection();
        double distance = eyeLoc.distance(target.getLocation());

        if (distance < minDistance) return;

        BoundingBox box = target.getBoundingBox();
        BoundingBox expandedBox = box.expand(0.1);

        org.bukkit.util.RayTraceResult result = expandedBox.rayTrace(eyeLoc.toVector(), direction, 10.0);

        double deviation = 0.0;

        if (result != null) {
            deviation = 0.0;
        } else {
            Vector targetCenter = box.getCenter();
            Vector toTarget = targetCenter.subtract(eyeLoc.toVector());
            double angleToCenter = Math.toDegrees(direction.angle(toTarget));

            double size = Math.max(box.getWidthX(), box.getHeight());
            double angularSize = Math.toDegrees(Math.atan(size / 2.0 / distance));

            deviation = Math.max(0, angleToCenter - angularSize);
        }

        if (debug) {
            System.out.println("[AuraN] Dev: " + String.format("%.2f", deviation) + " | Buffer: " + buffer);
        }

        if (deviation > maxAngle) {
            buffer += 1.0 + (deviation / 20.0);

            if (buffer > maxBuffer) {
                flag("angle=" + String.format("%.1f", deviation) + " dist=" + String.format("%.1f", distance));
                buffer = Math.max(0, maxBuffer - 1.0);
            }
        } else {
            if (buffer > 0) buffer -= bufferDecrease;
        }
    }

    @Override
    public void onReload() {
        super.onReload();
    }

    @Override
    protected void loadValues() {
        super.loadValues();
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 5.0);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
        this.maxAngle = Config.getDouble(getConfigPath() + ".max-angle", 60.0);
        this.minDistance = Config.getDouble(getConfigPath() + ".min-distance", 1.5);
        this.debug = Config.getBoolean(getConfigPath() + ".debug", false);
    }
}