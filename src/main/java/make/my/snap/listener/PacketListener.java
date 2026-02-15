package make.my.snap.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player; // Убрал лишний импорт Entity
import make.my.snap.SnapAI;
import make.my.snap.entity.Frame;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;
import make.my.snap.service.AnalysisService;
import make.my.snap.util.MouseCalculator;

import java.util.List;

public class PacketListener extends PacketListenerAbstract {

    private final boolean isMlCheckEnabled;
    private final int framesToAnalyze;
    private final SnapAI plugin;
    private final long combatTimerTicks;

    public PacketListener() {
        this.plugin = SnapAI.getInstance();
        this.isMlCheckEnabled = plugin.getConfig().getBoolean("ml-check.enabled", false);
        this.framesToAnalyze = plugin.getConfig().getInt("ml-check.frames-to-analyze", 150);
        this.combatTimerTicks = plugin.getConfig().getLong("ml-check.combat-timer-seconds", 3) * 20;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        User user = event.getUser();
        if (user == null || user.getUUID() == null) return;

        Player player = Bukkit.getPlayer(user.getUUID());
        if (player != null && plugin.getCheckManager() != null) {
            plugin.getCheckManager().handlePacketReceive(player, event);
            if (event.isCancelled()) return;
        }

        if (!isMlCheckEnabled) return;

        PlayerEntity entity = PlayerRegistry.getPlayer(user.getUUID());
        if (entity == null) return;

        // === ГЛАВНАЯ ЛОГИКА СБОРА ДАННЫХ ===
        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) {

            boolean isRecording = entity.isRecording();

            // Если идет сбор датасета -> PacketListener ВООБЩЕ не лезет в фреймы.
            // Всю математику и сохранение в файл берет на себя RotationData (или другой сервис).
            if (entity.isInCombat() && !isRecording) {
                WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);

                float lastYaw = MouseCalculator.normalizeAngle(entity.getLastYaw());
                float lastPitch = MouseCalculator.normalizeAngle(entity.getLastPitch());
                float currentYaw;
                float currentPitch;

                if (flying.hasRotationChanged()) {
                    currentYaw = MouseCalculator.normalizeAngle(flying.getLocation().getYaw());
                    currentPitch = MouseCalculator.normalizeAngle(flying.getLocation().getPitch());
                    updateLastLocation(entity, flying.getLocation());
                } else {
                    currentYaw = lastYaw;
                    currentPitch = lastPitch;
                }

                float deltaYaw = currentYaw - lastYaw;
                float deltaPitch = currentPitch - lastPitch;

                float accelYaw = MouseCalculator.calculateAcceleration(deltaYaw, entity.getLastDeltaYaw());
                float accelPitch = MouseCalculator.calculateAcceleration(deltaPitch, entity.getLastDeltaPitch());
                float jerkYaw = MouseCalculator.calculateJerk(accelYaw, entity.getLastAccelYaw());
                float jerkPitch = MouseCalculator.calculateJerk(accelPitch, entity.getLastAccelPitch());
                float gcdErrorYaw = MouseCalculator.calculateGCDError(deltaYaw);
                float gcdErrorPitch = MouseCalculator.calculateGCDError(deltaPitch);

                Frame frame = new Frame(deltaYaw, deltaPitch, accelYaw, accelPitch, jerkYaw, jerkPitch, gcdErrorYaw, gcdErrorPitch);

                entity.setLastDeltaYaw(deltaYaw);
                entity.setLastDeltaPitch(deltaPitch);
                entity.setLastAccelYaw(accelYaw);
                entity.setLastAccelPitch(accelPitch);

                List<Frame> frames = entity.getFrames();
                frames.add(frame);

                if (frames.size() >= framesToAnalyze) {
                    // --- ФИЛЬТР AFK ДЛЯ ЛАЙВ-АНАЛИЗА ---
                    int zeroFramesCount = 0;

                    for (Frame f : frames) {
                        if (Math.abs(f.deltaX) < 0.001 && Math.abs(f.deltaY) < 0.001) {
                            zeroFramesCount++;
                        }
                    }

                    if (zeroFramesCount > 130) {
                        frames.clear();
                    } else {
                        AnalysisService.analyze(entity);
                        frames.clear();
                    }
                }
            }
        }

        // === ЛОГИКА БОЯ ===
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                Player attackPlayer = Bukkit.getPlayer(user.getUUID());
                if (attackPlayer != null) {
                    plugin.getMitigationManager().handlePacketAttack(attackPlayer, event);
                    if (event.isCancelled()) return;

                    // --- ФИЛЬТР: ТОЛЬКО ИГРОКИ ---
                    int targetId = interact.getEntityId();
                    boolean isTargetPlayer = false;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getEntityId() == targetId) {
                            isTargetPlayer = true;
                            break;
                        }
                    }

                    // Если бьем не игрока — выходим и таймер не обновляем
                    if (!isTargetPlayer) return;

                    // Обновляем таймер ТОЛЬКО ДЛЯ АТАКУЮЩЕГО
                    entity.tagCombat(combatTimerTicks);
                }
            }
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        User user = event.getUser();
        if (user == null || user.getUUID() == null) return;
        Player player = Bukkit.getPlayer(user.getUUID());
        if (player != null && plugin.getCheckManager() != null) {
            plugin.getCheckManager().handlePacketSend(player, event);
        }
    }

    private void updateLastLocation(PlayerEntity entity, com.github.retrooper.packetevents.protocol.world.Location location) {
        entity.setLastYaw(MouseCalculator.normalizeAngle(location.getYaw()));
        entity.setLastPitch(MouseCalculator.normalizeAngle(location.getPitch()));
    }
}