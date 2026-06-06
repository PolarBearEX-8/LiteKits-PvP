package dev.souppvp;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class EditorSession {
    final ItemStack[] inventoryContents;
    final ItemStack[] armorContents;
    final int kitNumber;
    boolean saved;

    private EditorSession(ItemStack[] inventoryContents, ItemStack[] armorContents, int kitNumber) {
        this.inventoryContents = inventoryContents;
        this.armorContents = armorContents;
        this.kitNumber = kitNumber;
    }

    static EditorSession from(Player player, int kitNumber) {
        return new EditorSession(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                kitNumber);
    }
}
