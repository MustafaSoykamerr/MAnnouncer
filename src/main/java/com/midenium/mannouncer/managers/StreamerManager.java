package com.midenium.mannouncer.managers;

import com.midenium.mannouncer.MAnnouncer;
import com.midenium.mannouncer.models.Streamer;
import com.midenium.mannouncer.models.StreamerPlatform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages streamers and their live status
 */
public class StreamerManager {

    private final MAnnouncer plugin;
    private final Map<String, Streamer> streamers = new ConcurrentHashMap<>();
    private boolean isRunning = false;
    
    public StreamerManager(MAnnouncer plugin) {
        this.plugin = plugin;
        loadStreamers();
        startChecking();
    }
    
    private void loadStreamers() {
        streamers.clear();
        
        Map<String, Object> streamersConfig = plugin.getConfigManager().getStreamersConfig();
        Object streamersObj = streamersConfig.get("streamers");
        
        if (streamersObj instanceof Map) {
            Map<String, Object> streamersMap = (Map<String, Object>) streamersObj;
            
            for (Map.Entry<String, Object> entry : streamersMap.entrySet()) {
                String id = entry.getKey();
                
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> streamerConfig = (Map<String, Object>) entry.getValue();
                    Streamer streamer = new Streamer(id, streamerConfig);
                    streamers.put(id, streamer);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + streamers.size() + " streamers.");
    }
    
    private void startChecking() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        
        // Get check interval from config
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> streamersConfig = (Map<String, Object>) config.getOrDefault("streamers", Map.of());
        int checkInterval = getInt(streamersConfig, "check-interval", 60);
        
        plugin.getLogger().info("Starting streamer status checking (interval: " + checkInterval + "s)");
        
        // Start periodic checks
        plugin.getServer().getScheduler().buildTask(plugin, this::checkStreamers)
                .repeat(checkInterval, TimeUnit.SECONDS)
                .schedule();
    }
    
    private void checkStreamers() {
        // Get simulation settings from config
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> streamersConfig = (Map<String, Object>) config.getOrDefault("streamers", Map.of());
        
        // Simulation settings
        Map<String, Object> simulationConfig = (Map<String, Object>) streamersConfig.getOrDefault("simulation", Map.of());
        boolean simulationEnabled = (boolean) simulationConfig.getOrDefault("enabled", true);
        int changeProbability = getInt(simulationConfig, "change-probability", 10);
        
        // Check each streamer
        for (Streamer streamer : streamers.values()) {
            streamer.setLastCheck(System.currentTimeMillis());
            
            if (simulationEnabled) {
                // Simulate status change based on configured probability
                long now = System.currentTimeMillis();
                boolean wasLive = streamer.isLive();
                
                // Platform-based check - will work for all platforms
                boolean isLive = false;
                String streamerId = streamer.getId();
                StreamerPlatform platform = streamer.getPlatform();
                
                // Apply platform-specific check
                switch(platform) {
                    case TWITCH:
                        // Twitch status simulation
                        isLive = (streamerId.hashCode() + now) % changeProbability == 0;
                        break;
                    case KICK:
                        // Kick status simulation (different algorithm)
                        isLive = (streamerId.hashCode() + now / 1000) % changeProbability == 1;
                        break;
                    case YOUTUBE:
                        // YouTube status simulation (another algorithm)
                        isLive = (streamerId.hashCode() + now / 5000) % changeProbability == 2;
                        break;
                    default:
                        // General status simulation
                        isLive = (streamerId.hashCode() + now) % changeProbability == 0;
                }
                
                streamer.setLive(isLive);
                
                if (isLive && !wasLive) {
                    // Newly live - use platform-specific URL format
                    streamer.setStreamUrl(streamer.getPlatform().getStreamUrl(streamerId));
                    announceStreamer(streamer);
                }
            }
        }
    }
    
    private void announceStreamer(Streamer streamer) {
        // Check if enough time has passed since last announcement
        long now = System.currentTimeMillis();
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> streamersConfig = (Map<String, Object>) config.getOrDefault("streamers", Map.of());
        long globalCooldown = getLong(streamersConfig, "cooldown", 1800) * 1000;
        
        if (now - streamer.getLastAnnounced() < globalCooldown) {
            return;
        }
        
        streamer.setLastAnnounced(now);
        
        // Send Discord webhook if configured
        String webhookUrl = streamer.getWebhookUrl();
        if (webhookUrl.isEmpty()) {
            // Try global webhook URL
            webhookUrl = getString(streamersConfig, "default-webhook-url", "");
        }
        
        if (!webhookUrl.isEmpty()) {
            WebhookManager.sendStreamerLiveWebhook(
                    webhookUrl, 
                    streamer.getId(), 
                    streamer.getPlatform().getId(), 
                    streamer.getStreamUrl()
            );
        }
        
        // Send in-game announcements
        Map<String, Object> messagesConfig = plugin.getConfigManager().getMessagesConfig();
        Map<String, String> placeholders = Map.of(
                "streamer", streamer.getId(),
                "platform", streamer.getPlatform().getId(),
                "url", streamer.getStreamUrl()
        );
        
        // Announce to each server configured for this streamer
        for (String serverId : streamer.getServers()) {
            // Skip if server doesn't exist
            if (serverId.equals("all")) {
                // Announce to all servers
                plugin.getServer().getAllServers().forEach(server -> {
                    announceToServer(streamer, server.getServerInfo().getName(), messagesConfig, placeholders);
                });
            } else {
                // Announce to specific server
                if (plugin.getServer().getServer(serverId).isPresent()) {
                    announceToServer(streamer, serverId, messagesConfig, placeholders);
                }
            }
        }
    }
    
