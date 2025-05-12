package com.midenium.mannouncer.tasks;

import com.midenium.mannouncer.MAnnouncer;
import com.midenium.mannouncer.models.Announcement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.velocitypowered.api.scheduler.ScheduledTask;

public class AnnouncementTask {

    private final MAnnouncer plugin;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledTask task = null;
    
    public AnnouncementTask(MAnnouncer plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        if (running.get()) {
            return;
        }
        
        running.set(true);
        
        // Check for thread pool usage for better performance
        boolean useThreadPool = isThreadPoolEnabled();
        int checkFrequency = getAnnouncementCheckFrequency();
        
        // Schedule the task to run periodically to check for announcements
        task = plugin.getServer().getScheduler().buildTask(plugin, this::checkAnnouncements)
                .repeat(checkFrequency, TimeUnit.SECONDS)
                .schedule();
    }
    
    public void stop() {
        if (!running.get()) {
            return;
        }
        
        running.set(false);
        
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    private void checkAnnouncements() {
        if (!isAnnouncementsEnabled()) {
            return;
        }
        
        // Get all scheduled announcements
        List<Announcement> scheduledAnnouncements = plugin.getAnnouncementManager().getScheduledAnnouncements();
        
        if (scheduledAnnouncements.isEmpty()) {
            return;
        }
        
        // Check if we should batch process announcements
        boolean batchAnnouncements = isBatchAnnouncementsEnabled();
        long currentTime = System.currentTimeMillis();
        
        for (Announcement announcement : scheduledAnnouncements) {
            // Check if the announcement should be sent based on its interval
            long lastSent = announcement.getLastSent();
            int interval = announcement.getInterval();
            
            // If the announcement has never been sent (lastSent is 0), it's ready to send
            boolean readyToSend = (lastSent == 0) || (interval > 0 && currentTime - lastSent >= interval * 1000L);
            
            if (readyToSend) {
                // Check if the server is online
                String serverId = announcement.getServerId();
                boolean isServerOnline = plugin.getServerManager().isServerOnline(serverId);
                
                if (isServerOnline) {
                    // If batching is enabled and we're using a thread pool, send via scheduler
                    if (batchAnnouncements && isThreadPoolEnabled()) {
                        plugin.getServer().getScheduler().buildTask(plugin, () -> 
                            plugin.getAnnouncementManager().sendAnnouncement(announcement)
                        ).schedule();
                    } else {
                        // Send immediately
                        plugin.getAnnouncementManager().sendAnnouncement(announcement);
                    }
                }
            }
        }
    }
    
    private boolean isAnnouncementsEnabled() {
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Object announcementsObj = config.get("announcements");
        
        if (announcementsObj instanceof Map) {
            Map<String, Object> announcementsConfig = (Map<String, Object>) announcementsObj;
            Object enabledObj = announcementsConfig.get("enabled");
            return enabledObj instanceof Boolean && (Boolean) enabledObj;
        }
        
        return true; // Default to enabled
    }
    
    private boolean isThreadPoolEnabled() {
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Object performanceObj = config.get("performance");
        
        if (performanceObj instanceof Map) {
            Map<String, Object> performanceConfig = (Map<String, Object>) performanceObj;
            Object threadPoolObj = performanceConfig.get("use-thread-pool");
            return threadPoolObj instanceof Boolean && (Boolean) threadPoolObj;
        }
        
        return false;
    }
    
    private boolean isBatchAnnouncementsEnabled() {
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Object performanceObj = config.get("performance");
        
        if (performanceObj instanceof Map) {
            Map<String, Object> performanceConfig = (Map<String, Object>) performanceObj;
            Object batchObj = performanceConfig.get("batch-announcements");
            return batchObj instanceof Boolean && (Boolean) batchObj;
        }
        
        return false;
    }
    
    private int getAnnouncementCheckFrequency() {
        // Default to 1 second for backward compatibility
        return 1;
    }
    
    public boolean isRunning() {
        return running.get();
    }
} 