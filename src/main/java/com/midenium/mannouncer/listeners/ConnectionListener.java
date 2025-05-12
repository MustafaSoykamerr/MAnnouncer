package com.midenium.mannouncer.listeners;

import com.midenium.mannouncer.MAnnouncer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.sound.Sound;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionListener {

    private final MAnnouncer plugin;
    private final Map<UUID, Set<String>> playerJoinedServers = new ConcurrentHashMap<>();
    
    public ConnectionListener(MAnnouncer plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe(order = PostOrder.NORMAL)
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverId = event.getServer().getServerInfo().getName();
        
        // Check if this is the player's first join to this server
        UUID playerId = player.getUniqueId();
        Set<String> joinedServers = playerJoinedServers.computeIfAbsent(
                playerId, k -> ConcurrentHashMap.newKeySet());
        
        boolean isFirstJoin = joinedServers.add(serverId);
        
        if (isFirstJoin) {
            // This is the player's first join to this server, send welcome message
            sendWelcomeMessage(player, serverId);
        }
    }
    
    @Subscribe(order = PostOrder.NORMAL)
    public void onDisconnect(DisconnectEvent event) {
        // Clean up when player disconnects
        playerJoinedServers.remove(event.getPlayer().getUniqueId());
    }
    
    private void sendWelcomeMessage(Player player, String serverId) {
        // Get welcome message from config
        Map<String, Object> mainConfig = plugin.getConfigManager().getMainConfig();
        
        // Check if welcome messages are enabled
        Object announcementsObj = mainConfig.get("announcements");
        if (!(announcementsObj instanceof Map)) {
            return;
        }
        
        Map<String, Object> announcementsConfig = (Map<String, Object>) announcementsObj;
        Object welcomeObj = announcementsConfig.get("welcome");
        
        if (!(welcomeObj instanceof Map)) {
            return;
        }
        
        Map<String, Object> welcomeConfig = (Map<String, Object>) welcomeObj;
        boolean enabled = getBoolean(welcomeConfig, "enabled", true);
        
        if (!enabled) {
            return;
        }
        
        // Get message template
        String messageTemplate = getString(welcomeConfig, "message", "<green>Welcome to the server!");
        
        // Replace placeholders
        String message = messageTemplate
                .replace("{player}", player.getUsername())
                .replace("{server}", serverId);
        
        // Send message using MiniMessage
        Component component = MiniMessage.miniMessage().deserialize(message);
        player.sendMessage(component);
        
        // Play sound if configured
        String sound = getString(welcomeConfig, "sound", "");
        if (!sound.isEmpty()) {
            float volume = getFloat(welcomeConfig, "volume", 1.0f);
            float pitch = getFloat(welcomeConfig, "pitch", 1.0f);
            
            player.playSound(
                    Sound.sound(
                            Key.key(sound),
                            Sound.Source.MASTER,
                            volume,
                            pitch
                    )
            );
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
} 