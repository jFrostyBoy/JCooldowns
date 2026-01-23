package jfbdev.jcooldowns.manager;

import jfbdev.jcooldowns.JCooldowns;
import jfbdev.jcooldowns.util.ColorUtil;
import jfbdev.jcooldowns.util.TimeUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final JCooldowns plugin;
    private final Map<String, Map<String, Long>> groupCooldowns = new HashMap<>();
    public final Map<UUID, Map<String, Long>> playerCooldowns = new ConcurrentHashMap<>();
    private final Map<String, String> commandAliases = new HashMap<>();

    private File cooldownsFile;
    private YamlConfiguration cooldownsConfig;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public CooldownManager(JCooldowns plugin) {
        this.plugin = plugin;
        initFiles();
    }

    private void initFiles() {
        cooldownsFile = new File(plugin.getDataFolder(), "cooldowns.yml");
        if (!cooldownsFile.exists()) {
            plugin.saveResource("cooldowns.yml", false);
        }
        cooldownsConfig = YamlConfiguration.loadConfiguration(cooldownsFile);

        dataFile = new File(plugin.getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.createNewFile()) {
                    plugin.getLogger().warning("data.yml уже существует");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadAll() {
        loadGroupCooldowns();
        loadPlayerCooldowns();
        loadAliases();
    }

    public void saveAll() {
        savePlayerCooldowns();
    }

    public void loadGroupCooldowns() {
        groupCooldowns.clear();
        if (!cooldownsConfig.isConfigurationSection("cooldowns")) {
            plugin.getLogger().warning("Раздел 'cooldowns' не найден в cooldowns.yml!");
            return;
        }
        for (String group : Objects.requireNonNull(cooldownsConfig.getConfigurationSection("cooldowns")).getKeys(false)) {
            Map<String, Long> cmds = new HashMap<>();
            List<String> entries = cooldownsConfig.getStringList("cooldowns." + group);
            for (String entry : entries) {
                String[] parts = entry.split(":", 2);
                if (parts.length != 2) continue;
                String cmd = parts[0].trim().toLowerCase();
                String timeStr = parts[1].trim();
                long millis = TimeUtil.parseToMillis(timeStr);
                if (millis > 0) {
                    cmds.put(cmd, millis / 1000);
                } else {
                    plugin.getLogger().warning("Неверное время в записи '" + entry + "' группы " + group);
                }
            }
            if (!cmds.isEmpty()) {
                groupCooldowns.put(group.toLowerCase(), cmds);
            }
        }
    }

    private void loadPlayerCooldowns() {
        playerCooldowns.clear();
        if (!dataConfig.isConfigurationSection("players")) return;
        for (String uuidStr : Objects.requireNonNull(dataConfig.getConfigurationSection("players")).getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Map<String, Long> cds = new ConcurrentHashMap<>();
            for (String cmd : Objects.requireNonNull(dataConfig.getConfigurationSection("players." + uuidStr)).getKeys(false)) {
                long end = dataConfig.getLong("players." + uuidStr + "." + cmd);
                if (end > System.currentTimeMillis()) {
                    cds.put(cmd.toLowerCase(), end);
                }
            }
            if (!cds.isEmpty()) {
                playerCooldowns.put(uuid, cds);
            }
        }
    }

    private void savePlayerCooldowns() {
        dataConfig.set("players", null);
        long now = System.currentTimeMillis();
        for (var entry : playerCooldowns.entrySet()) {
            UUID uuid = entry.getKey();
            for (var cdEntry : entry.getValue().entrySet()) {
                long end = cdEntry.getValue();
                if (end > now) {
                    dataConfig.set("players." + uuid + "." + cdEntry.getKey(), end);
                }
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException ignored) {}
    }

    private void loadAliases() {
        commandAliases.clear();
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.isConfigurationSection("aliases")) {
            for (String alias : Objects.requireNonNull(cfg.getConfigurationSection("aliases")).getKeys(false)) {
                String baseCmd = cfg.getString("aliases." + alias);
                if (baseCmd != null && !baseCmd.isEmpty()) {
                    commandAliases.put(alias.toLowerCase(), baseCmd.toLowerCase());
                }
            }
        }
    }

    public String resolveCommand(String inputCmd) {
        String lower = inputCmd.toLowerCase();
        return commandAliases.getOrDefault(lower, lower);
    }

    public long getRemainingMillis(UUID uuid, String cmd) {
        var playerCds = playerCooldowns.get(uuid);
        if (playerCds == null) return 0;
        Long end = playerCds.get(cmd.toLowerCase());
        if (end == null) return 0;
        long rem = end - System.currentTimeMillis();
        if (rem <= 0) {
            playerCds.remove(cmd.toLowerCase());
            if (playerCds.isEmpty()) playerCooldowns.remove(uuid);
            return 0;
        }
        return rem;
    }

    public void applyCooldown(Player player, String command) {
        String cmdLower = command.toLowerCase();
        long maxSec = getMaxCooldownForPlayer(player.getUniqueId(), cmdLower);
        if (maxSec <= 0) return;
        long endTime = System.currentTimeMillis() + maxSec * 1000;
        playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>()).put(cmdLower, endTime);
        saveAll();
    }

    private long getMaxCooldownForPlayer(UUID uuid, String cmd) {
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getUserManager().getUser(uuid);
        if (user == null) return 0;

        Group highestPriorityGroup = null;
        int highestWeight = Integer.MIN_VALUE;

        for (Group group : user.getInheritedGroups(user.getQueryOptions())) {
            int weight = group.getWeight().orElse(0);

            if (weight > highestWeight) {
                highestWeight = weight;
                highestPriorityGroup = group;
            }
        }

        if (highestPriorityGroup == null) {
            return 0;
        }

        String groupName = highestPriorityGroup.getName().toLowerCase();
        var groupCd = groupCooldowns.get(groupName);
        if (groupCd == null) {
            return 0;
        }

        Long sec = groupCd.get(cmd.toLowerCase());
        return sec != null ? sec : 0;
    }

    public void sendCooldownNotification(Player player, String cmd, long remainingMillis) {
        FileConfiguration cfg = plugin.getConfig();
        String timeStr = TimeUtil.formatMillis(remainingMillis);

        if (cfg.getBoolean("cooldown_title.enabled", false)) {
            String title = ColorUtil.colorize(cfg.getString("cooldown_title.title", "&cЗадержка!")
                    .replace("%command%", cmd).replace("%cooldown_timer%", timeStr));
            String sub = ColorUtil.colorize(cfg.getString("cooldown_title.subtitle", "")
                    .replace("%command%", cmd).replace("%cooldown_timer%", timeStr));
            long ticks = TimeUtil.parseToMillis(cfg.getString("cooldown_title.display_time", "5s")) / 50;
            player.sendTitle(title, sub, -1, (int) ticks, -1);
        }

        if (cfg.getBoolean("cooldown_actionbar.enabled", false)) {
            String msg = ColorUtil.colorize(cfg.getString("cooldown_actionbar.message", "")
                    .replace("%command%", cmd).replace("%cooldown_timer%", timeStr));
            player.sendActionBar(msg);
        }

        if (cfg.getBoolean("cooldown_bossbar.enabled", false)) {
            String msg = ColorUtil.colorize(cfg.getString("cooldown_bossbar.bossbar.message", "")
                    .replace("%command%", cmd).replace("%cooldown_timer%", timeStr));
            BarColor color = BarColor.valueOf(cfg.getString("cooldown_bossbar.bossbar.color", "RED").toUpperCase());
            BarStyle style = BarStyle.valueOf(cfg.getString("cooldown_bossbar.bossbar.style", "SOLID").toUpperCase());
            BossBar bar = Bukkit.createBossBar(msg, color, style);
            bar.addPlayer(player);
            long ticks = TimeUtil.parseToMillis(cfg.getString("cooldown_bossbar.display_time", "5s")) / 50;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bar.removePlayer(player);
                bar.setVisible(false);
            }, ticks);
        }

        if (cfg.getBoolean("cooldown_chat.enabled", true)) {
            for (String line : cfg.getStringList("cooldown_chat.message")) {
                player.sendMessage(ColorUtil.colorize(line.replace("%command%", cmd).replace("%cooldown_timer%", timeStr)));
            }
        }

        if (cfg.getBoolean("cooldown_sound.enabled", true)) {
            try {
                Sound sound = Sound.valueOf(cfg.getString("cooldown_sound.sound", "BLOCK_ANVIL_PLACE"));
                float vol = (float) cfg.getDouble("cooldown_sound.volume", 1.0);
                float pitch = (float) cfg.getDouble("cooldown_sound.pitch", 1.5);
                player.playSound(player.getLocation(), sound, vol, pitch);
            } catch (Exception ignored) {}
        }
    }

    public Map<String, Map<String, Long>> getGroupCooldowns() {
        return groupCooldowns;
    }

    public YamlConfiguration getCooldownsConfig() {
        return cooldownsConfig;
    }

    public File getCooldownsFile() {
        return cooldownsFile;
    }

    public String getMessage(String key) {
        return ColorUtil.colorize(plugin.getConfig().getString("messages." + key, ""));
    }

    public List<String> getMessageList(String key) {
        return plugin.getConfig().getStringList("messages." + key).stream()
                .map(ColorUtil::colorize).toList();
    }

    public boolean skipCooldown(UUID uuid, String command) {
        String cmdLower = command.toLowerCase();
        var playerCds = playerCooldowns.get(uuid);
        if (playerCds == null) {
            return false;
        }

        Long removed = playerCds.remove(cmdLower);
        if (removed != null) {
            if (playerCds.isEmpty()) {
                playerCooldowns.remove(uuid);
            }
            saveAll();
            return true;
        }
        return false;
    }
}
