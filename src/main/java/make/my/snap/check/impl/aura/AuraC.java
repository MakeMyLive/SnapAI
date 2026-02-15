package make.my.snap.check.impl.aura;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.Config;
import make.my.snap.check.util.EvictingList;
import make.my.snap.check.util.PacketUtil;

public class AuraC extends Check implements PacketCheck {
    public AuraC(APlayer player) {
        super("AuraC", player);
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 1);
    }

    private final EvictingList<Long> attackDelays = new EvictingList<>(10);
    private long lastAttackTime;
    private double buffer;
    private double maxBuffer;
    private double bufferDecrease;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isEnabled() || !aPlayer.actionData.inCombat()) return;

        if (PacketUtil.isAttack(event)) {
            long now = System.currentTimeMillis();
            if (lastAttackTime > 0) {
                long delay = now - lastAttackTime;
                attackDelays.add(delay);

                if (attackDelays.isFull()) {
                    if (attackDelays.allEqual()) {
                        buffer++;
                        if (buffer > maxBuffer) {
                            flag();
                            buffer = 0;
                        }
                    } else if (buffer > 0) {
                        buffer -= bufferDecrease;
                    }
                }
            }
            lastAttackTime = now;
        }
    }

    @Override
    public void onReload() {
        this.maxBuffer = Config.getDouble(getConfigPath() + ".max-buffer", 1);
        this.bufferDecrease = Config.getDouble(getConfigPath() + ".buffer-decrease", 1);
    }
}
