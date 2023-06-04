package youtube.yomisyuram.protectownblocks;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.Location;


import java.util.List;

public class BucketInteraction implements Listener {
    private Plugin plugin;
    private double bucketRange;
    private List<String> restrictedBuckets;
    private BlockDataStorage blockDataStorage; // 데이터 저장소 인스턴스

    public BucketInteraction(Plugin plugin, double bucketRange, List<String> restrictedBuckets, BlockDataStorage blockDataStorage) {
        this.plugin = plugin;
        this.bucketRange = bucketRange;
        this.restrictedBuckets = restrictedBuckets;
        this.blockDataStorage = blockDataStorage;
    }


    @EventHandler
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Material bucketMaterial = event.getBucket();
        if (isRestrictedBucket(bucketMaterial) && !canInteractWithBucket(player, event.getBlockClicked().getLocation())) {
            event.setCancelled(true);

            String message = plugin.getConfig().getString("bucketOutOfRange");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        Location location = block.getLocation();

        if (!blockDataStorage.isRestrictedBlock(location) || blockDataStorage.getOwner(location).equals(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage("해당 위치에는 보호된 블록이 있어 물이나 용암을 설치할 수 없습니다.");
    }

    @EventHandler
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Material bucketMaterial = event.getItemStack().getType();
        if (isRestrictedBucket(bucketMaterial) && !canInteractWithBucket(player, event.getBlockClicked().getLocation())) {
            event.setCancelled(true);

            String message = plugin.getConfig().getString("bucketOutOfRange");
            if (message != null && !message.isEmpty()) {
                player.sendMessage(message);
            }
        }
    }


    private boolean isRestrictedBucket(Material material) {
        return restrictedBuckets.contains(material.toString());
    }

    private boolean canInteractWithBucket(Player player, Location location) {
        return player.getLocation().distance(location) <= bucketRange;
    }
}