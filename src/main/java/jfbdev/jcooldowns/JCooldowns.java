package jfbdev.jcooldowns;

import jfbdev.jcooldowns.command.CooldownCommand;
import jfbdev.jcooldowns.listener.PlayerCommandPreprocessListener;
import jfbdev.jcooldowns.manager.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class JCooldowns extends JavaPlugin {

    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        cooldownManager = new CooldownManager(this);
        cooldownManager.loadAll();

        var cmd = getCommand("jcd");
        if (cmd != null) {
            cmd.setExecutor(new CooldownCommand(this));
            cmd.setTabCompleter(new CooldownCommand(this));
        }

        getServer().getPluginManager().registerEvents(
                new PlayerCommandPreprocessListener(cooldownManager), this);

    }

    @Override
    public void onDisable() {
        if (cooldownManager != null) {
            cooldownManager.saveAll();
        }
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
}