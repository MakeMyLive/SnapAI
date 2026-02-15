package make.my.snap.command.subcommands;

import org.bukkit.command.CommandSender;
import make.my.snap.SnapAI;
import make.my.snap.command.SubCommand;
import make.my.snap.service.TokenService;

public class TokenCommand extends SubCommand {

    public TokenCommand(SnapAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "token";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.token.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.token.help-usage");
    }

    public String msg(String key) {
        return plugin.getLocaleManager().getMessage(key);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(msg("commands.token.usage"));
            return;
        }

        if (args.length == 1) {
            String first = args[0].toLowerCase();
            if (!"clear".equals(first) && !"info".equals(first) && !"set".equals(first)) {
                handleSetToken(sender, args[0]);
                return;
            }
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set": {
                if (args.length < 2) {
                    sender.sendMessage(msg("commands.token.usage"));
                    return;
                }
                StringBuilder tokenBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) {
                        tokenBuilder.append(" ");
                    }
                    tokenBuilder.append(args[i]);
                }
                handleSetToken(sender, tokenBuilder.toString());
                break;
            }

            case "clear":
                plugin.getTokenService().clearToken();
                sender.sendMessage(msg("commands.token.cleared"));
                break;

            case "info":
                handleInfo(sender);
                break;

            default:
                sender.sendMessage(msg("commands.token.usage"));
                break;
        }
    }

    private void handleSetToken(CommandSender sender, String token) {
        TokenService tokenService = plugin.getTokenService();
        tokenService.setToken(token);
        sender.sendMessage(msg("commands.token.saved"));

        final String yesStr = msg("commands.token.validate");
        final String noStr = msg("commands.token.nvalidate");
        final String validMsg = msg("commands.token.valid");
        final String invalidMsg = msg("commands.token.invalid");
        final String premiumLabel = msg("commands.token.info-premium");
        final String daysLabel = msg("commands.token.info-days");
        final String ipLabel = msg("commands.token.info-ip");

        tokenService.getSubscriptionInfoAsync().thenAccept(subInfo -> {
            if (subInfo != null && !subInfo.isEmpty()) {
                boolean premiumActive = false;
                int remainingDays = 0;
                String lastIp = "Not bound";

                Object remainingDaysValue = subInfo.get("remaining_days");
                if (remainingDaysValue instanceof Number) {
                    remainingDays = ((Number) remainingDaysValue).intValue();
                    premiumActive = remainingDays > 0 || remainingDays == -1;
                }
                Object lastIpValue = subInfo.get("last_ip");
                if (lastIpValue != null) {
                    lastIp = lastIpValue.toString();
                }

                sender.sendMessage(validMsg);
                sender.sendMessage(premiumLabel.replace("%value%", premiumActive ? yesStr : noStr));
                sender.sendMessage(daysLabel.replace("%days%", String.valueOf(remainingDays)));
                sender.sendMessage(ipLabel.replace("%ip%", lastIp));
            } else {
                sender.sendMessage(invalidMsg);
            }
        });
    }

    private void handleInfo(CommandSender sender) {
        TokenService infoService = plugin.getTokenService();
        if (!infoService.hasToken()) {
            sender.sendMessage(msg("commands.token.invalid-token"));
            sender.sendMessage(msg("commands.token.set-hint"));
            return;
        }

        String maskedToken = maskToken(infoService.getToken());
        sender.sendMessage(msg("commands.token.info-header"));
        sender.sendMessage(msg("commands.token.info-token").replace("%token%", maskedToken));

        final String yesStr = msg("commands.token.validate");
        final String noStr = msg("commands.token.nvalidate");
        final String premiumLabel = msg("commands.token.info-premium");
        final String daysLabel = msg("commands.token.info-days");
        final String ipLabel = msg("commands.token.info-ip");
        final String rebindLabel = msg("commands.token.info-can-rebind");

        infoService.getSubscriptionInfoAsync().thenAccept(subInfo -> {
            boolean premiumActive = false;
            int remainingDays = 0;
            String lastIp = "Not bound";
            boolean canRebind = false;

            if (subInfo != null && !subInfo.isEmpty()) {
                Object remainingDaysValue = subInfo.get("remaining_days");
                if (remainingDaysValue instanceof Number) {
                    remainingDays = ((Number) remainingDaysValue).intValue();
                    premiumActive = remainingDays > 0 || remainingDays == -1;
                }
                Object lastIpValue = subInfo.get("last_ip");
                if (lastIpValue != null) {
                    lastIp = lastIpValue.toString();
                }
                Object canRebindValue = subInfo.get("can_rebind");
                if (canRebindValue instanceof Boolean) {
                    canRebind = (Boolean) canRebindValue;
                }
            }

            sender.sendMessage(premiumLabel.replace("%value%", premiumActive ? yesStr : noStr));
            sender.sendMessage(daysLabel.replace("%days%", String.valueOf(remainingDays)));
            sender.sendMessage(ipLabel.replace("%ip%", lastIp));
            sender.sendMessage(rebindLabel.replace("%value%", canRebind ? yesStr : noStr));
        });
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
}
