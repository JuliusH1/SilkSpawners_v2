package de.corneliusmay.silkspawners.plugin.spawner;

import de.corneliusmay.silkspawners.plugin.SilkSpawners;
import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValue;
import de.corneliusmay.silkspawners.plugin.config.handler.ConfigValueArray;
import de.corneliusmay.silkspawners.plugin.config.PluginConfig;
import de.corneliusmay.silkspawners.plugin.utils.ItemBuilder;
import de.corneliusmay.silkspawners.plugin.utils.StringUtils;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.regex.Pattern;

public class Spawner {
    public static String EMPTY = "empty";

    private static final String PDC_ENTITY_KEY = "spawner_entity_type";
    private static final ItemFlag TOOLTIP_HIDE_FLAG = resolveTooltipHideFlag();

    private final SilkSpawners plugin;

    @Getter
    private EntityType entityType;

    @Getter
    private ItemStack itemStack;

    private final String prefix = new ConfigValue<String>(PluginConfig.SPAWNER_ITEM_PREFIX).get();
    private final String oldPrefix = new ConfigValue<String>(PluginConfig.SPAWNER_ITEM_PREFIX_OLD).get();

    public Spawner(SilkSpawners plugin, Block block) {
        this.plugin = plugin;
        if(block == null) return;
        if(block.getType() != this.plugin.getBukkitHandler().getSpawnerMaterial()) return;

        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
        this.entityType = creatureSpawner.getSpawnedType();
        this.itemStack = generateItemStack();
    }

    public Spawner(SilkSpawners plugin, ItemStack itemStack) {
        this.plugin = plugin;
        this.itemStack = itemStack;
        if(itemStack == null) return;
        if(itemStack.getType() != this.plugin.getBukkitHandler().getSpawnerMaterial()) return;
        if(itemStack.getItemMeta() == null) return;

        PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, PDC_ENTITY_KEY);
        if (pdc.has(key, PersistentDataType.STRING)) {
            String stored = pdc.get(key, PersistentDataType.STRING);
            this.entityType = stored.equalsIgnoreCase(Spawner.EMPTY) ? null : EntityType.fromName(stored);
            return;
        }

        // Fallback
        if (itemStack.getItemMeta().getLore() == null) return;
        this.entityType = getSpawnerEntity(itemStack.getItemMeta().getLore().get(0));
    }

    public Spawner(SilkSpawners plugin, EntityType entityType) {
        this.plugin = plugin;
        this.entityType = entityType;
        this.itemStack = generateItemStack();
    }

    public void setSpawnerBlockType(Block block, List<Block> editedList) {
        if(!isValid()){
            editedList.remove(block);
            return;
        }
        this.plugin.getPlatform().runTaskLater(block.getLocation(), () -> {
            BlockState blockState = block.getState();
            if(!(blockState instanceof CreatureSpawner)) return;
            CreatureSpawner creatureSpawner = (CreatureSpawner) blockState;
            creatureSpawner.setSpawnedType(this.entityType);
            blockState.update();
            editedList.remove(block);
        }, 1);
    }

    private ItemStack generateItemStack() {
        String itemName = applyPlaceholders(new ConfigValue<String>(PluginConfig.SPAWNER_ITEM_NAME).get());
        List<String> customLore = new ConfigValueArray<String>(PluginConfig.SPAWNER_ITEM_LORE).get()
                .stream()
                .map(this::applyPlaceholders)
                .map(line -> line.startsWith("§r") ? line : "§r" + line)
                .toList();
        ItemBuilder builder = new ItemBuilder(this.plugin.getBukkitHandler().getSpawnerMaterial())
                .setDisplayName(itemName)
                .addItemFlags(TOOLTIP_HIDE_FLAG);
        if (!prefix.isEmpty()) {
            builder.addToLore("§r" + serializedName());
        }
        ItemStack item = builder.addToLore(customLore).build();

        // Write entity type to PDC so placement/detection never relies on lore
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, PDC_ENTITY_KEY),
                PersistentDataType.STRING,
                serializedEntityType()
        );
        item.setItemMeta(meta);
        return item;
    }

    private static ItemFlag resolveTooltipHideFlag() {
        try {
            return ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP");
        } catch (IllegalArgumentException ignored) {
            return ItemFlag.HIDE_ATTRIBUTES;
        }
    }

    private String applyPlaceholders(String text) {
        String rawName = serializedEntityType();
        String casedName = StringUtils.capitalizeFully(rawName.replace("_", " "));
        String result = text
                .replace("%spawner_name%", rawName)
                .replace("%spawner_name_case%", casedName);

        // Resolve PAPI placeholders
        // null player = server-side/static placeholders only
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            result = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, result);
        }

        return result;
    }

    private EntityType getSpawnerEntity(String lore) {
        String name;
        if(!prefix.isEmpty() && lore.startsWith(prefix)) {
            name = lore.replaceFirst(Pattern.quote(prefix), "").replace(" ", "_").toLowerCase();
        }else if(!oldPrefix.isEmpty() && lore.startsWith(oldPrefix)) {
            name = lore.replaceFirst(Pattern.quote(oldPrefix), "").replace(" ", "_").toLowerCase();
        }else {
            // Fallback for dynamic prefix formats (e.g. %spawner_name_case%) where static prefix matching is impossible.
            name = ChatColor.stripColor(lore).trim().replace(" ", "_").toLowerCase();
            if (name.endsWith("_spawner")) {
                name = name.substring(0, name.length() - "_spawner".length());
            }
            EntityType fallbackType = EntityType.fromName(name);
            if (fallbackType != null) {
                return fallbackType;
            }

            for (EntityType type : EntityType.values()) {
                String entityName = type.getName();
                if (entityName != null && name.contains(entityName.toLowerCase())) {
                    return type;
                }
            }
            return null;
        }
        if (name.equalsIgnoreCase(Spawner.EMPTY)) {
            return null;
        }
        return EntityType.fromName(name);
    }

    public String serializedEntityType() {
        return entityType == null ? Spawner.EMPTY : entityType.getName().toLowerCase();
    }

    public String serializedName() {
        if (prefix.contains("%spawner_name%") || prefix.contains("%spawner_name_case%")) {
            return applyPlaceholders(prefix);
        }
        return prefix + StringUtils.capitalizeFully(serializedEntityType().replace("_", " "));
    }

    public boolean isValid() {
        return itemStack != null && (entityType == null || entityType.isSpawnable());
    }
}