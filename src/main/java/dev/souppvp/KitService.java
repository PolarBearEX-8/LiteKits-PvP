package dev.souppvp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

final class KitService {
    private final SoupPvPPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, EditorSession> editorSessions = new HashMap<UUID, EditorSession>();
    private File editKitFile;
    private FileConfiguration editKitConfig;

    KitService(SoupPvPPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    void useKit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return;
        }

        Player player = (Player) sender;
        if (!plugin.settings().isKitsEnabled()) {
            player.sendMessage(messages.get("kit-disabled"));
            return;
        }

        if (!plugin.hasKitPermission(player)) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }

        if (!plugin.arena().isInGame(player)) {
            player.sendMessage(messages.get("join-first"));
            return;
        }

        if (plugin.settings().isCombatKitBlocked() && plugin.cooldowns().isInCombat(player)) {
            player.sendMessage(messages.get("combat-command-blocked"));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(messages.get("kit-usage"));
            return;
        }

        int kitNumber = parseKitNumber(args[0]);
        if (kitNumber < 1) {
            player.sendMessage(messages.get("kit-number-invalid"));
            return;
        }

        if (!hasKit(player, kitNumber)) {
            player.sendMessage(messages.get("no-kit").replace("{kit}", String.valueOf(kitNumber)));
            return;
        }

        plugin.arena().clearPlayer(player);
        applyKit(player, kitNumber);
        plugin.arena().selectKit(player, kitNumber);
        plugin.scoreboard().update(player);
        player.sendMessage(messages.get("kit-loaded").replace("{kit}", String.valueOf(kitNumber)));
    }

    void openKitEditor(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("only-players"));
            return;
        }

        Player player = (Player) sender;
        if (!plugin.settings().isEditKitEnabled()) {
            player.sendMessage(messages.get("editkit-disabled"));
            return;
        }

        if (!plugin.hasKitPermission(player)) {
            player.sendMessage(messages.get("no-permission"));
            return;
        }

        if (!plugin.arena().isInGame(player)) {
            player.sendMessage(messages.get("join-first"));
            return;
        }

        if (plugin.settings().isCombatEditKitBlocked() && plugin.cooldowns().isInCombat(player)) {
            player.sendMessage(messages.get("combat-command-blocked"));
            return;
        }

        if (args.length != 1) {
            player.sendMessage(messages.get("editkit-usage"));
            return;
        }

        int kitNumber = parseKitNumber(args[0]);
        if (kitNumber < 1) {
            player.sendMessage(messages.get("kit-number-invalid"));
            return;
        }

        restoreEditorPlayer(player);
        editorSessions.put(player.getUniqueId(), EditorSession.from(player, kitNumber));
        setupEditorPalette(player);

        Inventory inventory = Bukkit.createInventory(player, SoupPvPConstants.KIT_EDITOR_SIZE, getKitEditorTitle(kitNumber));
        KitData kitData = loadKit(player, kitNumber);
        if (kitData != null) {
            for (int slot = 0; slot < SoupPvPConstants.KIT_INVENTORY_SIZE; slot++) {
                inventory.setItem(toGuiInventorySlot(slot), cleanItem(kitData.inventoryContents[slot]));
            }
        }

        setArmorPlaceholders(inventory);
        if (kitData != null) {
            setGuiArmorContents(inventory, kitData.armorContents);
        }

        for (int slot = 0; slot < SoupPvPConstants.GUI_MAIN_START_SLOT; slot++) {
            inventory.setItem(slot, createButton(Material.BARRIER, ChatColor.RED + "Locked"));
        }
        for (int slot = SoupPvPConstants.GUI_LOCKED_START_SLOT; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, createButton(Material.BARRIER, ChatColor.RED + "Locked"));
        }
        inventory.setItem(SoupPvPConstants.SAVE_SLOT, createButton(Material.EMERALD_BLOCK, ChatColor.GREEN + "Save Kit"));

        player.openInventory(inventory);
    }

    void handleInventoryClick(InventoryClickEvent event) {
        if (!isKitEditor(event.getInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).updateInventory();
            return;
        }

        int rawSlot = event.getRawSlot();
        if (isLockedEditorSlot(rawSlot, event.getInventory().getSize())) {
            event.setCancelled(true);

            if (rawSlot == SoupPvPConstants.SAVE_SLOT) {
                Player player = (Player) event.getWhoClicked();
                EditorSession session = editorSessions.get(player.getUniqueId());
                if (session == null) {
                    player.sendMessage(messages.get("kit-editor-session-expired"));
                    player.closeInventory();
                    return;
                }

                session.saved = true;
                saveKit(player, event.getInventory(), session.kitNumber);
                player.sendMessage(messages.get("kit-saved").replace("{kit}", String.valueOf(session.kitNumber)));
                player.closeInventory();
            }
            return;
        }

        if (isGuiArmorSlot(rawSlot)) {
            handleArmorSlotClick(event);
            return;
        }

        int topSize = event.getInventory().getSize();
        if (rawSlot >= topSize) {
            int playerSlot = event.getSlot();
            if (playerSlot < SoupPvPConstants.PLAYER_HOTBAR_SIZE) {
                event.setCancelled(true);
                handleCategoryClick((Player) event.getWhoClicked(), playerSlot);
            }
        }
    }

    void handleInventoryDrag(InventoryDragEvent event) {
        if (!isKitEditor(event.getInventory())) {
            return;
        }

        for (Integer rawSlot : event.getRawSlots()) {
            int topSize = event.getInventory().getSize();
            if (isLockedEditorSlot(rawSlot, topSize)) {
                event.setCancelled(true);
                return;
            }

            if (rawSlot >= topSize + 27) {
                event.setCancelled(true);
                return;
            }
        }
    }

    void handlePlayerDropItem(PlayerDropItemEvent event) {
        if (editorSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    void handleInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player && isKitEditor(event.getInventory())) {
            final Player player = (Player) event.getPlayer();
            final EditorSession session = editorSessions.get(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    restoreEditorPlayer(player);
                    if (session != null && !session.saved && player.isOnline()) {
                        player.sendMessage(messages.get("kit-edit-cancelled"));
                    }
                }
            });
        }
    }

    void restoreEditorPlayer(Player player) {
        EditorSession session = editorSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        player.getInventory().setContents(session.inventoryContents);
        player.getInventory().setArmorContents(session.armorContents);
        player.updateInventory();
    }

    void clear() {
        editorSessions.clear();
    }

    void loadConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        editKitFile = new File(plugin.getDataFolder(), "editkit.yml");
        editKitConfig = YamlConfiguration.loadConfiguration(editKitFile);
        if (editKitFile.isFile() && editKitConfig.getInt("editkit-version", 1) < 3) {
            File backupFile = new File(plugin.getDataFolder(), "editkit.yml.old");
            if (backupFile.exists() && !backupFile.delete()) {
                plugin.getLogger().warning("Could not delete old editkit.yml backup: " + backupFile.getName());
                return;
            }
            if (!editKitFile.renameTo(backupFile)) {
                plugin.getLogger().warning("Could not back up old editkit.yml to " + backupFile.getName());
                return;
            }
            editKitConfig = new YamlConfiguration();
        }
        migrateCategoryAliases();
        setEditKitDefaults(shouldSeedDefaultCategories());

        try {
            editKitConfig.save(editKitFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save editkit.yml: " + exception.getMessage());
        }
    }

    private void saveKit(Player player, Inventory inventory, int kitNumber) {
        ItemStack[] inventoryContents = new ItemStack[SoupPvPConstants.KIT_INVENTORY_SIZE];
        ItemStack[] armorContents = new ItemStack[SoupPvPConstants.KIT_ARMOR_SIZE];

        for (int slot = 0; slot < SoupPvPConstants.KIT_INVENTORY_SIZE; slot++) {
            inventoryContents[slot] = cleanItem(inventory.getItem(toGuiInventorySlot(slot)));
        }

        armorContents[3] = cleanArmorSlotItem(inventory.getItem(SoupPvPConstants.GUI_ARMOR_START_SLOT));
        armorContents[2] = cleanArmorSlotItem(inventory.getItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 1));
        armorContents[1] = cleanArmorSlotItem(inventory.getItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 2));
        armorContents[0] = cleanArmorSlotItem(inventory.getItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 3));

        File kitFile = getKitFile(player, kitNumber);
        if (!kitFile.getParentFile().exists()) {
            kitFile.getParentFile().mkdirs();
        }

        BukkitObjectOutputStream output = null;
        try {
            output = new BukkitObjectOutputStream(new FileOutputStream(kitFile));
            output.writeObject(inventoryContents);
            output.writeObject(armorContents);
        } catch (IOException exception) {
            player.sendMessage(messages.get("kit-save-failed"));
            plugin.getLogger().warning("Could not save kit " + kitNumber + " for " + player.getName() + ": " + exception.getMessage());
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void applyKit(Player player, int kitNumber) {
        KitData kitData = loadKit(player, kitNumber);
        if (kitData == null) {
            player.sendMessage(messages.get("no-kit").replace("{kit}", String.valueOf(kitNumber)));
            return;
        }

        player.getInventory().setContents(kitData.inventoryContents);
        player.getInventory().setArmorContents(kitData.armorContents);
        player.updateInventory();
    }

    private boolean hasKit(Player player, int kitNumber) {
        return getKitFile(player, kitNumber).isFile();
    }

    private int parseKitNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private File getKitFile(Player player, int kitNumber) {
        return new File(new File(new File(plugin.getDataFolder(), "kits"), player.getUniqueId().toString()), kitNumber + ".dat");
    }

    private String getKitEditorTitle(int kitNumber) {
        return SoupPvPConstants.KIT_EDITOR_TITLE_PREFIX + kitNumber;
    }

    private int toGuiInventorySlot(int playerInventorySlot) {
        if (playerInventorySlot >= 0 && playerInventorySlot < SoupPvPConstants.PLAYER_HOTBAR_SIZE) {
            return SoupPvPConstants.GUI_HOTBAR_START_SLOT + playerInventorySlot;
        }

        return SoupPvPConstants.GUI_MAIN_START_SLOT + (playerInventorySlot - SoupPvPConstants.PLAYER_HOTBAR_SIZE);
    }

    private boolean isGuiArmorSlot(int rawSlot) {
        return rawSlot >= SoupPvPConstants.GUI_ARMOR_START_SLOT
                && rawSlot < SoupPvPConstants.GUI_ARMOR_START_SLOT + SoupPvPConstants.KIT_ARMOR_SIZE;
    }

    private boolean isLockedEditorSlot(int rawSlot, int topSize) {
        return rawSlot >= 0
                && rawSlot < topSize
                && (rawSlot < SoupPvPConstants.GUI_MAIN_START_SLOT || rawSlot >= SoupPvPConstants.GUI_LOCKED_START_SLOT);
    }

    private void setArmorPlaceholders(Inventory inventory) {
        inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT, createButton(Material.STAINED_GLASS_PANE, (short) 7, ChatColor.GRAY + "Helmet"));
        inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 1, createButton(Material.STAINED_GLASS_PANE, (short) 7, ChatColor.GRAY + "Chestplate"));
        inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 2, createButton(Material.STAINED_GLASS_PANE, (short) 7, ChatColor.GRAY + "Leggings"));
        inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 3, createButton(Material.STAINED_GLASS_PANE, (short) 7, ChatColor.GRAY + "Boots"));
    }

    private void setGuiArmorContents(Inventory inventory, ItemStack[] armorContents) {
        if (armorContents.length > 3 && armorContents[3] != null) {
            inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT, cleanItem(armorContents[3]));
        }
        if (armorContents.length > 2 && armorContents[2] != null) {
            inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 1, cleanItem(armorContents[2]));
        }
        if (armorContents.length > 1 && armorContents[1] != null) {
            inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 2, cleanItem(armorContents[1]));
        }
        if (armorContents.length > 0 && armorContents[0] != null) {
            inventory.setItem(SoupPvPConstants.GUI_ARMOR_START_SLOT + 3, cleanItem(armorContents[0]));
        }
    }

    @SuppressWarnings("deprecation")
    private void handleArmorSlotClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        if (!isArmorPlaceholder(current)) {
            return;
        }

        event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }

        event.setCurrentItem(cursor.clone());
        event.setCursor(null);
    }

    private void setupEditorPalette(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[SoupPvPConstants.KIT_ARMOR_SIZE]);

        Map<Integer, String> usedSlots = new HashMap<Integer, String>();
        for (String category : getCategoryKeys()) {
            int slot = editKitConfig.getInt("categories." + category + ".hotbar-slot", -1);
            if (slot >= 0 && slot < SoupPvPConstants.PLAYER_HOTBAR_SIZE) {
                if (usedSlots.containsKey(slot)) {
                    plugin.getLogger().warning("Editkit category '" + category + "' uses hotbar-slot " + slot
                            + ", already used by '" + usedSlots.get(slot) + "'.");
                    continue;
                }

                usedSlots.put(slot, category);
                player.getInventory().setItem(slot, getCategoryIcon(category));
            } else {
                plugin.getLogger().warning("Editkit category '" + category + "' has invalid hotbar-slot: " + slot);
            }
        }

        player.updateInventory();
    }

    private List<String> getCategoryKeys() {
        if (!editKitConfig.isConfigurationSection("categories")) {
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<String>();
        for (String key : editKitConfig.getConfigurationSection("categories").getKeys(false)) {
            if (editKitConfig.getBoolean("categories." + key + ".enabled", true)) {
                keys.add(key);
            }
        }
        Collections.sort(keys, new Comparator<String>() {
            @Override
            public int compare(String first, String second) {
                int firstOrder = editKitConfig.getInt("categories." + first + ".order",
                        editKitConfig.getInt("categories." + first + ".hotbar-slot", 0));
                int secondOrder = editKitConfig.getInt("categories." + second + ".order",
                        editKitConfig.getInt("categories." + second + ".hotbar-slot", 0));
                return firstOrder - secondOrder;
            }
        });
        return keys;
    }

    private void handleCategoryClick(Player player, int playerSlot) {
        for (String category : getCategoryKeys()) {
            if (editKitConfig.getInt("categories." + category + ".hotbar-slot", -1) == playerSlot) {
                loadCategoryItems(player, category);
                return;
            }
        }
    }

    private void migrateCategoryAliases() {
        if (editKitConfig.isConfigurationSection("categories")) {
            return;
        }

        String alias = getCategoryAlias();
        if (alias == null) {
            return;
        }

        ConfigurationSection aliasSection = editKitConfig.getConfigurationSection(alias);
        for (String key : aliasSection.getKeys(false)) {
            editKitConfig.set("categories." + key, aliasSection.get(key));
        }
        editKitConfig.set(alias, null);
        plugin.getLogger().warning("Renamed editkit.yml '" + alias + "' section to 'categories'.");
    }

    private String getCategoryAlias() {
        if (editKitConfig.isConfigurationSection("category")) {
            return "category";
        }
        if (editKitConfig.isConfigurationSection("catagory")) {
            return "catagory";
        }
        if (editKitConfig.isConfigurationSection("catagories")) {
            return "catagories";
        }
        return null;
    }

    private void clearPaletteItems(Player player) {
        for (int slot = SoupPvPConstants.PLAYER_MAIN_START_SLOT; slot <= SoupPvPConstants.PLAYER_MAIN_END_SLOT; slot++) {
            player.getInventory().setItem(slot, null);
        }
    }

    private void loadCategoryItems(Player player, String category) {
        clearPaletteItems(player);
        List<?> items = editKitConfig.getList("categories." + category + ".items");
        if (items != null) {
            int slot = SoupPvPConstants.PLAYER_MAIN_START_SLOT;
            for (Object object : items) {
                if (slot > SoupPvPConstants.PLAYER_MAIN_END_SLOT) {
                    break;
                }

                ItemStack itemStack = null;
                if (object instanceof ItemStack) {
                    itemStack = ((ItemStack) object).clone();
                } else if (object instanceof String) {
                    itemStack = parseItemSpec((String) object);
                } else if (object instanceof Map) {
                    itemStack = parseItemMap((Map<?, ?>) object);
                }

                if (itemStack != null) {
                    player.getInventory().setItem(slot, itemStack);
                    slot++;
                }
            }
        }
        player.updateInventory();
    }

    private ItemStack getCategoryIcon(String category) {
        Object iconObject = editKitConfig.get("categories." + category + ".icon");
        if (iconObject instanceof ItemStack) {
            ItemStack icon = (ItemStack) iconObject;
            if (icon.getType() != Material.AIR) {
                return icon.clone();
            }
        } else if (iconObject instanceof String) {
            ItemStack icon = parseItemSpec((String) iconObject);
            if (icon != null) {
                return icon;
            }
        }

        String itemType = editKitConfig.getString("categories." + category + ".item");
        if (itemType != null) {
            ItemStack icon = createItem(
                    itemType,
                    1,
                    editKitConfig.getInt("categories." + category + ".damage", 0),
                    editKitConfig.getString("categories." + category + ".name"));
            if (icon != null) {
                return icon;
            }
        }

        return createButton(Material.CHEST, ChatColor.AQUA + category);
    }

    private ItemStack cleanItem(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        return itemStack.clone();
    }

    private ItemStack cleanArmorSlotItem(ItemStack itemStack) {
        if (isArmorPlaceholder(itemStack)) {
            return null;
        }
        return cleanItem(itemStack);
    }

    private boolean isArmorPlaceholder(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() != Material.STAINED_GLASS_PANE || !itemStack.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = itemStack.getItemMeta();
        return meta.hasDisplayName()
                && (meta.getDisplayName().contains("Helmet")
                || meta.getDisplayName().contains("Chestplate")
                || meta.getDisplayName().contains("Leggings")
                || meta.getDisplayName().contains("Boots"));
    }

    private ItemStack createButton(Material material, String name) {
        return createButton(material, (short) 0, name);
    }

    private ItemStack createButton(Material material, short durability, String name) {
        ItemStack itemStack = new ItemStack(material, 1);
        itemStack.setDurability(durability);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setDisplayName(name);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private ItemStack parseItemSpec(String spec) {
        String[] parts = spec.split(":", 4);
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase());
        if (material == null) {
            plugin.getLogger().warning("Invalid editkit item material: " + spec);
            return null;
        }

        int amount = 1;
        short durability = 0;
        String name = null;

        if (parts.length > 1 && parts[1].trim().length() > 0) {
            amount = parseInt(parts[1], 1);
        }
        if (parts.length > 2 && parts[2].trim().length() > 0) {
            durability = (short) parseInt(parts[2], 0);
        }
        if (parts.length > 3 && parts[3].trim().length() > 0) {
            name = ChatColor.translateAlternateColorCodes('&', parts[3].trim());
        }

        ItemStack itemStack = new ItemStack(material, amount, durability);
        if (name != null) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack parseItemMap(Map<?, ?> map) {
        Object typeObject = map.containsKey("type") ? map.get("type") : map.get("item");
        if (typeObject == null) {
            return null;
        }

        int amount = getIntFromMap(map, "amount", 1);
        TypeData typeData = parseTypeData(String.valueOf(typeObject));
        if (typeData.material == null) {
            plugin.getLogger().warning("Invalid editkit item material: " + typeObject);
            return null;
        }

        int damage = typeData.damage;
        if (map.containsKey("damage")) {
            damage = getIntFromMap(map, "damage", damage);
        } else if (map.containsKey("data")) {
            damage = getIntFromMap(map, "data", damage);
        } else if (map.containsKey("durability")) {
            damage = getIntFromMap(map, "durability", damage);
        }

        String name = map.get("name") == null ? null : String.valueOf(map.get("name"));
        ItemStack itemStack = new ItemStack(typeData.material, amount, (short) damage);
        applyMeta(itemStack, map.get("meta"));
        applyEnchantments(itemStack, map.get("enchants"));
        applyEnchantments(itemStack, map.get("enchantments"));

        if (name != null && name.trim().length() > 0) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name.trim()));
            applyLore(meta, map.get("lore"));
            itemStack.setItemMeta(meta);
        } else if (map.containsKey("lore")) {
            ItemMeta meta = itemStack.getItemMeta();
            applyLore(meta, map.get("lore"));
            itemStack.setItemMeta(meta);
        }

        return itemStack;
    }

    private ItemStack createItem(String type, int amount, int damage, String name) {
        TypeData typeData = parseTypeData(type);
        if (typeData.material == null) {
            plugin.getLogger().warning("Invalid editkit item material: " + type);
            return null;
        }

        int finalDamage = damage == 0 ? typeData.damage : damage;
        ItemStack itemStack = new ItemStack(typeData.material, amount, (short) finalDamage);
        if (name != null && name.trim().length() > 0) {
            ItemMeta meta = itemStack.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name.trim()));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    @SuppressWarnings("deprecation")
    private TypeData parseTypeData(String rawType) {
        String[] parts = rawType.trim().split(":", 2);
        Material material = null;
        int damage = 0;

        if (parts.length > 1) {
            damage = parseInt(parts[1], 0);
        }

        int id = parseInt(parts[0], -1);
        if (id >= 0) {
            material = Material.getMaterial(id);
        } else {
            material = Material.matchMaterial(parts[0].trim().toUpperCase());
        }

        return new TypeData(material, damage);
    }

    @SuppressWarnings("unchecked")
    private void applyMeta(ItemStack itemStack, Object metaObject) {
        if (metaObject instanceof ItemMeta) {
            itemStack.setItemMeta((ItemMeta) metaObject);
            return;
        }

        if (metaObject instanceof Map) {
            try {
                Object deserialized = ConfigurationSerialization.deserializeObject((Map<String, Object>) metaObject);
                if (deserialized instanceof ItemMeta) {
                    itemStack.setItemMeta((ItemMeta) deserialized);
                }
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Invalid editkit item meta for " + itemStack.getType() + ": " + exception.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void applyEnchantments(ItemStack itemStack, Object enchantmentsObject) {
        if (enchantmentsObject instanceof Map) {
            Map<Object, Object> enchantmentMap = (Map<Object, Object>) enchantmentsObject;
            for (Map.Entry<Object, Object> entry : enchantmentMap.entrySet()) {
                addEnchantment(itemStack, String.valueOf(entry.getKey()), parseInt(String.valueOf(entry.getValue()), 1));
            }
            return;
        }

        if (enchantmentsObject instanceof List) {
            for (Object object : (List<?>) enchantmentsObject) {
                String[] parts = String.valueOf(object).split(":", 2);
                int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
                addEnchantment(itemStack, parts[0], level);
            }
        }
    }

    private void applyLore(ItemMeta meta, Object loreObject) {
        if (loreObject == null) {
            return;
        }

        List<String> lore = new ArrayList<String>();
        if (loreObject instanceof List) {
            for (Object line : (List<?>) loreObject) {
                lore.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(line)));
            }
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(loreObject)));
        }
        meta.setLore(lore);
    }

    private void addEnchantment(ItemStack itemStack, String enchantmentName, int level) {
        Enchantment enchantment = Enchantment.getByName(enchantmentName.trim().toUpperCase());
        if (enchantment == null) {
            plugin.getLogger().warning("Invalid editkit enchantment for " + itemStack.getType() + ": " + enchantmentName);
            return;
        }

        itemStack.addUnsafeEnchantment(enchantment, level);
    }

    private int getIntFromMap(Map<?, ?> map, String key, int fallback) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value != null) {
            return parseInt(String.valueOf(value), fallback);
        }

        return fallback;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean isKitEditor(Inventory inventory) {
        return inventory != null && inventory.getTitle() != null
                && inventory.getTitle().startsWith(SoupPvPConstants.KIT_EDITOR_TITLE_PREFIX);
    }

    private KitData loadKit(Player player, int kitNumber) {
        File kitFile = getKitFile(player, kitNumber);
        if (!kitFile.isFile()) {
            return null;
        }

        BukkitObjectInputStream input = null;
        try {
            input = new BukkitObjectInputStream(new FileInputStream(kitFile));
            ItemStack[] inventoryContents = (ItemStack[]) input.readObject();
            ItemStack[] armorContents = (ItemStack[]) input.readObject();
            return new KitData(inventoryContents, armorContents);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not load kit " + kitNumber + " for " + player.getName() + ": " + exception.getMessage());
        } catch (ClassNotFoundException exception) {
            plugin.getLogger().warning("Could not load kit " + kitNumber + " for " + player.getName() + ": " + exception.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }

        return null;
    }

    private void setEditKitDefaults(boolean seedDefaultCategories) {
        editKitConfig.options().header("LiteKits editkit example\n"
                + "Set categories.<name>.enabled to false to hide a category.\n"
                + "hotbar-slot is the selector slot in the player's hotbar, from 0 to 8.\n"
                + "items supports simple strings like MATERIAL:amount:damage:&Name or maps with type, amount, damage, name, lore, and enchants.\n"
                + "Delete this file or leave it empty to regenerate the full example.");

        if (!editKitConfig.contains("editkit-version")) {
            editKitConfig.set("editkit-version", 3);
        }

        if (!editKitConfig.contains("layout")) {
            editKitConfig.set("layout", "kit inventory rows y1-y3, hotbar row y4, armor row y5 slots 45-48, save slot 53");
        }

        if (seedDefaultCategories) {
            setCategoryDefault("weapon", 0, "IRON_SWORD", "&cWeapon", mapItems(
                    item("DIAMOND_SWORD"),
                    item("IRON_SWORD"),
                    item("STONE_SWORD"),
                    item("FISHING_ROD"),
                    item("BOW"),
                    item("ARROW", 16)
            ));
            setCategoryDefault("soup", 1, "MUSHROOM_SOUP", "&6Soup", createSoupItems());
            setCategoryDefault("pot", 2, "POTION", "&dPot", mapItems(
                    item("POTION", 1, 16421, "&cInstant Health II"),
                    item("POTION", 1, 16418, "&eSwiftness II"),
                    item("POTION", 1, 16425, "&4Strength II"),
                    item("POTION", 1, 16417, "&7Regeneration")
            ));
            setCategoryDefault("misc", 3, "CHEST", "&bMisc", mapItems(
                    item("DIAMOND_HELMET"),
                    item("DIAMOND_CHESTPLATE"),
                    item("DIAMOND_LEGGINGS"),
                    item("DIAMOND_BOOTS"),
                    item("LEATHER_HELMET"),
                    item("LEATHER_CHESTPLATE"),
                    item("LEATHER_LEGGINGS"),
                    item("LEATHER_BOOTS"),
                    item("BOWL", 16),
                    item("RED_MUSHROOM", 16),
                    item("BROWN_MUSHROOM", 16),
                    item("ENDER_PEARL", 4)
            ));
            setCategoryDefault("enchanted", 4, "ENCHANTMENT_TABLE", "&5Enchant", mapItems(
                    enchantedItem("DIAMOND_SWORD", 1, 0, "&dSharpness Sword", enchantments(
                            enchantment("DAMAGE_ALL", 1)
                    )),
                    enchantedItem("IRON_SWORD", 1, 0, "&dSharpness II Iron Sword", enchantments(
                            enchantment("DAMAGE_ALL", 2)
                    )),
                    enchantedItem("BOW", 1, 0, "&dPower Bow", enchantments(
                            enchantment("ARROW_DAMAGE", 1)
                    )),
                    enchantedItem("DIAMOND_HELMET", 1, 0, "&dProtection Helmet", enchantments(
                            enchantment("PROTECTION_ENVIRONMENTAL", 1)
                    )),
                    enchantedItem("DIAMOND_CHESTPLATE", 1, 0, "&dProtection Chestplate", enchantments(
                            enchantment("PROTECTION_ENVIRONMENTAL", 1)
                    )),
                    enchantedItem("DIAMOND_BOOTS", 1, 0, "&dFeather Boots", enchantments(
                            enchantment("PROTECTION_FALL", 2)
                    ))
            ));
            setCategoryDefault("examples", 8, "BOOK", "&eExamples", false, mapItems(
                    item("MUSHROOM_SOUP", 1, 0, "&6Basic Item"),
                    item("276", 1, 0, "&bNumeric ID Sword"),
                    item("POTION", 1, 16421, "&cPotion With Lore", list("&7type + amount + damage", "&7name + lore example")),
                    enchantedItem("DIAMOND_SWORD", 1, 0, "&dMap Enchant Example", list("&7enchants map example"), enchantments(
                            enchantment("DAMAGE_ALL", 2),
                            enchantment("DURABILITY", 1)
                    ))
            ));
        }
    }

    private boolean shouldSeedDefaultCategories() {
        return !editKitFile.isFile() || editKitConfig.getKeys(false).isEmpty();
    }

    private void setCategoryDefault(String category, int hotbarSlot, String item, String name, List<Map<String, Object>> items) {
        setCategoryDefault(category, hotbarSlot, item, name, true, items);
    }

    private void setCategoryDefault(String category, int hotbarSlot, String item, String name, boolean enabled, List<Map<String, Object>> items) {
        String path = "categories." + category;
        if (!editKitConfig.contains(path + ".enabled")) {
            editKitConfig.set(path + ".enabled", enabled);
        }

        if (!editKitConfig.contains(path + ".hotbar-slot")) {
            editKitConfig.set(path + ".hotbar-slot", hotbarSlot);
        }

        if (!editKitConfig.contains(path + ".order")) {
            editKitConfig.set(path + ".order", hotbarSlot);
        }

        if (!editKitConfig.contains(path + ".item")) {
            editKitConfig.set(path + ".item", item);
        }

        if (!editKitConfig.contains(path + ".name")) {
            editKitConfig.set(path + ".name", name);
        }

        if (!editKitConfig.contains(path + ".items")) {
            editKitConfig.set(path + ".items", items);
        }
    }

    private List<Map<String, Object>> createSoupItems() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < 27; index++) {
            items.add(item("MUSHROOM_SOUP"));
        }
        return items;
    }

    @SafeVarargs
    private final List<Map<String, Object>> mapItems(Map<String, Object>... items) {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> item : items) {
            list.add(item);
        }
        return list;
    }

    private List<String> list(String... values) {
        List<String> list = new ArrayList<String>();
        for (String value : values) {
            list.add(value);
        }
        return list;
    }

    private Map<String, Object> item(String type) {
        return item(type, 1, 0, null);
    }

    private Map<String, Object> item(String type, int amount) {
        return item(type, amount, 0, null);
    }

    private Map<String, Object> item(String type, int amount, int damage, String name) {
        return item(type, amount, damage, name, null);
    }

    private Map<String, Object> item(String type, int amount, int damage, String name, List<String> lore) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("type", type);
        if (amount != 1) {
            map.put("amount", amount);
        }
        if (damage != 0) {
            map.put("damage", damage);
        }
        if (name != null) {
            map.put("name", name);
        }
        if (lore != null) {
            map.put("lore", lore);
        }
        return map;
    }

    private Map<String, Object> enchantedItem(String type, int amount, int damage, String name, Map<String, Object> enchantments) {
        return enchantedItem(type, amount, damage, name, null, enchantments);
    }

    private Map<String, Object> enchantedItem(String type, int amount, int damage, String name, List<String> lore, Map<String, Object> enchantments) {
        Map<String, Object> map = item(type, amount, damage, name, lore);
        map.put("enchants", enchantments);
        return map;
    }

    @SafeVarargs
    private final Map<String, Object> enchantments(Map<String, Object>... enchantments) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Map<String, Object> enchantment : enchantments) {
            map.putAll(enchantment);
        }
        return map;
    }

    private Map<String, Object> enchantment(String name, int level) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(name, level);
        return map;
    }
}
