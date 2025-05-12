package com.midenium.mannouncer.utils;

import com.midenium.mannouncer.MAnnouncer;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;

public class MessageUtils {

    private static final int DEFAULT_DELAY = 50; // ms
    private static final int DEFAULT_MAX_CHARS = 100;

    /**
     * Send a message with typing effect
     * @param plugin Plugin instance
     * @param player Player to send to
     * @param message Message component
     */
    public static void sendTypingMessage(MAnnouncer plugin, Player player, Component message) {
        // Convert component to plain text
        String text = PlainTextComponentSerializer.plainText().serialize(message);
        
        // Get typing settings from config
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> typingConfig = (Map<String, Object>) config.getOrDefault("typing", Map.of());
        
        int delay = getInt(typingConfig, "delay", DEFAULT_DELAY);
        int maxChars = getInt(typingConfig, "max-chars", DEFAULT_MAX_CHARS);
        
        // Limit text length for performance
        if (text.length() > maxChars) {
            // If too long, send normally
            player.sendMessage(message);
            return;
        }
        
        // Split into chunks for typing effect
        AtomicInteger index = new AtomicInteger(0);
        
        plugin.getServer().getScheduler().buildTask(plugin, new Runnable() {
            @Override
            public void run() {
                int i = index.getAndIncrement();
                if (i < text.length()) {
                    String partialText = text.substring(0, i + 1);
                    player.sendMessage(Component.text(partialText));
                    
                    // Schedule next character
                    plugin.getServer().getScheduler().buildTask(plugin, this)
                            .delay(delay, TimeUnit.MILLISECONDS)
                            .schedule();
                }
            }
        }).schedule();
    }
    
    private static int getInt(Map<String, Object> map, String key, int defaultValue) {
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
} 