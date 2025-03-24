package org.bonkmc.modernAuthenticationVelocity;

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
        String[] args = invocation.arguments();

        // Reload the configuration
        plugin.loadConfiguration();
        source.sendMessage(Component.text(plugin.getMessage("reloadSuccess")));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // You can add permission checks here if needed.
        return invocation.source().hasPermission("modernauth.reload");
    }
}
