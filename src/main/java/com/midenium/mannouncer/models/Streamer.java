package com.midenium.mannouncer.models;

import java.util.List;
import java.util.Map;

public class Streamer {
    
    private final String id;
    private final StreamerPlatform platform;
    private final List<String> servers;
    private final List<String> announcementTypes;
    private final int interval;
    private final String webhookUrl;
    private final Map<String, String> customMessages;
    
    private boolean isLive;
    private String streamUrl;
    private long lastCheck;
    private long lastAnnounced;
    
    public Streamer(String id, Map<String, Object> config) {
        this.id = id;
        this.platform = StreamerPlatform.fromString(
                (String) config.getOrDefault("platform", "twitch"));
                
        this.servers = (List<String>) config.getOrDefault("servers", List.of("all"));
        this.announcementTypes = (List<String>) config.getOrDefault("announcement-types", List.of("chat"));
        this.interval = getInt(config, "interval", 60);
        this.webhookUrl = (String) config.getOrDefault("webhook-url", "");
        
        this.customMessages = (Map<String, String>) config.getOrDefault("messages", Map.of());
        
        this.isLive = false;
        this.streamUrl = "";
        this.lastCheck = 0L;
        this.lastAnnounced = 0L;
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
    
    public String getId() {
        return id;
    }
    
    public StreamerPlatform getPlatform() {
        return platform;
    }
    
    public List<String> getServers() {
        return servers;
    }
    
    public List<String> getAnnouncementTypes() {
        return announcementTypes;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public Map<String, String> getCustomMessages() {
        return customMessages;
    }
    
    public boolean isLive() {
        return isLive;
    }
    
    public void setLive(boolean live) {
        isLive = live;
    }
    
    public String getStreamUrl() {
        return streamUrl;
    }
    
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
    
    public long getLastCheck() {
        return lastCheck;
    }
    
    public void setLastCheck(long lastCheck) {
        this.lastCheck = lastCheck;
    }
    
    public long getLastAnnounced() {
        return lastAnnounced;
    }
    
    public void setLastAnnounced(long lastAnnounced) {
        this.lastAnnounced = lastAnnounced;
    }
    
    public boolean shouldCheck() {
        return System.currentTimeMillis() - lastCheck >= interval * 1000L;
    }
    
    public boolean matchesServer(String serverName) {
        return servers.contains("all") || servers.contains(serverName);
    }
    
    public boolean hasCustomMessage(String type) {
        return customMessages.containsKey(type);
    }
    
    public String getCustomMessage(String type) {
        return customMessages.getOrDefault(type, "");
    }
} 