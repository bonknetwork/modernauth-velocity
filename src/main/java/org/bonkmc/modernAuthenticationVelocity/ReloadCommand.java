package org.bonkmc.modernAuthentication;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

public class ReloadCommand implements SimpleCommand {

    private final ModernAuthentication plugin;

    public ReloadCommand(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // Reload the plugin's configuration
        plugin.loadConfiguration();

        // Send a success message to the command sender
        source.sendMessage(Component.text(plugin.getMessage("reloadSuccess")));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // Check if the command sender has the required permission
        return invocation.source().hasPermission("modernauth.reload");
    }
}
