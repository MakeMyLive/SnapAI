package make.my.snap.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.PacketUtil;

public class AuraB extends Check implements PacketCheck {
    public AuraB(APlayer aPlayer) {
        super("AuraB", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }

    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;
    private double deltaYaw;
    private double lastDeltaYaw;
    private final double max = 60;
    private final double min = 3.2;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (PacketUtil.isAttack(event)) {
            deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            lastDeltaYaw = Math.abs(aPlayer.rotationData.lastDeltaYaw);

            if (deltaYaw > max && lastDeltaYaw < min) {
                buffer++;
                if (buffer > maxBuffer) {
                    flag(String.format("current=%.5f\nlast=%.5f", deltaYaw, lastDeltaYaw));
                    buffer = 0;
                }
            } else {
                if (buffer > 0) buffer -= bufferDecrease;
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.25);
    }
}
