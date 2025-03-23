package org.bonkmc.modernAuthentication;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PluginConfig {

    private final ModernAuthentication plugin;
    private final Logger logger;

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    private String serverId;
    private final Map<String, String> messages = new HashMap<>();

    public PluginConfig(ModernAuthentication plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void loadConfiguration() {
        try {
            // Get the plugin's data directory
            Path configFile = plugin.getDataDirectory().resolve("config.yml");

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
            accessCode = config.getNode("access-code").getString("");
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

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§cMessage not found: " + key);
    }
}
