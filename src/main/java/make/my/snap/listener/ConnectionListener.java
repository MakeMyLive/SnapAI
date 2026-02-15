package make.my.snap.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.event.UserLoginEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import make.my.snap.SnapAI;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;

public class ConnectionListener extends PacketListenerAbstract {

    @Override
    public void onUserLogin(UserLoginEvent event) {
        User user = event.getUser();
        if (user != null && user.getUUID() != null && user.getName() != null) {
            PlayerEntity entity = new PlayerEntity(user.getUUID(), user.getName());
            PlayerRegistry.addPlayer(user.getUUID(), entity);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        if (user != null && user.getUUID() != null) {
            PlayerRegistry.removePlayer(user.getUUID());
            SnapAI.getInstance().getViolationManager().clearPlayerData(user.getUUID());
        }
    }
}