package com.midenium.mannouncer.models;

public enum AnnouncementType {
    CHAT("chat-announcements.yml", "chat_announcements"),
    BOSSBAR("bossbar-announcements.yml", "bossbar_announcements"),
    TITLE("title-announcements.yml", "title_announcements"),
    SUBTITLE("subtitle-announcements.yml", "subtitle_announcements"),
    ADVANCEMENT("advancement-announcements.yml", "advancement_announcements");
    
    private final String configFileName;
    private final String configKey;
    
    AnnouncementType(String configFileName, String configKey) {
        this.configFileName = configFileName;
        this.configKey = configKey;
    }
    
    public String getConfigFileName() {
        return configFileName;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public String getPermissionNode() {
        return name().toLowerCase();
    }
    
    public static AnnouncementType fromString(String typeName) {
        try {
            return valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
} 