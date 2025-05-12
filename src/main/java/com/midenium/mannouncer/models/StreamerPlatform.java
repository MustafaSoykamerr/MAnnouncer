package com.midenium.mannouncer.models;

public enum StreamerPlatform {
    TWITCH("twitch", "https://twitch.tv/"),
    KICK("kick", "https://kick.com/"),
    YOUTUBE("youtube", "https://youtube.com/channel/");
    
    private final String id;
    private final String baseUrl;
    
    StreamerPlatform(String id, String baseUrl) {
        this.id = id;
        this.baseUrl = baseUrl;
    }
    
    public String getId() {
        return id;
    }
    
    public String getBaseUrl() {
        return baseUrl;
    }
    
    public String getStreamUrl(String username) {
        return baseUrl + username;
    }
    
    public static StreamerPlatform fromString(String platformName) {
        for (StreamerPlatform platform : values()) {
            if (platform.id.equalsIgnoreCase(platformName)) {
                return platform;
            }
        }
        return TWITCH; // Default to Twitch
    }
} 