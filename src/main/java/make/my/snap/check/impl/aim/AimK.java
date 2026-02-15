package make.my.snap.check.impl.aim;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.PacketUtil;

public class AimK extends Check implements PacketCheck {
    public AimK(APlayer aPlayer) {
        super("AimK", aPlayer);
    }

    private int ticks;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat() || aPlayer.bukkitPlayer.isInsideVehicle()) return;

        if (PacketUtil.isRotation(event)) {
            double pitch = Math.abs(aPlayer.rotationData.pitch);
            ticks++;

            if (pitch > 90 && ticks > 20) {
                flag(String.format("pitch=%.5f", pitch));
                ticks = 0;
            }
        }
    }
}
