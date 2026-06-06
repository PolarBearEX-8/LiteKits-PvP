package dev.souppvp;

import org.bukkit.inventory.ItemStack;

final class KitData {
    final ItemStack[] inventoryContents;
    final ItemStack[] armorContents;

    KitData(ItemStack[] inventoryContents, ItemStack[] armorContents) {
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
    }
}