    private void announceToServer(Streamer streamer, String serverId, Map<String, Object> messagesConfig, Map<String, String> placeholders) {
        // Get announcement types
        List<String> types = streamer.getAnnouncementTypes();
        
        if (types.contains("chat")) {
            sendChatAnnouncement(streamer, serverId, messagesConfig, placeholders);
        }
        
        if (types.contains("title")) {
            sendTitleAnnouncement(streamer, serverId, messagesConfig, placeholders);
        }
        
        if (types.contains("bossbar")) {
            sendBossbarAnnouncement(streamer, serverId, messagesConfig, placeholders);
        }
    }
    
    private void sendChatAnnouncement(Streamer streamer, String serverId, Map<String, Object> messagesConfig, Map<String, String> placeholders) {
        // Get message template
        String message;
        if (streamer.hasCustomMessage("chat")) {
            message = streamer.getCustomMessage("chat");
        } else {
            Map<String, Object> streamersMessages = (Map<String, Object>) messagesConfig.getOrDefault("streamers", Map.of());
            message = getString(streamersMessages, "live-chat", "<red>🔴 LIVE</red> <white>» {streamer} is now streaming on {platform}!</white>");
        }
        
        // Apply placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Parse with MiniMessage
        Component component = MiniMessage.miniMessage().deserialize(message);
        
        // Send to all players on this server
        plugin.getServer().getServer(serverId).ifPresent(server -> {
            server.sendMessage(component);
        });
    }
    
    private void sendTitleAnnouncement(Streamer streamer, String serverId, Map<String, Object> messagesConfig, Map<String, String> placeholders) {
        // Get message templates
        String titleMsg;
        String subtitleMsg;
        
        if (streamer.hasCustomMessage("title")) {
            titleMsg = streamer.getCustomMessage("title");
        } else {
            Map<String, Object> streamersMessages = (Map<String, Object>) messagesConfig.getOrDefault("streamers", Map.of());
            titleMsg = getString(streamersMessages, "live-title", "<red>🔴 LIVE</red>");
        }
        
        if (streamer.hasCustomMessage("subtitle")) {
            subtitleMsg = streamer.getCustomMessage("subtitle");
        } else {
            Map<String, Object> streamersMessages = (Map<String, Object>) messagesConfig.getOrDefault("streamers", Map.of());
            subtitleMsg = getString(streamersMessages, "live-subtitle", "<white>{streamer} is now streaming!</white>");
        }
        
        // Apply placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            titleMsg = titleMsg.replace("{" + entry.getKey() + "}", entry.getValue());
            subtitleMsg = subtitleMsg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Parse with MiniMessage
        Component title = MiniMessage.miniMessage().deserialize(titleMsg);
        Component subtitle = MiniMessage.miniMessage().deserialize(subtitleMsg);
        
        // Create Velocity title object
        net.kyori.adventure.title.Title titleObj = net.kyori.adventure.title.Title.title(
                title,
                subtitle,
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(3000),
                        java.time.Duration.ofMillis(500)
                )
        );
        
        // Send to all players on this server
        plugin.getServer().getServer(serverId).ifPresent(server -> {
            server.getPlayersConnected().forEach(player -> {
                player.showTitle(titleObj);
            });
        });
    }
    
    private void sendBossbarAnnouncement(Streamer streamer, String serverId, Map<String, Object> messagesConfig, Map<String, String> placeholders) {
        // Get message template
        String message;
        if (streamer.hasCustomMessage("bossbar")) {
            message = streamer.getCustomMessage("bossbar");
        } else {
            Map<String, Object> streamersMessages = (Map<String, Object>) messagesConfig.getOrDefault("streamers", Map.of());
            message = getString(streamersMessages, "live-chat", "<red>🔴 LIVE</red> <white>» {streamer} is now streaming on {platform}!</white>");
        }
        
        // Apply placeholders
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // Parse with MiniMessage
        Component component = MiniMessage.miniMessage().deserialize(message);
        
        // Create boss bar
        net.kyori.adventure.bossbar.BossBar bossBar = net.kyori.adventure.bossbar.BossBar.bossBar(
                component,
                1.0f,
                net.kyori.adventure.bossbar.BossBar.Color.RED,
                net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS
        );
        
        // Show to all players on this server
        plugin.getServer().getServer(serverId).ifPresent(server -> {
            server.getPlayersConnected().forEach(player -> {
                player.showBossBar(bossBar);
            });
            
            // Hide after 15 seconds
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                server.getPlayersConnected().forEach(player -> {
                    player.hideBossBar(bossBar);
                });
            }).delay(15, TimeUnit.SECONDS).schedule();
        });
    }
    
    // Utility methods
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    public void reload() {
        loadStreamers();
    }
    
    public void shutdown() {
        isRunning = false;
    }
    
    public Map<String, Streamer> getStreamers() {
        return streamers;
    }
} 