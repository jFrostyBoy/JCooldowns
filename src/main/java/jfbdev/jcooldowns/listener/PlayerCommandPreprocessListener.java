package jfbdev.jcooldowns.listener;

import jfbdev.jcooldowns.manager.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerCommandPreprocessListener implements Listener {

    private final CooldownManager cooldownManager;

    public PlayerCommandPreprocessListener(CooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("jcooldowns.bypass")) return;

        String msg = event.getMessage().trim();
        if (!msg.startsWith("/")) return;

        String[] parts = msg.substring(1).split(" ", 2);
        String raw = parts[0].toLowerCase();
        String effective = cooldownManager.resolveCommand(raw);

        long rem = cooldownManager.getRemainingMillis(player.getUniqueId(), effective);
        if (rem <= 0) {
            cooldownManager.applyCooldown(player, effective);
            return;
        }

        event.setCancelled(true);
        cooldownManager.sendCooldownNotification(player, raw, rem);
    }
}