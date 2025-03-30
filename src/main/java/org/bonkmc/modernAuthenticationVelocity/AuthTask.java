package org.bonkmc.modernAuthenticationVelocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.Identity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

public class AuthTask implements Runnable {

    private final ModernAuthentication plugin;
    private final AuthListener authListener;
    private final Player player;
    private final String token;
    private final boolean changePassword;
    private final Identity identity;
    private final ProxyServer proxyServer;

    public AuthTask(ModernAuthentication plugin, AuthListener authListener, Player player, String token, boolean changePassword) {
        this.plugin = plugin;
        this.authListener = authListener;
        this.player = player;
        this.token = token;
        this.changePassword = changePassword;
        this.identity = Identity.ofKnownName(player.getUsername());
        this.proxyServer = plugin.getProxyServer();
    }

    @Override
    public void run() {
        if (!player.isActive()) { // Velocity uses `isActive()` instead of `isOnline()`
            cancelTask();
            return;
        }

        try {
            // Build the API status URL using the public server ID.
            String statusUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                    "/api/authstatus/" + plugin.getServerId() + "/" + token;
            URL url = new URL(statusUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            // Use the access code in the request header (server-side only).
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode());

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseContent = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseContent.append(inputLine);
                }
                in.close();

                if (responseContent.toString().contains("\"logged_in\":true")) {
                    // Force login on the main thread.
                    proxyServer.getScheduler().buildTask(plugin, () -> {
                        // Compute IP once.
                        String ip = (player.getRemoteAddress() != null)
                                ? player.getRemoteAddress().getAddress().getHostAddress() : "unknown";

                        // Attempt to force login.
                        boolean loggedIn = plugin.getNLoginAPI().forceLogin(identity, true);

                        if (loggedIn) {
                            player.sendMessage(plugin.getMessage("authSuccess"));

                            // If a password change is requested, change it asynchronously.
                            if (changePassword) {
                                final String newPassword = generateRandomPassword();
                                proxyServer.getScheduler().buildTask(plugin, () -> {
                                    boolean passwordChanged = plugin.getNLoginAPI().changePassword(identity, newPassword);
                                    if (passwordChanged) {
                                        plugin.getLogger().info("Password changed successfully for " + player.getUsername());
                                    } else {
                                        plugin.getLogger().warning("Failed to change password for " + player.getUsername());
                                    }
                                }).schedule();
                            }

                        } else {
                            // Login failed: register the player then try logging in again.
                            final String newPassword = generateRandomPassword();
                            proxyServer.getScheduler().buildTask(plugin, () -> {
                                boolean registered = plugin.getNLoginAPI().performRegister(identity, newPassword, ip);
                                if (registered) {
                                    plugin.getLogger().info("Player " + player.getUsername() + " registered successfully.");
                                    // Reattempt force login on the main thread.
                                    proxyServer.getScheduler().buildTask(plugin, () -> {
                                        boolean secondLogin = plugin.getNLoginAPI().forceLogin(identity, true);
                                        if (secondLogin) {
                                            player.sendMessage(plugin.getMessage("authSuccessAfterRegister"));
                                        } else {
                                            player.sendMessage(plugin.getMessage("authFailed"));
                                        }
                                    }).schedule();
                                } else {
                                    plugin.getLogger().warning("Failed to register player " + player.getUsername());
                                    player.sendMessage(plugin.getMessage("registrationFailed"));
                                }
                            }).schedule();
                        }
                    }).schedule();

                    cancelTask();
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while checking auth status for " + player.getUsername() + ": " + e.getMessage());
        }
    }

    private void cancelTask() {
        proxyServer.getScheduler().buildTask(plugin, () -> {
            authListener.cancelAuthTask(player.getUniqueId());
        }).schedule();
    }

    // Generates a random password containing letters, numbers, and symbols.
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:,.<>?";
        SecureRandom random = new SecureRandom();
        int length = 20 + random.nextInt(16); // Generates a length between 20 and 35.
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
