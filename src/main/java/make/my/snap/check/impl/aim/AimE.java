package make.my.snap.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.PacketUtil;

public class AimE extends Check implements PacketCheck {
    public AimE(APlayer aPlayer) {
        super("AimE", aPlayer);
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 7);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }

    private double buffer1;
    private double buffer2;
    private double maxBuffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || aPlayer.bukkitPlayer.isInsideVehicle() || Math.abs(aPlayer.rotationData.pitch) == 90 || !aPlayer.actionData.inCombat() || aPlayer.rotationData.isCinematicRotation())
            return;

        if (PacketUtil.isRotation(event)) {
            float deltaYaw = Math.abs(aPlayer.rotationData.deltaYaw);
            float deltaPitch = Math.abs(aPlayer.rotationData.deltaPitch);

            if (deltaYaw > 3.5 && deltaPitch == 0) {
                buffer1++;
                if (buffer1 > maxBuffer) {
                    flag();
                    buffer1 = 0;
                }
            } else {
                if (buffer1 > 0) buffer1 -= bufferDecrease;
            }

            if (deltaPitch > 3.5 && deltaYaw == 0) {
                buffer2++;
                if (buffer2 > maxBuffer) {
                    flag("pitch>3.5 yaw==0");
                    buffer2 = 0;
                }
            } else {
                if (buffer2 > 0) buffer2 -= bufferDecrease;
            }
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getInt(getConfigPath() + ".max-buffer", 7);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }
}
