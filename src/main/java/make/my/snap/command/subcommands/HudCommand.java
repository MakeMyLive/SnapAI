package make.my.snap.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import make.my.snap.SnapAI;
import make.my.snap.command.SubCommand;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HudCommand extends SubCommand {

    public HudCommand(SnapAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "hud";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.hud.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.hud.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("player-only-command"));
            return;
        }

        Player admin = (Player) sender;

        if (!admin.hasPermission("snapai.admin")) {
            admin.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 1) {
            admin.sendMessage(plugin.getLocaleManager().getMessage("commands.hud.usage-error"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            String msg = plugin.getLocaleManager().getMessage("player-not-found");
            admin.sendMessage(msg.replace("%player%", args[0]));
            return;
        }
        plugin.getHologramManager().toggleHud(admin, target);
    }
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}