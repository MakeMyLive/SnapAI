package make.my.snap.mitigation.types;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import make.my.snap.SnapAI;

public class DamageReduction implements DamageMitigation {
    private final double multiplier;

    public DamageReduction(SnapAI plugin) {
        this.multiplier = plugin.getConfig().getDouble("mitigations.types.damage-reduction.multiplier", 0.7);
    }

    @Override
    public void onDamage(Player attacker, EntityDamageByEntityEvent event) {
        double newDamage = event.getDamage() * multiplier;
        event.setDamage(newDamage);
    }
}