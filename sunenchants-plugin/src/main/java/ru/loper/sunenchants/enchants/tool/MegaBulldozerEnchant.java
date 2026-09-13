package ru.loper.sunenchants.enchants.tool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sunenchants.SunEnchants;
import ru.loper.sunenchants.api.enchants.EnchantRegister;
import ru.loper.sunenchants.api.enchants.SEnchant;
import ru.loper.sunenchants.api.enchants.formatter.EnchantLevelFormatter;
import ru.loper.sunenchants.api.enchants.formatter.EnchantTextFormatter;
import ru.loper.sunenchants.api.enchants.levels.EnchantLevelType;
import ru.loper.sunenchants.api.enchants.levels.impl.IntLevel;
import ru.loper.sunenchants.manager.EnchantsManager;
import ru.loper.sunenchants.utils.BulldozerUtils;
import ru.loper.sunenchants.utils.HeldItemCache;
import ru.loper.sunenchants.utils.MessageUtils;

@EnchantRegister(name = "mega_bulldozer", level = EnchantLevelType.INTEGER)
public class MegaBulldozerEnchant extends SEnchant {
    private final Set<String> bulldozerToggleMap = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Set<Block> processedBlocks = ConcurrentHashMap.newKeySet();

    private BulldozerUtils bulldozerUtils;
    private EnchantsManager enchantsManager;

    private String onMessage;
    private String offMessage;
    private List<String> blockedWorlds;
    private int maxBlocksPerTick;

    public MegaBulldozerEnchant(
            @NotNull NamespacedKey namespacedKey,
            @NotNull EnchantTextFormatter textFormatter,
            @NotNull EnchantLevelFormatter levelFormatter) {
        super(namespacedKey, textFormatter, levelFormatter);
    }

    @Override
    protected void parseValues(@NotNull ConfigurationSection section) {
        this.enchantsManager = SunEnchants.getInstance().getEnchantsManager();
        this.bulldozerUtils = new BulldozerUtils(enchantsManager);

        onMessage = section.getString("on_message", "<green>Мега-бульдозер включен");
        offMessage = section.getString("off_message", "<red>Мега-бульдозер выключен");
        blockedWorlds = section.getStringList("blocked_worlds");
        maxBlocksPerTick = Math.max(1, section.getInt("max_blocks_per_tick", 64));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!event.getPlayer().isSneaking()) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !isApplied(item)) {
            return;
        }

        toggleBulldozerMode(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (BulldozerUtils.isInternalBreak(event.getBlock())) {
            return;
        }

        Player player = event.getPlayer();
        Block centerBlock = event.getBlock();
        ItemStack tool = HeldItemCache.mainHand(player);

        if (!isApplied(tool) || enchantsManager.hasEnchant("bulldozer_log", tool)) {
            return;
        }
        if (hasBulldozer(player) || blockedWorlds.contains(player.getWorld().getName())) {
            return;
        }

        IntLevel level = getLevel(tool);
        if (level == null || !level.hasWorkChance()) {
            return;
        }

        if (processedBlocks.contains(centerBlock)) {
            processedBlocks.remove(centerBlock);
            return;
        }

        level.playSounds(player);
        int radius = level.getValue();

        BlockFace face = player.getFacing();
        boolean useXZ = face == BlockFace.UP
                || face == BlockFace.DOWN
                || Math.abs(player.getLocation().getPitch()) > 45.0F;
        boolean useXY = face == BlockFace.NORTH || face == BlockFace.SOUTH;
        boolean useYZ = face == BlockFace.EAST || face == BlockFace.WEST;

        Collection<ItemStack> collectedItems = new ArrayList<>();

        int diameter = radius * 2 + 1;
        if (radius == 1) {
            bulldozerUtils.breakBlocksInPlane(
                    centerBlock,
                    tool,
                    player,
                    useXZ,
                    useXY,
                    useYZ,
                    collectedItems,
                    processedBlocks,
                    null,
                    1,
                    9,
                    1,
                    maxBlocksPerTick);
        } else {
            bulldozerUtils.breakBlocksInCube(
                    centerBlock,
                    tool,
                    player,
                    radius,
                    collectedItems,
                    processedBlocks,
                    null,
                    diameter * diameter * diameter,
                    1,
                    maxBlocksPerTick);
        }

        bulldozerUtils.collectItems(player, tool, collectedItems);
    }

    public void toggleBulldozerMode(Player player) {
        if (bulldozerToggleMap.contains(player.getName())) {
            MessageUtils.actionBar(player, onMessage);
            bulldozerToggleMap.remove(player.getName());
        } else {
            MessageUtils.actionBar(player, offMessage);
            bulldozerToggleMap.add(player.getName());
        }
    }

    public boolean hasBulldozer(Player player) {
        return bulldozerToggleMap.contains(player.getName());
    }
}
