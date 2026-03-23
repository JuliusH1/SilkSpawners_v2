package de.corneliusmay.silkspawners.plugin.listeners;

import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValue;
import de.corneliusmay.silkspawners.plugin.config.PluginConfig;
import de.corneliusmay.silkspawners.plugin.listeners.handler.SilkSpawnersListener;
import de.corneliusmay.silkspawners.plugin.spawner.Spawner;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PlayerInteractListener extends SilkSpawnersListener<PlayerInteractEvent> {

    private final List<Block> editedSpawners;

    public PlayerInteractListener(List<Block> editedSpawners) {
        this.editedSpawners = editedSpawners;
    }

    @Override @EventHandler(priority = EventPriority.HIGHEST)
    protected void onCall(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();

        Location blockLocation = block.getLocation();
        Spawner spawner = new Spawner(plugin, block);
        if (!spawner.isValid()) return;

        ItemStack heldItem = e.getItem();
        boolean isSpawnEgg = heldItem != null && heldItem.getType().name().endsWith("_SPAWN_EGG");

        if (isSpawnEgg) {
            boolean eggEnabled = new ConfigValue<Boolean>(PluginConfig.SPAWNER_EGG_ENABLED).get();
            if (!eggEnabled) {
                e.setCancelled(true);
                return;
            }
        }

        if (editedSpawners.stream().anyMatch(b -> b.getLocation().equals(blockLocation))) {
            e.setCancelled(true);
            return;
        }
        editedSpawners.add(block);

        final ItemStack eggSnapshot = isSpawnEgg ? heldItem.clone() : null;
        final int eggSlot = isSpawnEgg ? e.getPlayer().getInventory().getHeldItemSlot() : -1;

        this.plugin.getPlatform().runTaskLater(blockLocation, () -> {
            Spawner newSpawner = new Spawner(plugin, block.getWorld().getBlockAt(blockLocation));

            boolean permissionDenied =
                    !e.getPlayer().hasPermission("silkspawners.change." + newSpawner.serializedEntityType())
                            && !e.getPlayer().hasPermission("silkspawners.change.*")
                            && !new ConfigValue<Boolean>(PluginConfig.SPAWNER_PERMISSION_DISABLE_CHANGE).get()
                            && spawner.getEntityType() != newSpawner.getEntityType();

            if (permissionDenied) {
                spawner.setSpawnerBlockType(block, this.editedSpawners);
                if (new ConfigValue<Boolean>(PluginConfig.SPAWNER_MESSAGE_DENY_CHANGE).get()) {
                    e.getPlayer().sendMessage(plugin.getLocale().getMessage("SPAWNER_CHANGE_DENIED"));
                }
                if (eggSnapshot != null) {
                    e.getPlayer().getInventory().setItem(eggSlot, eggSnapshot);
                }
            } else {
                if (eggSnapshot != null && !new ConfigValue<Boolean>(PluginConfig.SPAWNER_EGG_CONSUME).get()) {
                    e.getPlayer().getInventory().setItem(eggSlot, eggSnapshot);
                }
                editedSpawners.remove(block);
            }
        }, 1);
    }
}