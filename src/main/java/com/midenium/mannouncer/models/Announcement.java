package com.midenium.mannouncer.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Announcement {
    
    private final String id;
    private final String serverId;
    private final AnnouncementType type;
    private boolean enabled;
    private String message;
    private String description;
    private String sound;
    private float volume;
    private float pitch;
    private String permission;
    private String webhookUrl;
    private boolean scheduled;
    private int interval;
    private long cooldown;
    private long lastSent;
    
    // Type-specific properties
    private final Map<String, Object> properties = new HashMap<>();
    
    public Announcement(String id, String serverId, AnnouncementType type, Map<String, Object> config) {
        this.id = id;
        this.serverId = serverId;
        this.type = type;
        
        this.enabled = getBoolean(config, "enabled", true);
        this.message = getString(config, "message", "");
        this.description = getString(config, "description", "");
        this.sound = getString(config, "sound", "");
        this.volume = getFloat(config, "volume", 1.0f);
        this.pitch = getFloat(config, "pitch", 1.0f);
        this.permission = getString(config, "permission", "");
        this.webhookUrl = getString(config, "webhook-url", "");
        this.scheduled = getBoolean(config, "scheduled", false);
        this.interval = getInt(config, "interval", 300);
        this.cooldown = getLong(config, "cooldown", 0L);
        this.lastSent = 0L;
        
        // Load type-specific properties based on announcement type
        switch (type) {
            case CHAT -> {
                properties.put("typing-effect", getBoolean(config, "typing-effect", false));
            }
            case BOSSBAR -> {
                properties.put("color", getString(config, "color", "BLUE"));
                properties.put("style", getString(config, "style", "SOLID"));
                properties.put("duration", getInt(config, "duration", 10));
            }
            case TITLE, SUBTITLE -> {
                properties.put("fade-in", getInt(config, "fade-in", 10));
                properties.put("stay", getInt(config, "stay", 40));
                properties.put("fade-out", getInt(config, "fade-out", 10));
            }
            case ADVANCEMENT -> {
                properties.put("frame", getString(config, "frame", "CHALLENGE"));
            }
        }
    }
    
    // Utility methods for safe type conversions
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
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
    
    private float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
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
    
    public String getId() {
        return id;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public AnnouncementType getType() {
        return type;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getSound() {
        return sound;
    }
    
    public void setSound(String sound) {
        this.sound = sound;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = volume;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public boolean isScheduled() {
        return scheduled;
    }
    
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }
    
    public int getInterval() {
        return interval;
    }
    
    public void setInterval(int interval) {
        this.interval = interval;
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
    
    public long getLastSent() {
        return lastSent;
    }
    
    public void setLastSent(long lastSent) {
        this.lastSent = lastSent;
    }
    
    public boolean isTypingEffect() {
        return getBoolean(properties, "typing-effect", false);
    }
    
    public String getBossbarColor() {
        return getString(properties, "color", "BLUE");
    }
    
    public String getBossbarStyle() {
        return getString(properties, "style", "SOLID");
    }
    
    public int getBossbarDuration() {
        return getInt(properties, "duration", 10);
    }
    
    public int getTitleFadeIn() {
        return getInt(properties, "fade-in", 10);
    }
    
    public int getTitleStay() {
        return getInt(properties, "stay", 40);
    }
    
    public int getTitleFadeOut() {
        return getInt(properties, "fade-out", 10);
    }
    
    public String getAdvancementFrame() {
        return getString(properties, "frame", "CHALLENGE");
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public boolean isOnCooldown() {
        if (cooldown <= 0) {
            return false;
        }
        return (System.currentTimeMillis() - lastSent) < (cooldown * 1000);
    }
    
    public boolean shouldSend() {
        return enabled && !isOnCooldown();
    }
    
    public boolean hasPermissionRequirement() {
        return permission != null && !permission.isEmpty();
    }
    
    public boolean hasSound() {
        return sound != null && !sound.isEmpty();
    }
    
    public boolean hasWebhook() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }
    
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("enabled", enabled);
        map.put("message", message);
        if (!description.isEmpty()) {
            map.put("description", description);
        }
        if (!sound.isEmpty()) {
            map.put("sound", sound);
            map.put("volume", volume);
            map.put("pitch", pitch);
        }
        if (!permission.isEmpty()) {
            map.put("permission", permission);
        }
        if (!webhookUrl.isEmpty()) {
            map.put("webhook-url", webhookUrl);
        }
        map.put("scheduled", scheduled);
        map.put("interval", interval);
        map.put("cooldown", cooldown);
        
        // Add type-specific properties
        map.putAll(properties);
        
        return map;
    }
} 