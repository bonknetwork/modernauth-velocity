package org.bonkmc.modernAuthenticationVelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(id = "modernauthenticationvelocity", name = "ModernAuthenticationVelocity", version = BuildConstants.VERSION, url = "https://discord.gg/bonknetwork", authors = {"Pyro"})
public class ModernAuthenticationVelocity {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
