package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.Identity;
import com.nickuc.login.api.types.AccountData;
import com.nickuc.login.api.event.velocity.auth.LoginEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class AuthListener {

    private final ModernAuthentication plugin;
    private final ProxyServer proxyServer;
    private final Map<UUID, ScheduledTask> authTasks = new HashMap<>();
    private final Map<UUID, Boolean> delayedConfirmations = new HashMap<>();

    public AuthListener(ModernAuthentication plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        Identity identity = Identity.ofKnownName(player.getUsername());

        if (plugin.getNLoginAPI().isAuthenticated(identity)) {
            return;
        }

        String userCheckUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                "/api/isuser/" + plugin.getServerId() + "/" + player.getUsername();
        proxyServer.getScheduler().buildTask(plugin, () -> {
            try {
                URL url = new URL(userCheckUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    boolean exists = response.toString().contains("\"exists\":true");
                    plugin.getLogger().info("User check for " + player.getUsername() + ": " + response.toString());
                    proxyServer.getScheduler().buildTask(plugin, () -> {
                        if (exists) {
                            startAuthentication(player, false);
                        } else {
                            Optional<AccountData> accountOpt = plugin.getNLoginAPI().getAccount(Identity.ofKnownName(player.getUsername()));
                            if (accountOpt.isPresent()) {
                                delayedConfirmations.put(player.getUniqueId(), true);
                            } else {
                                sendSwitchingConfirmation(player);
                            }
                        }
                    }).schedule();
                } else {
                    plugin.getLogger().warning("User check API responded with code " + responseCode + " for " + player.getUsername());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error while checking user for " + player.getUsername() + ": " + e.getMessage());
            }
        }).schedule();
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
        delayedConfirmations.remove(uuid);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (delayedConfirmations.containsKey(player.getUniqueId())) {
            sendSwitchingConfirmation(player);
            delayedConfirmations.remove(player.getUniqueId());
        }
    }

    public void startAuthentication(Player player, boolean changePassword) {
        String token = UUID.randomUUID().toString().replace("-", "");

        proxyServer.getScheduler().buildTask(plugin, () -> {
            try {
                String createTokenUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/api/createtoken";
                URL url = new URL(createTokenUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("X-Server-Secret", plugin.getAccessCode());
                con.setDoOutput(true);
                String jsonPayload = "{\"server_id\":\"" + plugin.getServerId() + "\",\"token\":\"" + token + "\",\"username\":\"" + player.getUsername() + "\"}";
                try (OutputStream os = con.getOutputStream()) {
                    os.write(jsonPayload.getBytes("UTF-8"));
                }
                int responseCode = con.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("Failed to create backend token for " + player.getUsername() + ". Response code: " + responseCode);
                    proxyServer.getScheduler().buildTask(plugin, () -> {
                        player.sendMessage(plugin.getMessage("tokenCreationFailed"));
                    }).schedule();
                    return;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error while creating backend token for " + player.getUsername() + ": " + e.getMessage());
                proxyServer.getScheduler().buildTask(plugin, () -> {
                    player.sendMessage(plugin.getMessage("tokenCreationFailed"));
                }).schedule();
                return;
            }
            proxyServer.getScheduler().buildTask(plugin, () -> {
                String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                        "/auth/" + plugin.getServerId() + "/" + token +
                        "?username=" + player.getUsername();
                String loginMsg = plugin.getMessage("passwordLoginDisabled");
                TextComponent fullMessage = new TextComponent(loginMsg);
                fullMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl));
                fullMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                        net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                        new ComponentBuilder(plugin.getMessage("passwordLoginDisabledHover")).create()
                ));
                player.spigot().sendMessage(fullMessage);
                ScheduledTask task = proxyServer.getScheduler().buildTask(plugin,
                        new AuthTask(plugin, this, player, token, changePassword))
                        .delay(1, TimeUnit.SECONDS)
                        .repeat(1, TimeUnit.SECONDS)
                        .schedule();
                authTasks.put(player.getUniqueId(), task);
            }).schedule();
        }).schedule();
    }

    public void sendSwitchingConfirmation(Player player) {
        String switchMsg = plugin.getMessage("switchConfirmation");
        TextComponent fullMessage = new TextComponent(switchMsg);
        fullMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernconfirm yes"));
        fullMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(plugin.getMessage("switchConfirmationHover")).create()
        ));
        player.spigot().sendMessage(fullMessage);
    }

    public void cancelAuthTask(UUID uuid) {
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }
}
