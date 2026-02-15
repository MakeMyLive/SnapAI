package make.my.snap.service;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import make.my.snap.SnapAI;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {

    private final SnapAI plugin;
    private final Map<UUID, UUID> activeHuds = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> entityIds = new ConcurrentHashMap<>();

    public HologramManager(SnapAI plugin) {
        this.plugin = plugin;
        startTickingTask();
    }

    public void toggleHud(Player admin, Player target) {
        if (activeHuds.containsKey(admin.getUniqueId())) {
            removeHologram(admin);
            activeHuds.remove(admin.getUniqueId());

            String msg = plugin.getLocaleManager().getMessage("commands.hud.disabled");
            admin.sendMessage(msg);
        } else {
            int entityId = SpigotReflectionUtil.generateEntityId();
            activeHuds.put(admin.getUniqueId(), target.getUniqueId());
            entityIds.put(target.getUniqueId(), entityId);

            spawnHologram(admin, target, entityId);

            String msg = plugin.getLocaleManager().getMessage("commands.hud.enabled");
            admin.sendMessage(msg.replace("%target%", target.getName()));
        }
    }

    private void spawnHologram(Player admin, Player target, int entityId) {
        double offset = plugin.getConfig().getDouble("hologram.height-offset", 2.3);
        Location loc = target.getLocation().add(0, offset, 0);

        User adminUser = PacketEvents.getAPI().getPlayerManager().getUser(admin);

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ARMOR_STAND,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(),
                loc.getYaw(),
                0, 0, Optional.empty()
        );
        adminUser.sendPacket(spawnPacket);

        updateHologramText(admin, target, entityId);
    }

    private void removeHologram(Player admin) {
        UUID targetUUID = activeHuds.get(admin.getUniqueId());
        if (targetUUID == null || !entityIds.containsKey(targetUUID)) return;

        int entityId = entityIds.get(targetUUID);
        User adminUser = PacketEvents.getAPI().getPlayerManager().getUser(admin);

        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(new int[]{entityId});
        adminUser.sendPacket(destroyPacket);
    }

    public void updateTextFor(UUID targetUUID) {
        for (Map.Entry<UUID, UUID> entry : activeHuds.entrySet()) {
            if (entry.getValue().equals(targetUUID)) {
                Player admin = Bukkit.getPlayer(entry.getKey());
                Player target = Bukkit.getPlayer(targetUUID);

                if (admin != null && target != null && entityIds.containsKey(targetUUID)) {
                    updateHologramText(admin, target, entityIds.get(targetUUID));
                }
            }
        }
    }

    private void updateHologramText(Player admin, Player target, int entityId) {
        PlayerEntity entity = PlayerRegistry.getPlayer(target.getUniqueId());
        if (entity == null) return;

        StringBuilder text = new StringBuilder();
        List<PlayerEntity.Verdict> verdicts = entity.getRecentVerdicts();

        int count = 0;
        for (PlayerEntity.Verdict v : verdicts) {
            double val = v.probability * 100.0;
            String color = val > 90 ? "§4" : (val > 50 ? "§c" : (val > 20 ? "§e" : "§a"));
            text.append(color).append(String.format("%.1f%% ", val));
            count++;
            if (count >= 5) break;
        }

        if (text.length() == 0) text.append("§7Ожидание данных...");

        Component nameComponent = LegacyComponentSerializer.legacySection().deserialize(text.toString());
        String jsonName = GsonComponentSerializer.gson().serialize(nameComponent);
        List<com.github.retrooper.packetevents.protocol.entity.data.EntityData> data = new ArrayList<>();

        data.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                0,
                com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                (byte) 0x20
        ));

        data.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                2,
                com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_COMPONENT,
                Optional.of(jsonName)
        ));

        data.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                3,
                com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN,
                true
        ));

        @SuppressWarnings({"unchecked", "rawtypes"})
        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, (List) data);

        PacketEvents.getAPI().getPlayerManager().getUser(admin).sendPacket(metaPacket);
    }

    private void startTickingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            double offset = plugin.getConfig().getDouble("hologram.height-offset", 2.3);

            for (Map.Entry<UUID, UUID> entry : activeHuds.entrySet()) {
                Player admin = Bukkit.getPlayer(entry.getKey());
                Player target = Bukkit.getPlayer(entry.getValue());

                if (admin == null || target == null || !admin.isOnline() || !target.isOnline()) {
                    continue;
                }

                if (entityIds.containsKey(target.getUniqueId())) {
                    int entityId = entityIds.get(target.getUniqueId());

                    Location loc = target.getLocation().add(0, offset, 0);

                    WrapperPlayServerEntityTeleport tpPacket = new WrapperPlayServerEntityTeleport(
                            entityId,
                            new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                            loc.getYaw(),
                            loc.getPitch(),
                            false
                    );

                    PacketEvents.getAPI().getPlayerManager().getUser(admin).sendPacket(tpPacket);
                }
            }
        }, 1L, 1L);
    }
}