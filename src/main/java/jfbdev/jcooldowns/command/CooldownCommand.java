package jfbdev.jcooldowns.command;

import jfbdev.jcooldowns.JCooldowns;
import jfbdev.jcooldowns.manager.CooldownManager;
import jfbdev.jcooldowns.util.ColorUtil;
import jfbdev.jcooldowns.util.TimeUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class CooldownCommand implements CommandExecutor, TabCompleter {

    private final JCooldowns plugin;
    private final CooldownManager manager;

    public CooldownCommand(JCooldowns plugin) {
        this.plugin = plugin;
        this.manager = plugin.getCooldownManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jcooldowns.admin")) {
            sender.sendMessage(manager.getMessage("no_permission"));
            return true;
        }

        if (args.length == 0) {
            manager.getMessageList("help").forEach(sender::sendMessage);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                plugin.reloadConfig();
                manager.loadAll();
                sender.sendMessage(manager.getMessage("reload_success"));
            }
            case "list" -> {
                FileConfiguration cfg = plugin.getConfig();
                cfg.getStringList("cooldowns_list.header").stream()
                        .map(ColorUtil::colorize)
                        .forEach(sender::sendMessage);

                Map<String, Map<String, Long>> groups = manager.getGroupCooldowns();

                if (groups.isEmpty()) {
                    String emptyGroup = cfg.getString("cooldowns_list.empty-groups", "  &cНет ни одной группы с кулдаунами");
                    sender.sendMessage(ColorUtil.colorize(emptyGroup));
                } else {
                    for (String group : groups.keySet()) {
                        String groupLine = cfg.getString("cooldowns_list.group", "  &6%group%:")
                                .replace("%group%", group);
                        sender.sendMessage(ColorUtil.colorize(groupLine));

                        var cmds = groups.get(group);
                        if (cmds == null || cmds.isEmpty()) {
                            String emptyMsg = cfg.getString("cooldowns_list.empty", "  &cНет установленных задержек");
                            sender.sendMessage(ColorUtil.colorize(emptyMsg));
                        } else {
                            cmds.forEach((cmd, sec) -> {
                                String time = TimeUtil.formatMillis(sec * 1000L);
                                String cmdLine = cfg.getString("cooldowns_list.commands", "    &6- &e%command% &7: &f%cooldown_time%")
                                        .replace("%command%", cmd)
                                        .replace("%cooldown_time%", time);
                                sender.sendMessage(ColorUtil.colorize(cmdLine));
                            });
                        }
                    }
                }

                cfg.getStringList("cooldowns_list.footer").stream()
                        .map(ColorUtil::colorize)
                        .forEach(sender::sendMessage);
            }
            case "set" -> {
                if (args.length != 4) {
                    manager.getMessageList("help").forEach(sender::sendMessage);
                    return true;
                }
                String cmdName = manager.resolveCommand(args[1].toLowerCase());
                String group = args[2].toLowerCase();
                String timeInput = args[3];

                long millis = TimeUtil.parseToMillis(timeInput);
                if (millis <= 0) {
                    sender.sendMessage(manager.getMessage("invalid_time"));
                    return true;
                }
                long seconds = millis / 1000;

                var config = manager.getCooldownsConfig();
                List<String> entries = config.getStringList("cooldowns." + group);
                entries.removeIf(s -> s.startsWith(cmdName + ":"));
                if (seconds > 0) {
                    entries.add(cmdName + ":" + timeInput);
                }
                config.set("cooldowns." + group, entries);

                try {
                    config.save(manager.getCooldownsFile());
                } catch (IOException e) {
                    sender.sendMessage("&cОшибка сохранения cooldowns.yml");
                    plugin.getLogger().warning("Ошибка сохранения: " + e.getMessage());
                }

                manager.loadGroupCooldowns();

                String success = manager.getMessage("set_success")
                        .replace("%command%", cmdName)
                        .replace("%group%", group)
                        .replace("%time%", TimeUtil.formatMillis(millis));
                sender.sendMessage(success);
            }
            case "skip" -> {
                if (args.length != 3) {
                    sender.sendMessage(ColorUtil.colorize("&cИспользование: /jcd skip <команда> <игрок>"));
                    return true;
                }

                String cmdName = manager.resolveCommand(args[1].toLowerCase());
                String playerName = args[2];

                Player target = Bukkit.getPlayerExact(playerName);
                UUID uuid;

                if (target != null) {
                    uuid = target.getUniqueId();
                } else {
                    @SuppressWarnings("deprecation")
                    org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
                    if (offline.getName() == null || !offline.hasPlayedBefore()) {
                        sender.sendMessage(manager.getMessage("player_not_found")
                                .replace("%player%", playerName));
                        return true;
                    }
                    uuid = offline.getUniqueId();
                }

                boolean removed = manager.skipCooldown(uuid, cmdName);

                if (removed) {
                    String success = manager.getMessage("skip_success")
                            .replace("%command%", cmdName)
                            .replace("%player%", playerName);
                    sender.sendMessage(success);

                    if (target != null) {
                        String notify = manager.getMessage("skip_notification")
                                .replace("%command%", cmdName);
                        target.sendMessage(notify);
                    }
                } else {
                    String fail = manager.getMessage("skip_not_found")
                            .replace("%command%", cmdName)
                            .replace("%player%", playerName);
                    sender.sendMessage(fail);
                }
            }
            case "unset" -> {
                if (args.length != 3) {
                    sender.sendMessage(ColorUtil.colorize("&cИспользование: /jcd unset <команда> <группа>"));
                    return true;
                }
                String cmdName = manager.resolveCommand(args[1].toLowerCase());
                String group = args[2].toLowerCase();

                var config = manager.getCooldownsConfig();
                List<String> entries = config.getStringList("cooldowns." + group);

                boolean removed = entries.removeIf(s -> s.startsWith(cmdName + ":"));

                if (!removed) {
                    String msg = manager.getMessage("unset_not_found")
                            .replace("%command%", cmdName)
                            .replace("%group%", group);
                    sender.sendMessage(msg);
                    return true;
                }

                config.set("cooldowns." + group, entries);

                try {
                    config.save(manager.getCooldownsFile());
                } catch (IOException e) {
                    sender.sendMessage("&cОшибка сохранения cooldowns.yml");
                    plugin.getLogger().warning("Ошибка сохранения cooldowns.yml при unset: " + e.getMessage());
                    return true;
                }

                int clearedCount = 0;
                for (Map<String, Long> playerCds : manager.playerCooldowns.values()) {
                    if (playerCds.remove(cmdName.toLowerCase()) != null) {
                        clearedCount++;
                    }
                }
                manager.saveAll();

                if (clearedCount > 0) {
                    sender.sendMessage(ColorUtil.colorize("&7(снято активных кулдаунов у игроков: &e" + clearedCount + "&7)"));
                }

                manager.loadGroupCooldowns();

                String success = manager.getMessage("unset_success")
                        .replace("%command%", cmdName)
                        .replace("%group%", group);
                sender.sendMessage(success);
            }
            default -> manager.getMessageList("help").forEach(sender::sendMessage);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "list", "set", "unset", "skip"));
            return filter(completions, args[0]);
        }

        String subCmd = args[0].toLowerCase();

        if ("set".equals(subCmd) || "unset".equals(subCmd)) {
            if (args.length == 2) {
                return getCommandNames(args[1]);
            }
            if (args.length == 3) {
                return getGroupNames(args[2]);
            }
        }

        if ("skip".equals(subCmd)) {
            if (args.length == 2) {
                return getCommandNames(args[1]);
            }
            if (args.length == 3) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }

    private List<String> getCommandNames(String input) {
        try {
            var commandMap = Bukkit.getCommandMap();
            Field knownCommandsField = commandMap.getClass().getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, org.bukkit.command.Command> knownCommands = (Map<String, org.bukkit.command.Command>) knownCommandsField.get(commandMap);

            return knownCommands.values().stream()
                    .map(org.bukkit.command.Command::getName)
                    .map(String::toLowerCase)
                    .filter(name -> !name.contains(":"))
                    .distinct()
                    .filter(name -> name.startsWith(input.toLowerCase()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getGroupNames(String input) {
        LuckPerms lp = LuckPermsProvider.get();
        return lp.getGroupManager().getLoadedGroups().stream()
                .map(Group::getName)
                .map(String::toLowerCase)
                .filter(name -> name.startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
