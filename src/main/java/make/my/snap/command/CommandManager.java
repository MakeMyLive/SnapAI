package make.my.snap.command;

import make.my.snap.command.subcommands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import make.my.snap.SnapAI;

import java.util.*;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    private final SnapAI plugin;

    public CommandManager(SnapAI plugin) {
        this.plugin = plugin;
        registerSubCommand(new AlertsCommand(plugin));
        registerSubCommand(new HistoryCommand(plugin));
        registerSubCommand(new StatsCommand(plugin));
        registerSubCommand(new PunishCommand(plugin));
        registerSubCommand(new ServerCommand(plugin));
        registerSubCommand(new TokenCommand(plugin));
        registerSubCommand(new CrashCommand(plugin));
        registerSubCommand(new RecordCommand(plugin));
        registerSubCommand(new HudCommand(plugin));
        registerSubCommand(new ReloadCommand(plugin));
    }

    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("unknown-subcommand"));
            return true;
        }

        if (!sender.hasPermission("snapai.admin") && !subCommandName.equals("token")) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("no-permission"));
            return true;
        }

        String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subCommandArgs);

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        String header = plugin.getLocaleManager().getMessage("help.header");
        String pattern = plugin.getLocaleManager().getMessage("help.entry");

        boolean isPlayer = sender instanceof Player;
        if (!isPlayer) {
            header = ChatColor.stripColor(header);
            pattern = ChatColor.stripColor(pattern);
        }

        sender.sendMessage(header);

        for (SubCommand subCommand : subCommands.values()) {
            String usage = subCommand.getUsage();
            String description = subCommand.getDescription();

            if (!isPlayer) {
                usage = ChatColor.stripColor(usage);
                description = ChatColor.stripColor(description);
            }

            String line = pattern
                    .replace("%usage%", usage)
                    .replace("%description%", description);
            sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("snapai.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], subCommands.keySet(), new ArrayList<>());
        }

        if (args.length > 1) {
            SubCommand subCommand = subCommands.get(args[0].toLowerCase());
            if (subCommand != null) {
                String[] subCommandArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.onTabComplete(sender, subCommandArgs);
            }
        }

        return Collections.emptyList();
    }
}