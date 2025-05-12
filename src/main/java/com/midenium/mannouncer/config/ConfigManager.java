package com.midenium.mannouncer.config;

import com.midenium.mannouncer.MAnnouncer;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final MAnnouncer plugin;
    private final Path dataDirectory;
    private final Map<String, Object> mainConfig = new HashMap<>();
    private final Map<String, Object> messagesConfig = new HashMap<>();
    private final Map<String, Object> streamersConfig = new HashMap<>();
    private final Map<String, Map<String, Object>> serverConfigs = new HashMap<>();

    public ConfigManager(MAnnouncer plugin) {
        this.plugin = plugin;
        this.dataDirectory = plugin.getDataDirectory();
    }

    public void loadConfigs() {
        createDirectories();
        loadMainConfig();
        loadMessagesConfig();
        loadStreamersConfig();
        loadServerConfigs();
    }

    private void createDirectories() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }

            Path serversDir = dataDirectory.resolve("servers");
            if (!Files.exists(serversDir)) {
                Files.createDirectories(serversDir);
            }

            // Create server subdirectories based on server list
            List<String> servers = plugin.getServer().getAllServers().stream()
                    .map(server -> server.getServerInfo().getName())
                    .toList();

            for (String server : servers) {
                Path serverDir = serversDir.resolve(server);
                if (!Files.exists(serverDir)) {
                    Files.createDirectories(serverDir);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().error("Failed to create directories", e);
        }
    }

    private void loadMainConfig() {
        Path configPath = dataDirectory.resolve("config.yml");
        if (!Files.exists(configPath)) {
            saveResource("config.yml", configPath);
        }
        mainConfig.putAll(loadYamlFile(configPath));
    }

    private void loadMessagesConfig() {
        Path messagesPath = dataDirectory.resolve("messages.yml");
        if (!Files.exists(messagesPath)) {
            saveResource("messages.yml", messagesPath);
        }
        messagesConfig.putAll(loadYamlFile(messagesPath));
    }

    private void loadStreamersConfig() {
        Path streamersPath = dataDirectory.resolve("streamers.yml");
        if (!Files.exists(streamersPath)) {
            saveResource("streamers.yml", streamersPath);
        }
        streamersConfig.putAll(loadYamlFile(streamersPath));
    }

    private void loadServerConfigs() {
        Path serversDir = dataDirectory.resolve("servers");
        
        // Get all server directories
        File[] serverDirs = serversDir.toFile().listFiles(File::isDirectory);
        if (serverDirs == null) return;
        
        for (File serverDir : serverDirs) {
            String serverName = serverDir.getName();
            Map<String, Object> serverConfig = new HashMap<>();
            
            // Load announcement config files for this server
            loadAnnouncementConfig(serverDir, "chat-announcements.yml", serverConfig);
            loadAnnouncementConfig(serverDir, "bossbar-announcements.yml", serverConfig);
            loadAnnouncementConfig(serverDir, "title-announcements.yml", serverConfig);
            loadAnnouncementConfig(serverDir, "subtitle-announcements.yml", serverConfig);
            loadAnnouncementConfig(serverDir, "advancement-announcements.yml", serverConfig);
            
            serverConfigs.put(serverName, serverConfig);
        }
    }
    
    private void loadAnnouncementConfig(File serverDir, String fileName, Map<String, Object> serverConfig) {
        File configFile = new File(serverDir, fileName);
        if (!configFile.exists()) {
            // Create default announcement config
            String resourcePath = "defaults/" + fileName;
            saveResource(resourcePath, configFile.toPath());
        }
        
        Map<String, Object> announcementConfig = loadYamlFile(configFile.toPath());
        String configKey = fileName.replace(".yml", "").replace("-", "_");
        serverConfig.put(configKey, announcementConfig);
    }

    private Map<String, Object> loadYamlFile(Path path) {
        try (InputStream inputStream = new FileInputStream(path.toFile());
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(reader);
            return config != null ? config : new HashMap<>();
            
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load YAML file: " + path, e);
            return new HashMap<>();
        }
    }

    private void saveResource(String resourcePath, Path targetPath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                plugin.getLogger().warn("Resource not found: " + resourcePath);
                // Create an empty file if resource doesn't exist
                Files.createFile(targetPath);
                return;
            }
            
            Files.copy(in, targetPath);
        } catch (IOException e) {
            plugin.getLogger().error("Failed to save resource: " + resourcePath, e);
        }
    }

    public void saveConfig(String configName, Map<String, Object> config) {
        Path configPath = dataDirectory.resolve(configName);
        saveYamlFile(configPath, config);
    }

    public void saveServerConfig(String serverName, String configName, Map<String, Object> config) {
        Path serverConfigPath = dataDirectory.resolve("servers").resolve(serverName).resolve(configName);
        saveYamlFile(serverConfigPath, config);
    }

    private void saveYamlFile(Path path, Map<String, Object> config) {
        try (Writer writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            
            Yaml yaml = new Yaml(options);
            yaml.dump(config, writer);
        } catch (IOException e) {
            plugin.getLogger().error("Failed to save YAML file: " + path, e);
        }
    }

    public void reloadConfigs() {
        mainConfig.clear();
        messagesConfig.clear();
        streamersConfig.clear();
        serverConfigs.clear();
        
        loadConfigs();
    }

    public Map<String, Object> getMainConfig() {
        return mainConfig;
    }

    public Map<String, Object> getMessagesConfig() {
        return messagesConfig;
    }

    public Map<String, Object> getStreamersConfig() {
        return streamersConfig;
    }

    public Map<String, Map<String, Object>> getServerConfigs() {
        return serverConfigs;
    }
    
    public Map<String, Object> getServerConfig(String serverName) {
        return serverConfigs.getOrDefault(serverName, new HashMap<>());
    }
} 