package make.my.snap.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import make.my.snap.SnapAI;
import make.my.snap.command.SubCommand;
import make.my.snap.entity.PlayerEntity;
import make.my.snap.registry.PlayerRegistry;
import make.my.snap.util.MouseCalculator;

import java.util.*;
import java.util.stream.Collectors;

public class RecordCommand extends SubCommand {

    public RecordCommand(SnapAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "record";
    }

    @Override
    public String getDescription() {
        return "Автоматический сбор нескольких датасетов с игрока";
    }

    @Override
    public String getUsage() {
        return "/snapai record <start/stop> <player> [1/0] [sets_amount]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snapai.admin")) {
            sender.sendMessage("§cУ вас нет прав (snapai.admin) для выполнения этой команды.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: " + getUsage());
            return;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        if (targetPlayer == null) {
            sender.sendMessage("§cИгрок " + targetName + " не найден в сети.");
            return;
        }

        PlayerEntity entity = PlayerRegistry.getPlayer(targetPlayer.getUniqueId());
        if (entity == null) {
            sender.sendMessage("§cКритическая ошибка: данные игрока не загружены в PlayerRegistry.");
            return;
        }

        UUID senderUUID = (sender instanceof Player) ? ((Player) sender).getUniqueId() : null;

        if (action.equals("start")) {
            if (entity.isRecording()) {
                sender.sendMessage("§cДля этого игрока запись уже запущена.");
                return;
            }

            int label = 1;
            if (args.length >= 3) {
                try {
                    label = Integer.parseInt(args[2]);
                    if (label != 0 && label != 1) {
                        sender.sendMessage("§cМетка должна быть 0 (Legit) или 1 (Cheat).");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cМетка должна быть числом 0 или 1.");
                    return;
                }
            }

            int amount = 1;
            if (args.length >= 4) {
                try {
                    amount = Integer.parseInt(args[3]);
                    if (amount <= 0) {
                        sender.sendMessage("§cКоличество сетов должно быть больше нуля.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cКоличество сетов должно быть числом.");
                    return;
                }
            }

            float currentYaw = MouseCalculator.normalizeAngle(targetPlayer.getLocation().getYaw());
            float currentPitch = targetPlayer.getLocation().getPitch();

            entity.startRecording(label, senderUUID, amount, currentYaw, currentPitch);

            sender.sendMessage("§a[SnapAI] Циклическая запись начата для §f" + targetPlayer.getName());
            sender.sendMessage("§7Тип данных: " + (label == 1 ? "§c§lCHEAT" : "§b§lLEGIT"));
            sender.sendMessage("§7План: §e" + amount + " §7сетов по 150 фреймов.");
            sender.sendMessage("§8(Счетчик фреймов и остаток сетов отображаются в ActionBar)");

        } else if (action.equals("stop")) {
            if (!entity.isRecording()) {
                sender.sendMessage("§cДля этого игрока запись не ведется.");
                return;
            }

            entity.stopRecording();
            entity.getFrames().clear();
            sender.sendMessage("§e[SnapAI] Запись для игрока §f" + targetPlayer.getName() + " §eбыла прервана вручную.");
        } else {
            sender.sendMessage("§cНеизвестное действие. Используйте start или stop.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("snapai.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("start", "stop"), new ArrayList<>());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> StringUtil.startsWithIgnoreCase(n, args[1]))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return StringUtil.copyPartialMatches(args[2], Arrays.asList("0", "1"), new ArrayList<>());
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("start")) {
            return StringUtil.copyPartialMatches(args[3], Arrays.asList("1", "5", "10", "20", "50"), new ArrayList<>());
        }

        return Collections.emptyList();
    }
}