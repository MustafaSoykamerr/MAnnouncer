package com.midenium.mannouncer.commands;

import com.midenium.mannouncer.MAnnouncer;
import com.midenium.mannouncer.models.Announcement;
import com.midenium.mannouncer.models.AnnouncementType;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MAnnouncerCommand implements SimpleCommand {

    private final MAnnouncer plugin;
    
    public MAnnouncerCommand(MAnnouncer plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (!plugin.getPermissionManager().hasPermission(source, "admin")) {
            source.sendMessage(getMessageComponent("general.no-permission"));
            return;
        }
        
        if (args.length == 0) {
            sendHelp(source);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!plugin.getPermissionManager().hasPermission(source, "reload")) {
                    source.sendMessage(getMessageComponent("general.no-permission"));
                    return;
                }
                handleReload(source);
            }
            case "announcement" -> {
                if (!plugin.getPermissionManager().hasPermission(source, "announcement")) {
                    source.sendMessage(getMessageComponent("general.no-permission"));
                    return;
                }
                handleAnnouncement(source, args);
            }
            case "test" -> {
                if (!plugin.getPermissionManager().hasPermission(source, "test")) {
                    source.sendMessage(getMessageComponent("general.no-permission"));
                    return;
                }
                handleTest(source, args);
            }
            default -> sendHelp(source);
        }
    }
    
    private void handleReload(CommandSource source) {
        // Reload plugin configuration
        plugin.getConfigManager().reloadConfigs();
        plugin.getAnnouncementManager().reload();
        plugin.getStreamerManager().reload();
        plugin.getPermissionManager().reload();
        
        source.sendMessage(getMessageComponent("general.plugin-reloaded"));
    }
    
    private void handleAnnouncement(CommandSource source, String[] args) {
        if (args.length < 4) {
            source.sendMessage(getMessageComponent("general.invalid-command"));
            return;
        }
        
        // Format: /mannouncer announcement <type> <server> <id> on|off
        String typeArg = args[1];
        String serverArg = args[2];
        String idArg = args[3];
        
        // Check if server exists
        if (!serverExists(serverArg)) {
            source.sendMessage(
                    getMessageComponent("general.server-not-found")
                    .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
            );
            return;
        }
        
        // Parse announcement type
        AnnouncementType type = AnnouncementType.fromString(typeArg);
        if (type == null) {
            source.sendMessage(
                    getMessageComponent("announcements.invalid-type")
                    .replaceText(builder -> builder.matchLiteral("{type}").replacement(typeArg))
            );
            return;
        }
        
        // Find announcement
        Optional<Announcement> announcement = plugin.getAnnouncementManager().getAnnouncement(serverArg, type, idArg);
        if (announcement.isEmpty()) {
            source.sendMessage(
                    getMessageComponent("announcements.not-found")
                    .replaceText(builder -> builder.matchLiteral("{id}").replacement(idArg))
                    .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
            );
            return;
        }
        
        // Check for on/off argument
        if (args.length < 5) {
            source.sendMessage(getMessageComponent("general.invalid-command"));
            return;
        }
        
        boolean enable = args[4].equalsIgnoreCase("on");
        plugin.getAnnouncementManager().setAnnouncementEnabled(serverArg, type, idArg, enable);
        
        source.sendMessage(
                getMessageComponent(enable ? "announcements.enabled" : "announcements.disabled")
                .replaceText(builder -> builder.matchLiteral("{id}").replacement(idArg))
                .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
        );
    }
    
    private void handleTest(CommandSource source, String[] args) {
        if (args.length < 4) {
            source.sendMessage(getMessageComponent("general.invalid-command"));
            return;
        }
        
        // Format: /mannouncer test <type> <server> <id>
        String typeArg = args[1];
        String serverArg = args[2];
        String idArg = args[3];
        
        // Check if server exists
        if (!serverExists(serverArg)) {
            source.sendMessage(
                    getMessageComponent("general.server-not-found")
                    .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
            );
            return;
        }
        
        // Parse announcement type
        AnnouncementType type = AnnouncementType.fromString(typeArg);
        if (type == null) {
            source.sendMessage(
                    getMessageComponent("announcements.invalid-type")
                    .replaceText(builder -> builder.matchLiteral("{type}").replacement(typeArg))
            );
            return;
        }
        
        // Find announcement
        Optional<Announcement> announcement = plugin.getAnnouncementManager().getAnnouncement(serverArg, type, idArg);
        if (announcement.isEmpty()) {
            source.sendMessage(
                    getMessageComponent("announcements.not-found")
                    .replaceText(builder -> builder.matchLiteral("{id}").replacement(idArg))
                    .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
            );
            return;
        }
        
        // Test announcement
        plugin.getAnnouncementManager().sendAnnouncement(announcement.get());
        
        source.sendMessage(
                getMessageComponent("announcements.test-sent")
                .replaceText(builder -> builder.matchLiteral("{id}").replacement(idArg))
                .replaceText(builder -> builder.matchLiteral("{server}").replacement(serverArg))
        );
    }
    
    private void sendHelp(CommandSource source) {
        String prefix = getPrefix();
        
        List<Component> messages = new ArrayList<>();
        messages.add(MiniMessage.miniMessage().deserialize(prefix + "<yellow>mAnnouncer Commands:</yellow>"));
        
        // Only show commands the player has permission for
        if (plugin.getPermissionManager().hasPermission(source, "reload")) {
            messages.add(MiniMessage.miniMessage().deserialize("<gray>/mannouncer reload</gray> - <white>Reload the plugin configuration</white>"));
        }
        
        if (plugin.getPermissionManager().hasPermission(source, "announcement")) {
            messages.add(MiniMessage.miniMessage().deserialize("<gray>/mannouncer announcement <type> <server> <id> on|off</gray> - <white>Enable or disable an announcement</white>"));
        }
        
        if (plugin.getPermissionManager().hasPermission(source, "test")) {
            messages.add(MiniMessage.miniMessage().deserialize("<gray>/mannouncer test <type> <server> <id></gray> - <white>Test an announcement</white>"));
        }
        
        for (Component message : messages) {
            source.sendMessage(message);
        }
    }
    
    private Component getMessageComponent(String path) {
        Map<String, Object> messagesConfig = plugin.getConfigManager().getMessagesConfig();
        
        // Parse path (e.g., "general.no-permission" -> ["general", "no-permission"])
        String[] parts = path.split("\\.");
        
        Object current = messagesConfig;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                current = null;
                break;
            }
        }
        
        // Get message text
        String messageText = current != null ? current.toString() : "<red>Message not found: " + path + "</red>";
        
        // Add prefix
        String prefix = getPrefix();
        
        return MiniMessage.miniMessage().deserialize(prefix + messageText);
    }
    
    private String getPrefix() {
        Map<String, Object> messagesConfig = plugin.getConfigManager().getMessagesConfig();
        Object prefixObj = messagesConfig.get("prefix");
        return prefixObj != null ? prefixObj.toString() : "";
    }
    
    private boolean serverExists(String serverId) {
        return plugin.getServer().getServer(serverId).isPresent();
    }
    
    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (!plugin.getPermissionManager().hasPermission(source, "admin")) {
            return Collections.emptyList();
        }
        
        // Empty arguments - show available commands
        if (args.length == 0) {
            List<String> commands = new ArrayList<>();
            
            if (plugin.getPermissionManager().hasPermission(source, "reload")) {
                commands.add("reload");
            }
            if (plugin.getPermissionManager().hasPermission(source, "announcement")) {
                commands.add("announcement");
            }
            if (plugin.getPermissionManager().hasPermission(source, "test")) {
                commands.add("test");
            }
            
            return commands;
        }
        
        // First argument - filter available commands
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            
            if (plugin.getPermissionManager().hasPermission(source, "reload") && 
                    "reload".startsWith(args[0].toLowerCase())) {
                commands.add("reload");
            }
            if (plugin.getPermissionManager().hasPermission(source, "announcement") && 
                    "announcement".startsWith(args[0].toLowerCase())) {
                commands.add("announcement");
            }
            if (plugin.getPermissionManager().hasPermission(source, "test") && 
                    "test".startsWith(args[0].toLowerCase())) {
                commands.add("test");
            }
            
            return commands;
        }
        
        // Second argument - show announcement types
        if (args.length == 2 && (args[0].equalsIgnoreCase("announcement") || args[0].equalsIgnoreCase("test"))) {
            if ((args[0].equalsIgnoreCase("announcement") && !plugin.getPermissionManager().hasPermission(source, "announcement")) ||
                    (args[0].equalsIgnoreCase("test") && !plugin.getPermissionManager().hasPermission(source, "test"))) {
                return Collections.emptyList();
            }
            
            return Arrays.stream(AnnouncementType.values())
                    .map(type -> type.name().toLowerCase())
                    .filter(type -> type.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Third argument - show server names
        if (args.length == 3 && (args[0].equalsIgnoreCase("announcement") || args[0].equalsIgnoreCase("test"))) {
            if ((args[0].equalsIgnoreCase("announcement") && !plugin.getPermissionManager().hasPermission(source, "announcement")) ||
                    (args[0].equalsIgnoreCase("test") && !plugin.getPermissionManager().hasPermission(source, "test"))) {
                return Collections.emptyList();
            }
            
            return plugin.getServer().getAllServers().stream()
                    .map(server -> server.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Fourth argument - show announcement IDs for the selected server and type
        if (args.length == 4 && (args[0].equalsIgnoreCase("announcement") || args[0].equalsIgnoreCase("test"))) {
            if ((args[0].equalsIgnoreCase("announcement") && !plugin.getPermissionManager().hasPermission(source, "announcement")) ||
                    (args[0].equalsIgnoreCase("test") && !plugin.getPermissionManager().hasPermission(source, "test"))) {
                return Collections.emptyList();
            }
            
            String typeArg = args[1];
            String serverArg = args[2];
            
            AnnouncementType type = AnnouncementType.fromString(typeArg);
            if (type != null && serverExists(serverArg)) {
                // Get all announcement IDs for this server and type
                Map<AnnouncementType, Map<String, Announcement>> serverAnnouncements = 
                        plugin.getAnnouncementManager().getAnnouncementsForServer(serverArg);
                
                if (serverAnnouncements != null && serverAnnouncements.containsKey(type)) {
                    return serverAnnouncements.get(type).keySet().stream()
                            .filter(id -> id.toLowerCase().startsWith(args[3].toLowerCase()))
                            .collect(Collectors.toList());
                }
            }
        }
        
        // Fifth argument - show on/off options for announcement commands
        if (args.length == 5 && args[0].equalsIgnoreCase("announcement")) {
            if (!plugin.getPermissionManager().hasPermission(source, "announcement")) {
                return Collections.emptyList();
            }
            
            return Arrays.asList("on", "off").stream()
                    .filter(option -> option.toLowerCase().startsWith(args[4].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return Collections.emptyList();
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return plugin.getPermissionManager().hasPermission(invocation.source(), "admin");
    }
} 