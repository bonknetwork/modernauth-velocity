package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class AuthTask implements Runnable {

    private final ModernAuthentication plugin;
    private final AuthListener authListener;
    private final Player player;
    private final String password; // The player's password

    public AuthTask(ModernAuthentication plugin, AuthListener authListener, Player player, String password) {
        this.plugin = plugin;
        this.authListener = authListener;
        this.player = player;
        this.password = password;
    }

    @Override
    public void run() {
        // Check if the player is still connected
        if (!player.isActive()) {
            cancelTask();
            return;
        }

        try {
            // Send the player's credentials to the backend for authentication
            String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/api/authenticate";
            URL url = new URL(authUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode()); // Include access code in the header
            connection.setDoOutput(true);

            // Create the JSON payload with the player's username and password
            String jsonPayload = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"server_id\":\"%s\"}",
                player.getUsername(), password, plugin.getServerId()
            );

            // Send the payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Get the response from the backend
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                // Authentication successful
                plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
                    player.sendMessage(plugin.getMessage("authSuccess"));
                    cancelTask(); // Stop the task since authentication is complete
                }).schedule();
            } else if (responseCode == 401) {
                // Authentication failed
                plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
                    player.sendMessage(plugin.getMessage("authFailed"));
                    cancelTask(); // Stop the task since authentication failed
                }).schedule();
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error while authenticating player: " + player.getUsername(), e);
            cancelTask(); // Stop the task if an error occurs
        }
    }

    /**
     * Cancels the authentication task for the player.
     */
    private void cancelTask() {
        plugin.getProxyServer().getScheduler().buildTask(plugin, () -> {
            authListener.cancelAuthTask(player.getUniqueId());
        }).schedule();
    }
}
