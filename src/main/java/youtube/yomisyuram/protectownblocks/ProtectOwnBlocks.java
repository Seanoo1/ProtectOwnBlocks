package youtube.yomisyuram.protectownblocks;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.TNT;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class ProtectOwnBlocks extends JavaPlugin implements Listener {
    private BlockDataStorage blockDataStorage;
    private BucketInteraction bucketInteraction;
    private ProtectExploit protectExploit;
    private double bucketRange;
    private List<String> restrictedBuckets;
    private FileConfiguration config; // config 변수 추가
    private Set<UUID> protectedPlayers; // protectedPlayers 변수 추가
    private FileConfiguration disabledPlayerList; // disabledPlayerList 변수 추가

    private Plugin plugin; // plugin 변수 추가


    @Override
    public void onEnable() {
        plugin = this; // 플러그인 인스턴스 할당
        // Plugin startup logic
        blockDataStorage = new BlockDataStorage(this, "worlds_list");
        protectedPlayers = new HashSet<>(); // protectedPlayers 초기화
        loadConfig();
        loadDisabledPlayerList(); // disabledPlayerList 로드 추가

        // BucketInteraction 인스턴스 생성 및 등록
        bucketInteraction = new BucketInteraction(this, bucketRange, restrictedBuckets, blockDataStorage);
        getServer().getPluginManager().registerEvents(bucketInteraction, this);

        // ProtectExploit 인스턴스 생성 및 BlockDataStorage 인스턴스 전달
        protectExploit = new ProtectExploit(blockDataStorage);

        // ProtectExploit 이벤트 리스너 등록
        getServer().getPluginManager().registerEvents(protectExploit, this);

        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("ProtectOwnBlocks 플러그인이 활성화되었습니다.");
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig(); // config 변수에 getConfig() 결과 할당
        bucketRange = getConfig().getDouble("bucket_range", 10.0);
        restrictedBuckets = getConfig().getStringList("restricted_buckets");
    }

    private void loadDisabledPlayerList() {
        disabledPlayerList = new YamlConfiguration();
        File disabledPlayerListFile = new File(getDataFolder(), "Protect_mode_disabled_player_list.yml");

        if (!disabledPlayerListFile.exists()) {
            try {
                disabledPlayerListFile.createNewFile();
                disabledPlayerList.set("disabled_players", new ArrayList<String>());
                disabledPlayerList.save(disabledPlayerListFile);
            } catch (IOException e) {
                getLogger().severe("Protect_mode_disabled_player_list.yml 파일을 생성하는 중에 오류가 발생했습니다.");
                e.printStackTrace();
            }
        }

        try {
            disabledPlayerList.load(disabledPlayerListFile);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("Protect_mode_disabled_player_list.yml 파일을 로드하는 중에 오류가 발생했습니다.");
            e.printStackTrace();
        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("ProtectOwnBlocks 플러그인이 비활성화되었습니다.");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Location blockLocation = block.getLocation();

        if (!player.hasPermission("protectownblocks.bypass")) {
            if (blockDataStorage.isRestrictedBlock(blockLocation)) {
                if (!blockDataStorage.isBlockOwner(blockLocation, player.getUniqueId())) {
                    // 보호된 블록을 파괴할 권한이 없는 경우
                    if (isExplosion(block)) {
                        // 폭발에 의한 파괴
                        event.setCancelled(true);
                        player.sendMessage(blockDataStorage.getMessage("no_access_message"));
                    } else {
                        // 일반적인 방법에 의한 파괴
                        event.setCancelled(true);
                        blockDataStorage.removeRestrictedBlock(blockLocation, player.getUniqueId());

                        //config.yml 에 적힌 no_access_message 값을 출력해주기 위한 코드 -
                        UUID blockOwner = blockDataStorage.getBlockOwner(block.getLocation());
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(blockOwner);
                        String blockOwnerName = offlinePlayer.getName();
                        String blockOwnerUUID = offlinePlayer.getUniqueId().toString();

                        String message = blockDataStorage.getMessage("no_access_message");
                        message = message.replace("[blockOwnerName]", blockOwnerName);
                        message = message.replace("[blockOwnerUUID]", blockOwnerUUID);
                        player.sendMessage(message);
                        // -
                    }
                } else {
                    // 보호된 블록의 소유자인 경우
                    blockDataStorage.removeRestrictedBlock(blockLocation, player.getUniqueId());
                }
            }
        } else {
            // bypass 권한을 가지고 있는 경우
            blockDataStorage.removeRestrictedBlock(blockLocation, player.getUniqueId());

            // 블록 파괴 시 해당 좌표에 저장된 데이터 삭제
            if (blockDataStorage.isRestrictedBlock(blockLocation)) {
                Set<UUID> owners = blockDataStorage.getBlockOwners(blockLocation);
                for (UUID owner : owners) {
                    blockDataStorage.removeRestrictedBlock(blockLocation, owner);
                }
                player.sendMessage("이미 해당 좌표를 보호하고 있는 플레이어의 데이터를 삭제하였습니다.");
            }
        }
    }



    private void disableBlockProtection(Player player) {
        UUID playerUUID = player.getUniqueId();
        protectedPlayers.add(playerUUID);
        player.sendMessage("The block that you place from now on will §4§lNOT §rbe protected.\n(If you want to enable again, type : '/pob on'");

        // 플레이어 기록 추가
        List<String> disabledPlayers = disabledPlayerList.getStringList("disabled_players");
        if (!disabledPlayers.contains(playerUUID.toString())) {
            disabledPlayers.add(playerUUID.toString());
            disabledPlayerList.set("disabled_players", disabledPlayers);
            saveDisabledPlayerList();
        }
    }

    private void enableBlockProtection(Player player) {
        UUID playerUUID = player.getUniqueId();
        protectedPlayers.remove(playerUUID);
        player.sendMessage(ChatColor.YELLOW + "§aThe block that you place from now on will be protected.");

        // 플레이어 기록 삭제
        List<String> disabledPlayers = disabledPlayerList.getStringList("disabled_players");
        if (disabledPlayers.contains(playerUUID.toString())) {
            disabledPlayers.remove(playerUUID.toString());
            disabledPlayerList.set("disabled_players", disabledPlayers);
            saveDisabledPlayerList();
        }
    }



    private void saveDisabledPlayerList() {
        File disabledPlayerListFile = new File(getDataFolder(), "Protect_mode_disabled_player_list.yml");
        try {
            disabledPlayerList.save(disabledPlayerListFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Location location = block.getLocation();

        // config.yml에서 블록 보호를 하지 않을 월드인지 확인
        List<String> disabledWorlds = config.getStringList("disabled_protection_world");
        String worldName = location.getWorld().getName();
        if (disabledWorlds.contains(worldName)) {
            return;
        }

        // 블록 설치 시 해당 좌표에 저장된 데이터 삭제
        if (blockDataStorage.isRestrictedBlock(location)) {
            Set<UUID> owners = blockDataStorage.getBlockOwners(location);
            for (UUID owner : owners) {
                blockDataStorage.removeRestrictedBlock(location, owner);
            }
            player.sendMessage("이미 해당 좌표를 보호하고 있는 플레이어의 데이터를 삭제하였습니다.");
        }

        // 블록 설치 시 새로운 데이터 저장
        if (!protectedPlayers.contains(player.getUniqueId())) {
            blockDataStorage.addRestrictedBlock(location, player.getUniqueId());
        }
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock != null && !player.hasPermission("protectownblocks.bypass")) {
            Location blockLocation = clickedBlock.getLocation();

            // 보호된 블록인지 확인
            if (blockDataStorage.isRestrictedBlock(blockLocation)) {
                if (!blockDataStorage.isBlockOwner(blockLocation, player.getUniqueId())) {
                    boolean restrictInteractions = config.getBoolean("restrict_interactions", true); // plugin.getConfig() 대신 config 변수 사용
                    if (restrictInteractions) {
                        // 보호된 블록에 대한 우클릭 및 좌클릭 제한
                        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
                            UUID blockOwner = blockDataStorage.getBlockOwner(blockLocation);
                            if (blockOwner != null) {
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(blockOwner);
                                String blockOwnerName = offlinePlayer.getName();
                                String blockOwnerUUID = offlinePlayer.getUniqueId().toString();

                                // config.yml에서 메시지 읽어오기
                                String message = blockDataStorage.getMessage("no_access_message");
                                message = message.replace("[blockOwnerName]", blockOwnerName);
                                message = message.replace("[blockOwnerUUID]", blockOwnerUUID);

                                player.sendMessage(message);
                            } else {
                                player.sendMessage(blockDataStorage.getMessage("no_access_generic_message"));
                            }
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
        if (config.getBoolean("prevent-liquid-break-protected-block")) {
            Block block = event.getToBlock();
            Location location = block.getLocation();

            if (blockDataStorage.isRestrictedBlock(location)) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        List<Block> blocks = event.blockList();
        List<Block> blocksToRemove = new ArrayList<>();

        for (Block block : blocks) {
            if (blockDataStorage.isRestrictedBlock(block.getLocation())) {
                blocksToRemove.add(block);
            }
        }

        blocks.removeAll(blocksToRemove);
    }
    private boolean isExplosion(Block block) {
        Location location = block.getLocation();
        World world = location.getWorld();

        // 블록이 폭발에 의해 파괴되는지 확인
        List<Block> nearbyBlocks = new ArrayList<>();
        nearbyBlocks.add(block);
        for (BlockFace blockFace : BlockFace.values()) {
            Block relative = block.getRelative(blockFace);
            if (!nearbyBlocks.contains(relative)) {
                nearbyBlocks.add(relative);
            }
        }

        for (Block nearbyBlock : nearbyBlocks) {
            BlockState blockState = world.getBlockAt(nearbyBlock.getLocation()).getState();
            if (blockState instanceof TNT || blockState instanceof Creeper || blockState instanceof Fireball || blockState instanceof WitherSkull
                    || blockState instanceof EnderCrystal) {
                return true;
            }
        }

        return false;
    }



    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (!config.getBoolean("piston-protect")) {
            return;
        }

        List<Block> blocks = event.getBlocks();
        for (Block block : blocks) {
            if (blockDataStorage.isRestrictedBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if (!config.getBoolean("piston-protect")) {
            return;
        }

        List<Block> blocks = event.getBlocks();
        for (Block block : blocks) {
            if (blockDataStorage.isRestrictedBlock(block.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // PlayerCommandPreprocessEvent 이벤트 핸들러
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String[] args = event.getMessage().split(" ");

        if (args.length > 0 && args[0].equalsIgnoreCase("/pob")) {
            event.setCancelled(true); // 명령어 취소

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("off")) {
                    disableBlockProtection(player);
                } else if (args[1].equalsIgnoreCase("on")) {
                    enableBlockProtection(player);
                } else {
                    player.sendMessage(ChatColor.RED + "잘못된 명령어입니다. 사용법: /pob [on|off]");
                }
            } else {
                player.sendMessage(ChatColor.RED + "잘못된 명령어입니다. 사용법: /pob [on|off]");
            }
        }
    }


}
