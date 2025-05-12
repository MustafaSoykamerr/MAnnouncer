package com.midenium.mannouncer.managers;

import com.midenium.mannouncer.MAnnouncer;
import com.midenium.mannouncer.models.Announcement;
import com.midenium.mannouncer.models.AnnouncementType;
import com.midenium.mannouncer.utils.MessageUtils;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private final MAnnouncer plugin;
    private final Map<String, Map<AnnouncementType, Map<String, Announcement>>> announcements = new ConcurrentHashMap<>();
    private final Map<String, BossBar> activeBossBars = new ConcurrentHashMap<>();
    
    public AnnouncementManager(MAnnouncer plugin) {
        this.plugin = plugin;
        loadAnnouncements();
    }
    
    public void loadAnnouncements() {
        announcements.clear();
        
        // Get all server configs from ConfigManager
        Map<String, Map<String, Object>> serverConfigs = plugin.getConfigManager().getServerConfigs();
        
        for (Map.Entry<String, Map<String, Object>> serverEntry : serverConfigs.entrySet()) {
            String serverId = serverEntry.getKey();
            Map<String, Object> serverConfig = serverEntry.getValue();
            
            Map<AnnouncementType, Map<String, Announcement>> serverAnnouncements = new HashMap<>();
            
            // Load each announcement type for this server
            for (AnnouncementType type : AnnouncementType.values()) {
                String configKey = type.getConfigKey();
                
                if (serverConfig.containsKey(configKey)) {
                    Object typeConfig = serverConfig.get(configKey);
                    if (typeConfig instanceof Map) {
                        Map<String, Object> announcementTypeConfig = (Map<String, Object>) typeConfig;
                        
                        // Load announcements for this type
                        Object announcementsObj = announcementTypeConfig.get("announcements");
                        if (announcementsObj instanceof Map) {
                            Map<String, Object> announcementConfigs = (Map<String, Object>) announcementsObj;
                            Map<String, Announcement> typeAnnouncements = new HashMap<>();
                            
                            for (Map.Entry<String, Object> announcementEntry : announcementConfigs.entrySet()) {
                                String id = announcementEntry.getKey();
                                
                                if (announcementEntry.getValue() instanceof Map) {
                                    Map<String, Object> announcementConfig = (Map<String, Object>) announcementEntry.getValue();
                                    Announcement announcement = new Announcement(id, serverId, type, announcementConfig);
                                    typeAnnouncements.put(id, announcement);
                                }
                            }
                            
                            serverAnnouncements.put(type, typeAnnouncements);
                        }
                    }
                } else {
                    // No config for this type, create empty map
                    serverAnnouncements.put(type, new HashMap<>());
                }
            }
            
            announcements.put(serverId, serverAnnouncements);
        }
        
        plugin.getLogger().info("Loaded " + countAnnouncements() + " announcements.");
    }
    
    private int countAnnouncements() {
        return announcements.values().stream()
                .flatMap(typeMap -> typeMap.values().stream())
                .mapToInt(announcementMap -> announcementMap.size())
                .sum();
    }
    
    public List<Announcement> getScheduledAnnouncements() {
        return announcements.values().stream()
                .flatMap(typeMap -> typeMap.values().stream())
                .flatMap(announcementMap -> announcementMap.values().stream())
                .filter(Announcement::isScheduled)
                .filter(Announcement::isEnabled)
                .collect(Collectors.toList());
    }
    
    public Optional<Announcement> getAnnouncement(String serverId, AnnouncementType type, String id) {
        Map<AnnouncementType, Map<String, Announcement>> serverAnnouncements = announcements.get(serverId);
        if (serverAnnouncements == null) {
            return Optional.empty();
        }
        
        Map<String, Announcement> typeAnnouncements = serverAnnouncements.get(type);
        if (typeAnnouncements == null) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(typeAnnouncements.get(id));
    }
    
    /**
     * Get all announcements for a specific server
     * 
     * @param serverId The server ID
     * @return A map of announcement types to announcement maps, or null if the server doesn't exist
     */
    public Map<AnnouncementType, Map<String, Announcement>> getAnnouncementsForServer(String serverId) {
        return announcements.get(serverId);
    }
    
    public void sendAnnouncement(Announcement announcement) {
        sendAnnouncement(announcement, Collections.emptyMap());
    }
    
    public void sendAnnouncement(Announcement announcement, Map<String, String> placeholders) {
        String serverId = announcement.getServerId();
        
        // Check if server exists
        Optional<RegisteredServer> optServer = plugin.getServer().getServer(serverId);
        if (optServer.isEmpty()) {
            return;
        }
        
        RegisteredServer server = optServer.get();
        if (server.getPlayersConnected().isEmpty()) {
            // No players on server, don't send
            return;
        }
        
        // Check permission predicate
        Predicate<Player> permissionPredicate = player -> {
            if (!announcement.hasPermissionRequirement()) {
                return true;
            }
            
            String permissionNode = "mannouncer.announcement." + 
                    announcement.getType().getPermissionNode() + "." + 
                    serverId + "." + announcement.getId();
            
            return player.hasPermission(permissionNode);
        };
        
        // Get message with placeholders
        String messageText = announcement.getMessage();
        if (!placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = sanitizeInput(entry.getValue());
                messageText = messageText.replace("{" + entry.getKey() + "}", value);
            }
        }
        
        Component message = MiniMessage.miniMessage().deserialize(messageText);
        
        // Send based on announcement type
        switch (announcement.getType()) {
            case CHAT -> sendChatAnnouncement(server, message, announcement, permissionPredicate);
            case BOSSBAR -> sendBossBarAnnouncement(server, message, announcement, permissionPredicate);
            case TITLE -> sendTitleAnnouncement(server, message, announcement, permissionPredicate);
            case SUBTITLE -> sendSubtitleAnnouncement(server, message, announcement, permissionPredicate);
            case ADVANCEMENT -> sendAdvancementAnnouncement(server, message, announcement, permissionPredicate, placeholders);
        }
        
        // Play sound if configured
        if (announcement.hasSound()) {
            sendSound(server, announcement, permissionPredicate);
        }
        
        // Send to webhook if configured
        if (announcement.hasWebhook()) {
            String webhookUrl = announcement.getWebhookUrl();
            final String finalMessageText = sanitizeInput(messageText);
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                WebhookManager.sendWebhookMessage(webhookUrl, finalMessageText, announcement.getType().name());
            }).schedule();
        }
        
        // Update last sent time
        announcement.setLastSent(System.currentTimeMillis());
    }
    
    private void sendChatAnnouncement(RegisteredServer server, Component message, Announcement announcement, Predicate<Player> permissionPredicate) {
        // If typing effect is enabled, use typing effect
        if (announcement.isTypingEffect() && isTypingEffectEnabled()) {
            for (Player player : server.getPlayersConnected()) {
                if (permissionPredicate.test(player)) {
                    MessageUtils.sendTypingMessage(plugin, player, message);
                }
            }
        } else {
            // Regular chat message
            Audience audience = Audience.audience(
                    server.getPlayersConnected().stream()
                            .filter(permissionPredicate)
                            .collect(Collectors.toList())
            );
            
            audience.sendMessage(message);
        }
    }
    
    private void sendBossBarAnnouncement(RegisteredServer server, Component message, Announcement announcement, Predicate<Player> permissionPredicate) {
        // Create boss bar
        BossBar.Color color = getBossBarColor(announcement.getBossbarColor());
        BossBar.Overlay overlay = getBossBarOverlay(announcement.getBossbarStyle());
        
        BossBar bossBar = BossBar.bossBar(message, 1.0f, color, overlay);
        
        // Add to active boss bars
        String bossBarKey = announcement.getServerId() + ":" + announcement.getId();
        activeBossBars.put(bossBarKey, bossBar);
        
        // Show to players
        Audience audience = Audience.audience(
                server.getPlayersConnected().stream()
                        .filter(permissionPredicate)
                        .collect(Collectors.toList())
        );
        
        audience.showBossBar(bossBar);
        
        // Schedule removal after duration
        int duration = announcement.getBossbarDuration();
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            audience.hideBossBar(bossBar);
            activeBossBars.remove(bossBarKey);
        }).delay(duration, TimeUnit.SECONDS).schedule();
    }
    
    private void sendTitleAnnouncement(RegisteredServer server, Component message, Announcement announcement, Predicate<Player> permissionPredicate) {
        Title title = Title.title(
                message,
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(announcement.getTitleFadeIn() * 50L),
                        Duration.ofMillis(announcement.getTitleStay() * 50L),
                        Duration.ofMillis(announcement.getTitleFadeOut() * 50L)
                )
        );
        
        Audience audience = Audience.audience(
                server.getPlayersConnected().stream()
                        .filter(permissionPredicate)
                        .collect(Collectors.toList())
        );
        
        audience.showTitle(title);
    }
    
    private void sendSubtitleAnnouncement(RegisteredServer server, Component message, Announcement announcement, Predicate<Player> permissionPredicate) {
        Title title = Title.title(
                Component.empty(),
                message,
                Title.Times.times(
                        Duration.ofMillis(announcement.getTitleFadeIn() * 50L),
                        Duration.ofMillis(announcement.getTitleStay() * 50L),
                        Duration.ofMillis(announcement.getTitleFadeOut() * 50L)
                )
        );
        
        Audience audience = Audience.audience(
                server.getPlayersConnected().stream()
                        .filter(permissionPredicate)
                        .collect(Collectors.toList())
        );
        
        audience.showTitle(title);
    }
    
    private void sendAdvancementAnnouncement(RegisteredServer server, Component message, Announcement announcement, 
                                             Predicate<Player> permissionPredicate, Map<String, String> placeholders) {
        Component description = Component.empty();
        if (!announcement.getDescription().isEmpty()) {
            String descText = announcement.getDescription();
            
            // Apply placeholders
            if (!placeholders.isEmpty()) {
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    descText = descText.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            
            description = MiniMessage.miniMessage().deserialize(descText);
        }
        
        // Send to each player (no common way to show advancements via Adventure API)
        for (Player player : server.getPlayersConnected()) {
            if (permissionPredicate.test(player)) {
                // Advancement toast is not directly supported in Adventure API
                // For now, we'll send a title and subtitle as fallback
                Title title = Title.title(
                        message,
                        description,
                        Title.Times.times(
                                Duration.ofMillis(10 * 50L),
                                Duration.ofMillis(40 * 50L),
                                Duration.ofMillis(10 * 50L)
                        )
                );
                
                player.showTitle(title);
            }
        }
    }
    
    private void sendSound(RegisteredServer server, Announcement announcement, Predicate<Player> permissionPredicate) {
        String soundName = announcement.getSound();
        float volume = announcement.getVolume();
        float pitch = announcement.getPitch();
        
        try {
            // Try to parse as Minecraft sound key
            Sound sound = Sound.sound(
                    Key.key(soundName),
                    Sound.Source.MASTER,
                    volume,
                    pitch
            );
            
            Audience audience = Audience.audience(
                    server.getPlayersConnected().stream()
                            .filter(permissionPredicate)
                            .collect(Collectors.toList())
            );
            
            audience.playSound(sound);
        } catch (Exception e) {
            plugin.getLogger().warn("Invalid sound: " + soundName);
        }
    }
    
    private BossBar.Color getBossBarColor(String colorName) {
        try {
            return BossBar.Color.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }
    
    private BossBar.Overlay getBossBarOverlay(String styleName) {
        switch (styleName.toUpperCase()) {
            case "SEGMENTED_6":
                return BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10":
                return BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12":
                return BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20":
                return BossBar.Overlay.NOTCHED_20;
            default:
                return BossBar.Overlay.PROGRESS;
        }
    }
    
    public void setAnnouncementEnabled(String serverId, AnnouncementType type, String id, boolean enabled) {
        Optional<Announcement> announcement = getAnnouncement(serverId, type, id);
        if (announcement.isPresent()) {
            announcement.get().setEnabled(enabled);
            saveAnnouncement(announcement.get());
        }
    }
    
    public void saveAnnouncement(Announcement announcement) {
        // Get server config
        Map<String, Object> serverConfig = plugin.getConfigManager().getServerConfig(announcement.getServerId());
        AnnouncementType type = announcement.getType();
        
        // Get announcement type config
        Object typeConfigObj = serverConfig.get(type.getConfigKey());
        if (!(typeConfigObj instanceof Map)) {
            return;
        }
        
        Map<String, Object> typeConfig = (Map<String, Object>) typeConfigObj;
        
        // Get announcements map
        Object announcementsObj = typeConfig.get("announcements");
        if (!(announcementsObj instanceof Map)) {
            return;
        }
        
        Map<String, Object> announcementsMap = (Map<String, Object>) announcementsObj;
        
        // Update announcement config
        announcementsMap.put(announcement.getId(), announcement.toMap());
        
        // Save to file
        plugin.getConfigManager().saveServerConfig(
                announcement.getServerId(),
                type.getConfigFileName(),
                Map.of("announcements", announcementsMap)
        );
    }
    
    private boolean isTypingEffectEnabled() {
        Map<String, Object> mainConfig = plugin.getConfigManager().getMainConfig();
        Object typingObj = mainConfig.get("typing");
        
        if (typingObj instanceof Map) {
            Map<String, Object> typingConfig = (Map<String, Object>) typingObj;
            Object enabledObj = typingConfig.get("enabled");
            return enabledObj instanceof Boolean && (Boolean) enabledObj;
        }
        
        return false;
    }
    
    private boolean isSanitizationEnabled() {
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> securityConfig = (Map<String, Object>) config.getOrDefault("security", Map.of());
        Object sanitizeObj = securityConfig.getOrDefault("sanitize-input", true);
        return sanitizeObj instanceof Boolean && (Boolean) sanitizeObj;
    }
    
    private String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Check if sanitization is enabled
        boolean sanitize = isSanitizationEnabled();
        if (!sanitize) {
            return input;
        }
        
        // Simple sanitization - remove potentially harmful characters
        return input.replaceAll("[\\<\\>\\{\\}\\[\\]\\=\\;]", "");
    }
    
    public void reload() {
        // Clear active boss bars
        for (BossBar bossBar : activeBossBars.values()) {
            plugin.getServer().getAllPlayers().forEach(player -> player.hideBossBar(bossBar));
        }
        activeBossBars.clear();
        
        // Reload announcements
        loadAnnouncements();
    }
} 