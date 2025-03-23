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

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text(plugin.getMessage("notPlayer")));
            return;
        }

        Player player = (Player) source;
        if (args.length == 0) {
            player.sendMessage(Component.text(plugin.getMessage("confirmUsage")));
            return;
        }

        String response = args[0].toLowerCase();
        if (response.equals("yes")) {
            // Start the authentication flow with changePassword enabled (registration/switching flow).
            plugin.getAuthListener().startAuthentication(player, true);
        } else if (response.equals("no")) {
            player.sendMessage(Component.text(plugin.getMessage("noSwitch")));
        } else {
            player.sendMessage(Component.text(plugin.getMessage("invalidOption")));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true; // Adjust permissions as needed
    }
}
