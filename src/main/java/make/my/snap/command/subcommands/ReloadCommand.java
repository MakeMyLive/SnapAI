package make.my.snap.command.subcommands;

import org.bukkit.command.CommandSender;
import make.my.snap.SnapAI;
import make.my.snap.command.SubCommand;

public class ReloadCommand extends SubCommand {

    public ReloadCommand(SnapAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.reload.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.reload.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snapai.admin")) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return;
        }

        plugin.saveDefaultConfig();

        plugin.reloadConfig();

        plugin.getLocaleManager().reload();

        if (plugin.getCheckManager() != null) {
            plugin.getCheckManager().reloadChecks();
        }

        sender.sendMessage(plugin.getLocaleManager().getMessage("commands.reload.success"));
    }
}