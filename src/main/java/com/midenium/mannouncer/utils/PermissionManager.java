package com.midenium.mannouncer.utils;

import com.midenium.mannouncer.MAnnouncer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.Map;
import java.util.HashMap;

public class PermissionManager {

    private final MAnnouncer plugin;
    private final Map<String, String> permissionNodes;
    
    public PermissionManager(MAnnouncer plugin) {
        this.plugin = plugin;
        this.permissionNodes = loadPermissionNodes();
    }
    
    private Map<String, String> loadPermissionNodes() {
        Map<String, String> nodes = new HashMap<>();
        
        // Get permission settings from config
        Map<String, Object> config = plugin.getConfigManager().getMainConfig();
        Map<String, Object> permConfig = (Map<String, Object>) config.getOrDefault("permissions", Map.of());
        
        // Get base permission
        String basePermission = getString(permConfig, "base", "mannouncer");
        nodes.put("base", basePermission);
        
        // Get command permissions
        Map<String, Object> cmdConfig = (Map<String, Object>) permConfig.getOrDefault("commands", Map.of());
        
        nodes.put("admin", getString(cmdConfig, "admin", basePermission + ".admin"));
        nodes.put("reload", getString(cmdConfig, "reload", basePermission + ".reload"));
        nodes.put("test", getString(cmdConfig, "test", basePermission + ".test"));
        nodes.put("announcement", getString(cmdConfig, "announcement", basePermission + ".announcement"));
        
        return nodes;
    }
    
    /**
     * Check if a player or console has a specific permission
     * 
     * @param source The command source (player or console)
     * @param permission The permission key (e.g., "admin", "reload")
     * @return true if the source has the permission
     */
    public boolean hasPermission(CommandSource source, String permission) {
        // Console always has permission
        if (!(source instanceof Player)) {
            return true;
        }
        
        // If permission doesn't exist in our nodes, use the raw permission
        String node = permissionNodes.getOrDefault(permission, permission);
        
        // Add debug logging
        boolean hasPerm = source.hasPermission(node);
        if (plugin.getConfigManager().getMainConfig().getOrDefault("debug", false) instanceof Boolean && 
                (Boolean) plugin.getConfigManager().getMainConfig().getOrDefault("debug", false)) {
            plugin.getLogger().info("Permission check: " + source.getClass().getSimpleName() + 
                    " checking for permission: " + node + " - Result: " + hasPerm);
        }
        
        return hasPerm;
    }
    
    /**
     * Get a permission node from the configured nodes
     * 
     * @param key The permission key
     * @return The permission node
     */
    public String getPermissionNode(String key) {
        return permissionNodes.getOrDefault(key, key);
    }
    
    /**
     * Helper method to safely get a string from a map
     */
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Reload permission settings from config
     */
    public void reload() {
        permissionNodes.clear();
        permissionNodes.putAll(loadPermissionNodes());
    }
} 