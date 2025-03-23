package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.command.CommandManager;
import com.google.inject.Inject;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "modernauthentication", name = "ModernAuthentication", version = "1.0", authors = {"Pyro & Gabuzard"})
public class ModernAuthentication {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    private String serverId;
    private AuthListener authListener;

    @Inject
    public ModernAuthentication(ProxyServer proxyServer, Logger logger, Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Load configuration
        loadConfiguration();

        // Initialize AuthListener
        authListener = new AuthListener(this, proxyServer);
        proxyServer.getEventManager().register(this, authListener);

        // Register commands
        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register("authreload", new ReloadCommand(this));
        commandManager.register("modernconfirm", new ModernConfirmCommand(this));
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Optional cleanup
    }

    public void loadConfiguration() {
        // Load configuration from a file or use default values
        // This is a simplified example; you may want to use a proper configuration library
        backendUrl = "https://auth.bonkmc.org"; // Default value
        backendPort = 8080; // Default value
        accessCode = ""; // Default value
        serverId = "bonk-network"; // Default value
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public String getServerId() {
        return serverId;
    }

    public AuthListener getAuthListener() {
        return authListener;
    }

    public String getMessage(String key) {
        
        return "Default message for " + key;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }
}
