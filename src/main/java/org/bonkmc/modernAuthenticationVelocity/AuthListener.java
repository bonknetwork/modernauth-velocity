package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener {

    private final ModernAuthentication plugin;
    private final ProxyServer proxyServer;
    private final Map<UUID, String> pendingAuths = new HashMap<>(); // Stores players awaiting authentication

    public AuthListener(ModernAuthentication plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();

        // Prompt the player to enter their password
        player.sendMessage(plugin.getMessage("enterPassword"));
        pendingAuths.put(player.getUniqueId(), null); // Mark player as pending authentication
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if the player is pending authentication
        if (pendingAuths.containsKey(uuid)) {
            String password = event.getMessage().trim(); // Get the password from the chat message

            // Start the authentication process
            AuthTask authTask = new AuthTask(plugin, this, player, password);
            proxyServer.getScheduler().buildTask(plugin, authTask)
                .delay(1, TimeUnit.SECONDS)
                .schedule();

            // Remove the player from the pending list
            pendingAuths.remove(uuid);
            event.setResult(PlayerChatEvent.ChatResult.denied()); // Prevent the password from being broadcast
        }
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Remove the player from the pending list if they disconnect
        pendingAuths.remove(uuid);
    }

    public void cancelAuthTask(UUID uuid) {
        pendingAuths.remove(uuid);
    }
}
