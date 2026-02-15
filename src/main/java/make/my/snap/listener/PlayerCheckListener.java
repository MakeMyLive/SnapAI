package make.my.snap.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import make.my.snap.SnapAI;
import make.my.snap.check.PlayerIdRegistry;

public class PlayerCheckListener implements Listener {

    private final SnapAI plugin;

    public PlayerCheckListener(SnapAI plugin) {
        this.plugin = plugin;
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerIdRegistry.add(player);
            plugin.getCheckManager().registerChecks(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerIdRegistry.add(player);
        plugin.getCheckManager().registerChecks(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getCheckManager().unregisterChecks(player);
        PlayerIdRegistry.remove(player);
    }
}

