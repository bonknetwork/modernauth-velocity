package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;

public class ModernConfirmCommand implements SimpleCommand {

    private final ModernAuthentication plugin;

    public ModernConfirmCommand(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        // Ensure the command is executed by a player
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(plugin.getMessage("notPlayer")));
            return;
        }

        Player player = (Player) source;

        // Check if the player provided the correct number of arguments
        if (args.length == 0) {
            player.sendMessage(Component.text(plugin.getMessage("confirmUsage")));
            return;
        }

        String response = args[0].toLowerCase();

        // Handle the player's response
        if (response.equals("yes")) {
            // Start the authentication flow (e.g., for switching or registration)
            player.sendMessage(Component.text(plugin.getMessage("switchConfirmation")));
            plugin.getAuthListener().startAuthentication(player);
        } else if (response.equals("no")) {
            // Player chose not to proceed
            player.sendMessage(Component.text(plugin.getMessage("noSwitch")));
        } else {
            // Invalid option
            player.sendMessage(Component.text(plugin.getMessage("invalidOption")));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Allow all players to use this command (adjust permissions as needed)
        return true;
    }
}
