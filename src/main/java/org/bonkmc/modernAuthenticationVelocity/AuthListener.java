package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AuthListener {

    private final ModernAuthentication plugin;
    private final ProxyServer proxyServer;
    private final Map<UUID, ScheduledTask> authTasks = new HashMap<>();

    public AuthListener(ModernAuthentication plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();

        // Start the authentication process for the player
        startAuthentication(player);
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Cancel any ongoing authentication task for the player
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }

    /**
     * Starts the authentication process for a player.
     *
     * @param player The player to authenticate.
     */
    public void startAuthentication(Player player) {
        // Generate a unique token for the player
        String token = UUID.randomUUID().toString().replace("-", "");

        // Send the token to the backend to initiate authentication
        proxyServer.getScheduler().buildTask(plugin, () -> {
            boolean tokenCreated = createBackendToken(player, token);
            if (!tokenCreated) {
                player.sendMessage(plugin.getMessage("tokenCreationFailed"));
                return;
            }

            // Send the authentication URL to the player
            String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                    "/auth/" + plugin.getServerId() + "/" + token +
                    "?username=" + player.getUsername();
            player.sendMessage(plugin.getMessage("passwordLoginDisabled") + " " + authUrl);

            // Start a task to periodically check if the player has completed authentication
            ScheduledTask task = proxyServer.getScheduler().buildTask(plugin, () -> checkAuthentication(player, token))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(1, TimeUnit.SECONDS)
                    .schedule();
            authTasks.put(player.getUniqueId(), task);
        }).schedule();
    }

    /**
     * Sends a token creation request to the backend.
     *
     * @param player The player to authenticate.
     * @param token  The token to send to the backend.
     * @return True if the token was successfully created, false otherwise.
     */
    private boolean createBackendToken(Player player, String token) {
        try {
            String createTokenUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/api/createtoken";
            HttpURLConnection connection = (HttpURLConnection) new URL(createTokenUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode()); // Use access code in the header
            connection.setDoOutput(true);

            // Create the JSON payload
            String jsonPayload = "{\"server_id\":\"" + plugin.getServerId() + "\",\"token\":\"" + token + "\",\"username\":\"" + player.getUsername() + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            // Check the response code
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            plugin.getLogger().error("Failed to create backend token for " + player.getUsername(), e);
            return false;
        }
    }

    /**
     * Checks if the player has completed authentication on the backend.
     *
     * @param player The player to check.
     * @param token  The token associated with the player.
     */
    private void checkAuthentication(Player player, String token) {
        try {
            String checkAuthUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/api/checkauth";
            HttpURLConnection connection = (HttpURLConnection) new URL(checkAuthUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode()); // Use access code in the header
            connection.setDoOutput(true);

            // Create the JSON payload
            String jsonPayload = "{\"server_id\":\"" + plugin.getServerId() + "\",\"token\":\"" + token + "\",\"username\":\"" + player.getUsername() + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes("UTF-8"));
            }

            // Check the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Authentication successful
                player.sendMessage(plugin.getMessage("authSuccess"));
                cancelAuthTask(player.getUniqueId());
            } else if (responseCode == 401) {
                // Authentication failed
                player.sendMessage(plugin.getMessage("authFailed"));
                cancelAuthTask(player.getUniqueId());
            }
        } catch (Exception e) {
            plugin.getLogger().error("Failed to check authentication for " + player.getUsername(), e);
        }
    }

    /**
     * Cancels the authentication task for a player.
     *
     * @param uuid The UUID of the player.
     */
    public void cancelAuthTask(UUID uuid) {
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }
}
