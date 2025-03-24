package org.bonkmc.modernAuthenticationVelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.command.CommandManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Plugin(
        id = "modernauthentication",
        name = "ModernAuthentication",
        version = "1.0.0",
        description = "A modern authentication plugin for Velocity",
        authors = {"YourName"}
)
public final class ModernAuthentication {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    private String serverId; // Loaded from config.
    private AuthListener authListener;

    private nLoginAPI nLoginAPI; // nLogin API instance

    @Inject
    public ModernAuthentication(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Ensure the data directory exists
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Failed to create data directory", e);
                return;
            }
        }

        // Load or create the config file
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        if (!configFile.exists()) {
            createDefaultConfig(configFile);
        }

        // Load configuration
        loadConfiguration(configFile);

        // Check if nLogin is available
        if (!setupNLogin()) {
            logger.error("nLogin not found. Disabling ModernAuthentication.");
            return; // Disable the plugin if nLogin is not present
        }

        // Register listeners and commands
        authListener = new AuthListener(this, proxyServer);
        proxyServer.getEventManager().register(this, authListener);

        CommandManager commandManager = proxyServer.getCommandManager();
        commandManager.register(commandManager.metaBuilder("authreload").build(), new ReloadCommand(this));
        commandManager.register(commandManager.metaBuilder("modernconfirm").build(), new ModernConfirmCommand(this));

        logger.info("ModernAuthentication has been enabled!");
    }

    /**
     * Creates the default configuration file.
     * @param configFile The configuration file to create.
     */
    private void createDefaultConfig(File configFile) {
        try (FileWriter writer = new FileWriter(configFile)) {
            // Default configuration values
            Map<String, Object> config = new HashMap<>();
            config.put("backendUrl", "https://auth.bonkmc.org");
            config.put("backendPort", 8080);
            config.put("access-code", "your-access-code");
            config.put("server-id", "bonk-network");

            // Write the default configuration to the file
            writer.write("backendUrl: " + config.get("backendUrl") + "\n");
            writer.write("backendPort: " + config.get("backendPort") + "\n");
            writer.write("access-code: " + config.get("access-code") + "\n");
            writer.write("server-id: " + config.get("server-id") + "\n");
            writer.write("\n");
            writer.write("messages:\n");
            writer.write("  passwordLoginDisabled: \"Please click here to log in.\"\n");
            writer.write("  passwordLoginDisabledHover: \"Click to open the authentication page.\"\n");
            writer.write("  switchConfirmation: \"Click here to switch to the new authentication system.\"\n");
            writer.write("  switchConfirmationHover: \"Confirm your switch to the new system.\"\n");
            writer.write("  tokenCreationFailed: \"Failed to create a token. Please try again.\"\n");
            writer.write("  notPlayer: \"This command can only be used by players.\"\n");
            writer.write("  confirmUsage: \"Usage: /modernconfirm <yes|no>\"\n");
            writer.write("  noSwitch: \"You have chosen not to switch to the new authentication system.\"\n");
            writer.write("  invalidOption: \"Invalid option. Please use 'yes' or 'no'.\"\n");
            writer.write("  reloadSuccess: \"Configuration reloaded successfully!\"\n");

            logger.info("Created default configuration file.");
        } catch (IOException e) {
            logger.error("Failed to create default configuration file", e);
        }
    }

    /**
     * Loads the configuration from the config file.
     * @param configFile The configuration file.
     */
    private void loadConfiguration(File configFile) {
        try {
            // Read the configuration file
            Map<String, Object> config = new HashMap<>();
            for (String line : Files.readAllLines(configFile.toPath())) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    config.put(key, value);
                }
            }

            // Load configuration values
            backendUrl = (String) config.getOrDefault("backendUrl", "https://auth.bonkmc.org");
            backendPort = Integer.parseInt((String) config.getOrDefault("backendPort", "8080"));
            accessCode = (String) config.getOrDefault("access-code", "your-access-code");
            serverId = (String) config.getOrDefault("server-id", "bonk-network");

            logger.info("Configuration loaded successfully.");
        } catch (IOException e) {
            logger.error("Failed to load configuration file", e);
        }
    }

    /**
     * Attempts to set up the nLogin API.
     * @return true if nLogin is available, false otherwise.
     */
    private boolean setupNLogin() {
        try {
            // Attempt to load the nLogin API class
            Class<?> nLoginAPIClass = Class.forName("com.nickuc.login.api.nLoginAPI");
            // Get the API instance using reflection
            nLoginAPI = (nLoginAPI) nLoginAPIClass.getMethod("getApi").invoke(null);
            logger.info("Successfully hooked into nLogin!");
            return true;
        } catch (Exception e) {
            logger.error("Failed to hook into nLogin. Make sure nLogin is installed.", e);
            return false;
        }
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

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public Logger getLogger() {
        return logger;
    }

    public nLoginAPI getNLoginAPI() {
        return nLoginAPI;
    }

    /**
     * Reloads the configuration file.
     */
    public void reloadConfiguration() {
        File configFile = new File(dataDirectory.toFile(), "config.yml");
        loadConfiguration(configFile);
    }

    /**
     * Gets a message from the configuration.
     * @param key The message key.
     * @return The message, or a default if not found.
     */
    public String getMessage(String key) {
        // For simplicity, this example hardcodes messages.
        // You should load these from your configuration file.
        switch (key) {
            case "passwordLoginDisabled":
                return "Please click here to log in.";
            case "passwordLoginDisabledHover":
                return "Click to open the authentication page.";
            case "switchConfirmation":
                return "Click here to switch to the new authentication system.";
            case "switchConfirmationHover":
                return "Confirm your switch to the new system.";
            case "tokenCreationFailed":
                return "Failed to create a token. Please try again.";
            case "notPlayer":
                return "This command can only be used by players.";
            case "confirmUsage":
                return "Usage: /modernconfirm <yes|no>";
            case "noSwitch":
                return "You have chosen not to switch to the new authentication system.";
            case "invalidOption":
                return "Invalid option. Please use 'yes' or 'no'.";
            case "reloadSuccess":
                return "Configuration reloaded successfully!";
            default:
                return "Message not found.";
        }
    }
}
