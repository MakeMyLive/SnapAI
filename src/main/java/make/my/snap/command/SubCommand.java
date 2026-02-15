package make.my.snap.command;

import org.bukkit.command.CommandSender;
import make.my.snap.SnapAI;
import java.util.Collections;
import java.util.List;

public abstract class SubCommand {

    protected final SnapAI plugin;

    public SubCommand(SnapAI plugin) {
        this.plugin = plugin;
    }

    public abstract String getName();
    public abstract String getDescription();
    public abstract String getUsage();
    public abstract void execute(CommandSender sender, String[] args);

    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}