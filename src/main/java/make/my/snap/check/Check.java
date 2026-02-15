package make.my.snap.check;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import make.my.snap.SnapAI;
import make.my.snap.check.util.Config;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;

public abstract class Check {
    private final String name;
    protected final APlayer aPlayer;
    private final Plugin plugin;

    private boolean enabled;
    private boolean alert;
    private final boolean experimental;

    private int violations;
    private int maxViolations;
    private int decay;
    public int hitCancelTicks;
    public int hitTicksToCancel;

    private long delayTaskRawDelay;
    private BukkitTask decayTask;

    public Check(String name, APlayer aPlayer) {
        this.name = name;
        this.aPlayer = aPlayer;
        this.plugin = SnapAI.getInstance();
        this.experimental = name.contains("*");
        this.hitTicksToCancel = 0;

        loadValues();
        startDecayTask();
    }

    public void onReload() {
        cancelDecayTask();
        loadValues();
        startDecayTask();
    }

    protected void loadValues() {
        this.enabled = Config.getBoolean(getConfigPath() + ".enabled", true);
        this.alert = Config.getBoolean(getConfigPath() + ".alert", true);
        this.maxViolations = Config.getInt(getConfigPath() + ".max-violations", 10);
        this.hitCancelTicks = Config.getInt(getConfigPath() + ".hit-cancel-ticks", 0);
        this.decay = Config.getInt(getConfigPath() + ".decay", 1);
        this.delayTaskRawDelay = Config.getInt(getConfigPath() + ".remove-violations-after", 300);
    }

    protected void flag() {
        flag("");
    }

    protected void flag(String verbose) {
        if (!enabled) return;

        if (!experimental) {
            this.hitTicksToCancel += hitCancelTicks;
        }

        if (violations < maxViolations) {
            violations++;
            aPlayer.globalVl++;
            aPlayer.kaNpcVl++;
        }

        PlayerEntity entity = PlayerRegistry.getPlayer(aPlayer.getBukkitPlayer().getUniqueId());
        if (entity != null) {
            SnapAI.getInstance().getViolationManager().handleLegacyViolation(entity, name);
        }
    }

    private void startDecayTask() {
        long delayTicks = delayTaskRawDelay * 20L;

        this.decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (violations > 0) {
                    violations -= decay;
                    if (violations < 0) violations = 0;
                }
            }
        }.runTaskTimer(plugin, delayTicks, delayTicks);
    }

    public void cancelDecayTask() {
        if (decayTask != null && !decayTask.isCancelled()) {
            decayTask.cancel();
            decayTask = null;
        }
    }

    public void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public void resetViolations() {
        this.violations = 0;
        aPlayer.globalVl = 0;
    }

    public String getName() { return name; }
    public boolean isEnabled() { return enabled; }
    public String getConfigPath() { return "checks." + name; }
    public boolean alert() { return alert; }
    public int getViolations() { return violations; }
    public int getMaxViolations() { return maxViolations; }
}