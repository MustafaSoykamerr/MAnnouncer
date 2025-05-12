package com.midenium.mannouncer;

import com.google.inject.Inject;
import com.midenium.mannouncer.commands.MAnnouncerCommand;
import com.midenium.mannouncer.commands.RateLimitedCommand;
import com.midenium.mannouncer.config.ConfigManager;
import com.midenium.mannouncer.listeners.ConnectionListener;
import com.midenium.mannouncer.managers.AnnouncementManager;
import com.midenium.mannouncer.managers.ServerManager;
import com.midenium.mannouncer.managers.StreamerManager;
import com.midenium.mannouncer.tasks.AnnouncementTask;
import com.midenium.mannouncer.utils.PermissionManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Plugin(
        id = "mannouncer",
        name = "mAnnouncer",
        version = "1.0.0",
        description = "A modern and feature-rich announcement system for Velocity",
        authors = {"Midenium"}
)
public class MAnnouncer {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    
    private ConfigManager configManager;
    private AnnouncementManager announcementManager;
    private ServerManager serverManager;
    private StreamerManager streamerManager;
    private AnnouncementTask announcementTask;
    private PermissionManager permissionManager;
    private boolean luckPermsHooked = false;

    @Inject
    public MAnnouncer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize metrics
        metricsFactory.make(this, 19386);
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        
        // Hook into LuckPerms if available
        setupLuckPerms();
        
        // Initialize permission manager
        permissionManager = new PermissionManager(this);
        
        // Initialize managers
        serverManager = new ServerManager(this);
        announcementManager = new AnnouncementManager(this);
        
        // Initialize streamer manager if enabled
        Map<String, Object> config = configManager.getMainConfig();
        Map<String, Object> streamersConfig = (Map<String, Object>) config.getOrDefault("streamers", Map.of());
        boolean streamersEnabled = (boolean) streamersConfig.getOrDefault("enabled", true);
        
        if (streamersEnabled) {
            streamerManager = new StreamerManager(this);
        }
        
        // Register commands with rate limiting if enabled
        boolean rateLimitCommands = isRateLimitCommandsEnabled();
        int rateLimitThreshold = getRateLimitThreshold();
        
        if (rateLimitCommands) {
            server.getCommandManager().register(
                "mannouncer", 
                new RateLimitedCommand(new MAnnouncerCommand(this), rateLimitThreshold)
            );
        } else {
            server.getCommandManager().register("mannouncer", new MAnnouncerCommand(this));
        }
        
        // Register listeners
        server.getEventManager().register(this, new ConnectionListener(this));
        
        // Start announcement task
        announcementTask = new AnnouncementTask(this);
        announcementTask.start();
        
        logger.info("mAnnouncer has been enabled!");
    }
    
    private void setupLuckPerms() {
        // Check if LuckPerms is enabled in the config
        Map<String, Object> config = configManager.getMainConfig();
        Map<String, Object> permConfig = (Map<String, Object>) config.getOrDefault("permissions", Map.of());
        boolean useLuckPerms = (boolean) permConfig.getOrDefault("use-luckperms", true);
        
        if (!useLuckPerms) {
            logger.info("LuckPerms integration is disabled in the config.");
            return;
        }
        
        // Check if LuckPerms is installed
        Optional<PluginContainer> luckPermsPlugin = server.getPluginManager().getPlugin("luckperms");
        if (luckPermsPlugin.isPresent()) {
            try {
                // Try to get the LuckPerms API
                LuckPerms luckPermsApi = LuckPermsProvider.get();
                luckPermsHooked = true;
                logger.info("Hooked into LuckPerms-Velocity (For Permissions)");
            } catch (Exception e) {
                logger.warn("Failed to hook into LuckPerms: " + e.getMessage());
            }
        } else {
            logger.info("LuckPerms is not installed. Using default permission system.");
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Clean up resources
        if (announcementTask != null) {
            announcementTask.stop();
        }
        
        if (streamerManager != null) {
            streamerManager.shutdown();
        }
        
        logger.info("mAnnouncer has been disabled!");
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public AnnouncementManager getAnnouncementManager() {
        return announcementManager;
    }
    
    public ServerManager getServerManager() {
        return serverManager;
    }
    
    public StreamerManager getStreamerManager() {
        return streamerManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public boolean isLuckPermsHooked() {
        return luckPermsHooked;
    }

    private boolean isRateLimitCommandsEnabled() {
        Map<String, Object> config = configManager.getMainConfig();
        Map<String, Object> securityConfig = (Map<String, Object>) config.getOrDefault("security", Map.of());
        Object rateLimitObj = securityConfig.getOrDefault("rate-limit-commands", false);
        return rateLimitObj instanceof Boolean && (Boolean) rateLimitObj;
    }
    
    private int getRateLimitThreshold() {
        Map<String, Object> config = configManager.getMainConfig();
        Map<String, Object> securityConfig = (Map<String, Object>) config.getOrDefault("security", Map.of());
        Object thresholdObj = securityConfig.getOrDefault("rate-limit-threshold", 10);
        
        if (thresholdObj instanceof Integer) {
            return (Integer) thresholdObj;
        } else if (thresholdObj instanceof String) {
            try {
                return Integer.parseInt((String) thresholdObj);
            } catch (NumberFormatException e) {
                return 10;
            }
        }
        
        return 10;
    }
} 