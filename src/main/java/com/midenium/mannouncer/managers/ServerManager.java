package com.midenium.mannouncer.managers;

import com.midenium.mannouncer.MAnnouncer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ServerManager {

    private final MAnnouncer plugin;
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();
    private boolean isRunning = false;
    private boolean assumeServersOnline = true; // Default to true to make sure announcements work
    
    public ServerManager(MAnnouncer plugin) {
        this.plugin = plugin;
        loadConfig();
        initializeServerStatus();
        startMonitoring();
    }
    
    private void loadConfig() {
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> serverConfig = (Map<String, Object>) config.getOrDefault("servers", Map.of());
        assumeServersOnline = (boolean) serverConfig.getOrDefault("assume-all-online", true);
        
        plugin.getLogger().info("Server status monitoring: assume-all-online = " + assumeServersOnline);
    }
    
    private void initializeServerStatus() {
        // Initialize all servers based on configuration
        plugin.getServer().getAllServers().forEach(server -> {
            String serverId = server.getServerInfo().getName();
            serverStatus.put(serverId, assumeServersOnline);
            plugin.getLogger().info("Initialized server " + serverId + " as " + 
                  (assumeServersOnline ? "online" : "pending ping check"));
        });
    }
    
    private void startMonitoring() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // If we're assuming all servers are online, we don't need active monitoring
        if (assumeServersOnline) {
            plugin.getLogger().info("Server status monitoring is minimal because assume-all-online is true");
            return;
        }
        
        // Check server status every 30 seconds
        plugin.getServer().getScheduler().buildTask(plugin, this::checkServers)
                .repeat(30, TimeUnit.SECONDS)
                .schedule();
    }
    
    private void checkServers() {
        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            String serverId = server.getServerInfo().getName();
            
            // Ping server asynchronously
            server.ping().thenAccept(ping -> {
                boolean wasOnline = serverStatus.getOrDefault(serverId, false);
                boolean isOnline = ping != null;
                
                // Status changed
                if (wasOnline != isOnline) {
                    serverStatus.put(serverId, isOnline);
                    onServerStatusChange(serverId, isOnline);
                }
            }).exceptionally(ex -> {
                // Failed to ping, server is offline
                boolean wasOnline = serverStatus.getOrDefault(serverId, false);
                if (wasOnline) {
                    serverStatus.put(serverId, false);
                    onServerStatusChange(serverId, false);
                }
                return null;
            });
        }
    }
    
    private void onServerStatusChange(String serverId, boolean isOnline) {
        plugin.getLogger().info("Server " + serverId + " is now " + (isOnline ? "online" : "offline"));
        
        // Get webhook URL from config
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> discordConfig = (Map<String, Object>) config.getOrDefault("discord", Map.of());
        
        String webhookUrl = (String) discordConfig.getOrDefault("server-status-webhook-url", "");
        if (!webhookUrl.isEmpty()) {
            WebhookManager.sendServerStatusWebhook(webhookUrl, serverId, !isOnline);
        }
    }
    
    /**
     * Check if a server is online
     * @param serverId Server ID to check
     * @return true if server is online, false otherwise
     */
    public boolean isServerOnline(String serverId) {
        // If we're assuming all servers are online, always return true
        if (assumeServersOnline) {
            return true;
        }
        
        // Check if the server exists in our status map
        if (!serverStatus.containsKey(serverId)) {
            // If the server exists in Velocity but we haven't tracked it, add it
            if (plugin.getServer().getServer(serverId).isPresent()) {
                serverStatus.put(serverId, true);
                return true;
            }
            return false;
        }
        
        return serverStatus.getOrDefault(serverId, false);
    }
    
    /**
     * Force update server status
     * @param serverId Server ID
     * @param isOnline Server status
     */
    public void setServerStatus(String serverId, boolean isOnline) {
        boolean wasOnline = serverStatus.getOrDefault(serverId, false);
        
        if (wasOnline != isOnline) {
            serverStatus.put(serverId, isOnline);
            onServerStatusChange(serverId, isOnline);
        }
    }
    
    public void reload() {
        loadConfig();
        initializeServerStatus();
    }
    
    public void shutdown() {
        isRunning = false;
    }
} 