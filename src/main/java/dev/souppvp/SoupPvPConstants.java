package dev.souppvp;

import org.bukkit.ChatColor;

final class SoupPvPConstants {
    static final String KIT_EDITOR_TITLE_PREFIX = ChatColor.DARK_GREEN + "LiteKits Kit ";
    static final String ADMIN_PERMISSION = "litekits.admin";
    static final String LEGACY_ADMIN_PERMISSION = "souppvp.admin";
    static final String KIT_PERMISSION = "litekits.kit";
    static final String LEGACY_KIT_PERMISSION = "souppvp.kit";
    static final int KIT_EDITOR_SIZE = 54;
    static final int KIT_INVENTORY_SIZE = 36;
    static final int KIT_ARMOR_SIZE = 4;
    static final int GUI_MAIN_START_SLOT = 9;
    static final int GUI_HOTBAR_START_SLOT = 36;
    static final int GUI_ARMOR_START_SLOT = 45;
    static final int GUI_LOCKED_START_SLOT = 49;
    static final int SAVE_SLOT = 53;
    static final int PLAYER_HOTBAR_SIZE = 9;
    static final int PLAYER_MAIN_START_SLOT = 9;
    static final int PLAYER_MAIN_END_SLOT = 35;
    static final double SOUP_HEAL_AMOUNT = 6.0D;
    static final int SOUP_FOOD_AMOUNT = 6;
    static final float SOUP_SATURATION_AMOUNT = 7.2F;
    static final int SCOREBOARD_TASK_INTERVAL_TICKS = 20;

    private SoupPvPConstants() {
    }
}
