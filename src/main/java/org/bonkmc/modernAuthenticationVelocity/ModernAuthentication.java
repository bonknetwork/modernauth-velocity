package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.command.CommandManager;
import com.google.inject.Inject;
import org.slf4j.Logger;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Plugin(id = "modernauthentication", name = "ModernAuthentication", version = "1.0", authors = {"Pyro & Gabuzard"})
public class ModernAuthentication {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private String backendUrl;
    private int backendPort;
    private String accessCode; // Access code is securely loaded from config
    private String serverId;
    private final Map<String, String> messages = new HashMap<>();
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

        logger.info("ModernAuthentication has been initialized!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Optional cleanup
        logger.info("ModernAuthentication has been disabled.");
    }

    public void loadConfiguration() {
        try {
            // Get the plugin's data directory
            Path configFile = dataDirectory.resolve("config.yml");

            // Create the config file if it doesn't exist
            if (!Files.exists(configFile)) {
                createDefaultConfig(configFile);
                logger.info("Default configuration file created!");
            }

            // Load the configuration
            YAMLConfigurationLoader loader = YAMLConfigurationLoader.builder()
                .setPath(configFile)
                .build();
            ConfigurationNode config = loader.load();

            // Read values from the configuration
            backendUrl = config.getNode("backendUrl").getString("https://auth.bonkmc.org");
            backendPort = config.getNode("backendPort").getInt(443);
            accessCode = config.getNode("access-code").getString(""); // Access code is loaded here
            serverId = config.getNode("server-id").getString("server-id-here");

            // Load messages
            ConfigurationNode messagesNode = config.getNode("messages");
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : messagesNode.getChildrenMap().entrySet()) {
                messages.put(entry.getKey().toString(), entry.getValue().getString());
            }

            logger.info("Configuration loaded successfully!");
        } catch (Exception e) {
            logger.error("Failed to load configuration", e);
        }
    }

    private void createDefaultConfig(Path configFile) throws IOException {
        // Create the parent directory if it doesn't exist
        Files.createDirectories(configFile.getParent());

        // Write the default config to the file
        String defaultConfig = """
        # The URL of your authentication backend.
        backendUrl: "https://auth.bonkmc.org"

        # The port on which your backend is running.
        backendPort: 443

        # The access code used when communicating with the backend API.
        access-code: "your_access_code_here"

        # The public server ID used in the link sent to players.
        server-id: "server-id-here"

        messages:
          reloadSuccess: "§7------------------------------\\n§aModernAuthentication configuration reloaded.\\n§7------------------------------"
          notPlayer: "§7------------------------------\\nOnly players can use this command.\\n§7------------------------------"
          confirmUsage: "§7------------------------------\\n§ePlease type /modernconfirm <yes|no>\\n§7------------------------------"
          noSwitch: "§7------------------------------\\n§aNo problem! You can continue using your current login method.\\n§7------------------------------"
          invalidOption: "§7------------------------------\\n§eInvalid option. Please type /modernconfirm <yes|no>\\n§7------------------------------"
          authSuccess: ""
          authSuccessAfterRegister: ""
          authFailed: "§7------------------------------\\n§cAuthentication failed even after registration. Please contact a higher administrator.\\n§7------------------------------"
          registrationFailed: "§7------------------------------\\n§cRegistration failed. Please contact an administrator. You have not been logged in.\\n§7------------------------------"
          tokenCreationFailed: "§7------------------------------\\n§cError: Failed to initiate authentication. Please try again later.\\n§7------------------------------"
          passwordLoginDisabled: "§7------------------------------\\n§cPassword login is modified for this account.\\n§aClick here to login using ModernAuth.\\n§7------------------------------"
          passwordLoginDisabledHover: "Open the ModernAuth login page"
          switchConfirmation: "§7------------------------------\\n§eClick here to switch to ModernAuth\\n§7------------------------------"
          switchConfirmationHover: "Switch to ModernAuth"
        """;
        Files.write(configFile, defaultConfig.getBytes());
    }

    // Getters for configuration values
    public String getBackendUrl() {
        return backendUrl;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public String getAccessCode() {
        return accessCode; // Access code is securely retrieved here
    }

    public String getServerId() {
        return serverId;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§cMessage not found: " + key);
    }

    public AuthListener getAuthListener() {
        return authListener;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }
}
