package make.my.snap.mitigation.types;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public interface DamageMitigation {

    void onDamage(Player attacker, EntityDamageByEntityEvent event);
}