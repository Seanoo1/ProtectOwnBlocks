package youtube.yomisyuram.protectownblocks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.BlockProjectileSource;

public class BlockDataStorage {
    private final Plugin plugin;
    private final Map<String, FileConfiguration> worldConfigs;

    public BlockDataStorage(JavaPlugin plugin, String folderName) {
        this.plugin = plugin;
        this.worldConfigs = new HashMap<>();

        // 데이터 폴더 경로 설정
        String dataFolderPath = plugin.getDataFolder().getPath();
        String worldFolderPath = dataFolderPath + File.separator + folderName;

        // 폴더 생성
        File worldFolder = new File(worldFolderPath);
        if (!worldFolder.exists()) {
            worldFolder.mkdirs();
        }
    }


    private FileConfiguration getWorldConfig(String worldName) {
        if (!worldConfigs.containsKey(worldName)) {
            File file = new File(plugin.getDataFolder() + "/worlds_list", worldName + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            worldConfigs.put(worldName, config);
        }
        return worldConfigs.get(worldName);
    }

    public void addRestrictedBlock(Location location, UUID playerUUID) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        String playerUUIDString = playerUUID.toString();
        List<String> blockLocations = config.getStringList(playerUUIDString);

        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        blockLocations.add(blockKey);

        config.set(playerUUIDString, blockLocations);

        saveConfig(config, worldName);
    }

    public void removeRestrictedBlock(Location location, UUID playerUUID) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        String playerUUIDString = playerUUID.toString();
        List<String> blockLocations = config.getStringList(playerUUIDString);

        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        blockLocations.remove(blockKey);

        config.set(playerUUIDString, blockLocations);

        saveConfig(config, worldName);
    }

    public boolean isRestrictedBlock(Location location) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        for (String playerUUIDString : config.getKeys(false)) {
            List<String> blockLocations = config.getStringList(playerUUIDString);

            String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            if (blockLocations.contains(blockKey)) {
                return true;
            }
        }

        return false;
    }

    public UUID getBlockOwner(Location location) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        for (String playerUUIDString : config.getKeys(false)) {
            UUID playerUUID = UUID.fromString(playerUUIDString);
            List<String> blockLocations = config.getStringList(playerUUIDString);

            String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            if (blockLocations.contains(blockKey)) {
                return playerUUID;
            }
        }

        return null;
    }

    public Set<UUID> getBlockOwners(Location location) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);
        Set<UUID> owners = new HashSet<>();

        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        for (String playerUUIDString : config.getKeys(false)) {
            List<String> blockLocations = config.getStringList(playerUUIDString);

            if (blockLocations.contains(blockKey)) {
                UUID ownerUUID = UUID.fromString(playerUUIDString);
                owners.add(ownerUUID);
            }
        }

        return owners;
    }


    public UUID getOwner(Location location) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        for (String playerUUIDString : config.getKeys(false)) {
            UUID playerUUID = UUID.fromString(playerUUIDString);
            List<String> blockLocations = config.getStringList(playerUUIDString);

            String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
            if (blockLocations.contains(blockKey)) {
                return playerUUID;
            }
        }

        return null;
    }


    public boolean getBlockRestrictInteraction(Location location) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        return config.getBoolean("block_restrict_interaction." + blockKey, true);
    }



    public String getMessage(String key) {
        // config.yml에서 메시지를 읽어옴
        return plugin.getConfig().getString(key);
    }

    public boolean isBlockOwner(Location location, UUID playerUUID) {
        String worldName = location.getWorld().getName();
        FileConfiguration config = getWorldConfig(worldName);

        String playerUUIDString = playerUUID.toString();
        List<String> blockLocations = config.getStringList(playerUUIDString);

        String blockKey = location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        if (blockLocations.contains(blockKey)) {
            return true;
        }

        // 추가: TNT 폭발에 의한 블록 파괴 검사
        if (isTNTExplosion(location)) {
            for (String ownerUUIDString : config.getKeys(false)) {
                List<String> ownerBlockLocations = config.getStringList(ownerUUIDString);
                if (ownerBlockLocations.contains(blockKey)) {
                    return true;
                }
            }
        }


        return false;
    }

    private boolean isTNTExplosion(Location location) {
        Block block = location.getBlock();
        for (org.bukkit.entity.Entity entity : block.getWorld().getNearbyEntities(location, 1, 1, 1)) {
            if (entity.getType() == EntityType.PRIMED_TNT) {
                TNTPrimed tnt = (TNTPrimed) entity;
                if (tnt.getSource() instanceof BlockProjectileSource) {
                    BlockProjectileSource source = (BlockProjectileSource) tnt.getSource();
                    if (source.getBlock().getType() == Material.TNT) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void saveConfig(FileConfiguration config, String worldName) {
        File file = new File(plugin.getDataFolder() + "/worlds_list", worldName + ".yml");
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
