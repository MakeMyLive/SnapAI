package make.my.snap.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.PacketUtil;

public class AuraA extends Check implements PacketCheck {
    public AuraA(APlayer aPlayer) {
        super("AuraA", aPlayer);
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }

    private double maxBuffer;
    private double buffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (PacketUtil.isAttack(event)) {
            if (aPlayer.bukkitPlayer.isHandRaised()) {
                buffer++;
                if (buffer > maxBuffer) {
                    flag();
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
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 0.5);
    }
}
