package com.midenium.mannouncer.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A command wrapper that implements rate limiting for command execution
 */
public class RateLimitedCommand implements SimpleCommand {

    private final SimpleCommand delegate;
    private final int maxCommandsPerMinute;
    private final Map<UUID, CommandUsage> usageMap = new ConcurrentHashMap<>();
    
    private static class CommandUsage {
        private int count;
        private long resetTime;
        
        public CommandUsage() {
            this.count = 1;
            this.resetTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        }
        
        public boolean increment() {
            long now = System.currentTimeMillis();
            
            // Reset counter if time has passed
            if (now > resetTime) {
                count = 1;
                resetTime = now + TimeUnit.MINUTES.toMillis(1);
                return true;
            }
            
            // Increment counter
            count++;
            return true;
        }
        
        public int getCount() {
            return count;
        }
    }
    
    public RateLimitedCommand(SimpleCommand delegate, int maxCommandsPerMinute) {
        this.delegate = delegate;
        this.maxCommandsPerMinute = maxCommandsPerMinute;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        
        // Skip rate limiting for console
        if (!(source instanceof com.velocitypowered.api.proxy.Player player)) {
            delegate.execute(invocation);
            return;
        }
        
        UUID playerId = player.getUniqueId();
        
        // Check if player has used too many commands
        CommandUsage usage = usageMap.computeIfAbsent(playerId, k -> new CommandUsage());
        if (usage.getCount() > maxCommandsPerMinute) {
            // Rate limit exceeded
            player.sendMessage(Component.text("Command rate limit exceeded. Please wait a moment before trying again.", NamedTextColor.RED));
            return;
        }
        
        // Track command usage
        usage.increment();
        
        // Execute the actual command
        delegate.execute(invocation);
    }
    
    @Override
    public List<String> suggest(Invocation invocation) {
        return delegate.suggest(invocation);
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return delegate.hasPermission(invocation);
    }
} 