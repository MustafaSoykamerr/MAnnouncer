package com.midenium.mannouncer.managers;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookManager {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}):(#[0-9A-Fa-f]{6})(.+?)\\s*/gradient>");
    
    /**
     * Sends a message to a Discord webhook asynchronously
     * @param webhookUrl The Discord webhook URL
     * @param message The message content
     * @param type The type of announcement (for logging)
     */
    public static void sendWebhookMessage(String webhookUrl, String message, String type) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        
        EXECUTOR.submit(() -> {
            try {
                // Önce mesajdan tüm MiniMessage formatlamalarını temizle
                String cleanMessage = stripMiniMessageFormatting(message);
                
                // Split message for embed if it contains a line break
                String title = "";
                String description = cleanMessage;
                int newlineIndex = cleanMessage.indexOf('\n');
                if (newlineIndex > 0) {
                    title = cleanMessage.substring(0, newlineIndex).trim();
                    description = cleanMessage.substring(newlineIndex + 1).trim();
                }
                
                // Build the JSON payload
                String json = String.format(
                    "{\"embeds\":[{" +
                    "\"title\":\"%s\"," +
                    "\"description\":\"%s\"," +
                    "\"color\":%d," +
                    "\"footer\":{\"text\":\"Powered by mAnnouncer\"}" +
                    "}]}",
                    escapeJson(title), escapeJson(description), 3447003
                );
                
                // Send POST request
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                // Silent fail
            }
        });
    }
    
    /**
     * Sends a streamer announcement to a Discord webhook
     * @param webhookUrl The Discord webhook URL
     * @param streamerName The name of the streamer
     * @param platform The platform they are streaming on
     * @param streamUrl The URL of the stream
     */
    public static void sendStreamerLiveWebhook(String webhookUrl, String streamerName, String platform, String streamUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        
        EXECUTOR.submit(() -> {
            try {
                // Tüm parametrelerden formatlamayı temizle
                String cleanStreamerName = stripMiniMessageFormatting(streamerName);
                String cleanPlatform = stripMiniMessageFormatting(platform);
                String cleanStreamUrl = streamUrl; // URL'de formatlamaya gerek yok
                
                String json = String.format(
                    "{\"embeds\":[{" +
                    "\"title\":\"%s\"," +
                    "\"description\":\"%s\"," +
                    "\"url\":\"%s\"," +
                    "\"color\":16711680" +
                    "}]}",
                    escapeJson("🔴 LIVE: " + cleanStreamerName),
                    escapeJson(cleanStreamerName + " is streaming on " + cleanPlatform),
                    escapeJson(cleanStreamUrl)
                );
                
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                // Silent fail
            }
        });
    }
    
    /**
     * Sends a server status webhook message
     * @param webhookUrl The Discord webhook URL
     * @param serverName The name of the server
     * @param isOffline Whether the server is offline
     */
    public static void sendServerStatusWebhook(String webhookUrl, String serverName, boolean isOffline) {
        if (webhookUrl == null || webhookUrl.isEmpty()) return;
        
        EXECUTOR.submit(() -> {
            try {
                // Sunucu adından formatlamayı temizle
                String cleanServerName = stripMiniMessageFormatting(serverName);
                
                String json = String.format(
                    "{\"embeds\":[{" +
                    "\"title\":\"%s\"," +
                    "\"description\":\"%s\"," +
                    "\"color\":%s" +
                    "}]}",
                    escapeJson(isOffline ? "⚠️ Server Offline" : "✅ Server Online"),
                    escapeJson("The server " + cleanServerName + " is currently " + 
                            (isOffline ? "offline" : "online") + "."),
                    isOffline ? "16711680" : "65280"
                );
                
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                // Silent fail
            }
        });
    }
    
    private static String stripMiniMessageFormatting(String message) {
        if (message == null) return "";
        
        // Tüm formatlamaları ve renk kodlarını tamamen kaldır
        String result = message;
        
        // Gradient formatlamasını kaldır
        result = result.replaceAll("<gradient:[^>]+>", "").replaceAll("</gradient>", "");
        
        // Renk kodlarını kaldır (örn: <red>text</red>, <#FF0000>text</#FF0000>)
        result = result.replaceAll("<[a-zA-Z0-9#_:]+>", "").replaceAll("</[a-zA-Z0-9#_:]+>", "");
        
        // Click ve hover gibi etkileşim formatlamalarını kaldır
        result = result.replaceAll("<click:[^>]+>", "").replaceAll("</click>", "");
        result = result.replaceAll("<hover:[^>]+>", "").replaceAll("</hover>", "");
        
        // Kalan tüm MiniMessage formatlamalarını kaldır
        result = result.replaceAll("<[^>]+>", "");
        
        return result;
    }
    
    private static String parseGradientTags(String message) {
        if (message == null) return "";
        
        // Discord gradientleri desteklemediği için doğrudan tüm formatları kaldıralım
        return stripMiniMessageFormatting(message);
    }
    
    private static String escapeJson(String text) {
        if (text == null) return "";
        
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * Shutdown webhook executor service
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
} 