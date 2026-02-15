package make.my.snap.service;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import make.my.snap.check.impl.aim.*;
import make.my.snap.check.impl.aura.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import make.my.snap.SnapAI;
import make.my.snap.check.APlayer;
import make.my.snap.check.Check;
import make.my.snap.check.PacketCheck;
import make.my.snap.check.util.PacketUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {
    private final SnapAI plugin;
    private final Map<UUID, List<Check>> packetChecks = new ConcurrentHashMap<>();

    public CheckManager(SnapAI plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    public void registerChecks(Player player) {
        APlayer aPlayer = new APlayer(player);
        List<Check> playerChecks = new ArrayList<>();

        playerChecks.add(aPlayer.actionData);
        playerChecks.add(aPlayer.rotationData);
        playerChecks.add(aPlayer.packetData);

        playerChecks.add(new AimA(aPlayer));
        playerChecks.add(new AimB(aPlayer));
        playerChecks.add(new AimC(aPlayer));
        playerChecks.add(new AimD(aPlayer));
        playerChecks.add(new AimE(aPlayer));
        playerChecks.add(new AimF(aPlayer));
        playerChecks.add(new AimH(aPlayer));
        playerChecks.add(new AimI(aPlayer));
        playerChecks.add(new AimJ(aPlayer));
        playerChecks.add(new AimK(aPlayer));

        playerChecks.add(new AuraA(aPlayer));
        playerChecks.add(new AuraB(aPlayer));
        playerChecks.add(new AuraC(aPlayer));
        playerChecks.add(new AuraS(aPlayer));
        playerChecks.add(new AuraN(aPlayer));

        for (Check check : playerChecks) {
            check.resetViolations();
        }

        packetChecks.put(player.getUniqueId(), playerChecks);
    }

    public void unregisterChecks(Player player) {
        List<Check> playerChecks = packetChecks.remove(player.getUniqueId());
        if (playerChecks != null) {
            for (Check check : playerChecks) {
                check.resetViolations();
                check.cancelDecayTask();
            }
        }
    }

    public void reloadChecks() {
        for (List<Check> checks : packetChecks.values()) {
            if (checks == null) continue;
            for (Check check : checks) {
                check.onReload();
            }
        }
    }

    public List<Check> getChecks(Player player) {
        return packetChecks.getOrDefault(player.getUniqueId(), Collections.emptyList());
    }

    public void handlePacketReceive(Player player, PacketReceiveEvent event) {
        List<Check> checks = packetChecks.get(player.getUniqueId());
        if (checks == null || checks.isEmpty()) return;

        for (Check check : checks) {
            if (check instanceof PacketCheck) {
                ((PacketCheck) check).onPacketReceive(event);
            }
        }

        if (PacketUtil.isAttack(event)) {
            for (Check check : checks) {
                if (check.hitTicksToCancel > 0) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    public void handlePacketSend(Player player, PacketSendEvent event) {
        List<Check> checks = packetChecks.get(player.getUniqueId());
        if (checks == null || checks.isEmpty()) return;

        for (Check check : checks) {
            if (check instanceof PacketCheck) {
                ((PacketCheck) check).onPacketSend(event);
            }
        }
    }

    private void startTickTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (List<Check> checks : packetChecks.values()) {
                for (Check check : checks) {
                    if (check.hitTicksToCancel > 0) {
                        check.hitTicksToCancel--;
                    }
                }
            }
        }, 1L, 1L);
    }
}
