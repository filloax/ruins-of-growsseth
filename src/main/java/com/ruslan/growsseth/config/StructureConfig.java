package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("structures")
public class StructureConfig {
    @ConfigEntry(id = "researcherTentEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "researcherTentEnabled")
    public static boolean researcherTentEnabled = true;
    @ConfigEntry(id = "caveCampEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "caveCampEnabled")
    public static boolean caveCampEnabled = true;
    @ConfigEntry(id = "beekeeperHouseEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "beekeeperHouseEnabled")
    public static boolean beekeeperHouseEnabled = true;
    @ConfigEntry(id = "conduitChurchEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "conduitChurchEnabled")
    public static boolean conduitChurchEnabled = true;
    @ConfigEntry(id = "enchantTowerEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "enchantTowerEnabled")
    public static boolean enchantTowerEnabled = true;
    @ConfigEntry(id = "forgeEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "forgeEnabled")
    public static boolean forgeEnabled = true;
    @Comment(value = "If changed ingame, takes effect on world reload", translation = T_PREF + "comment.golemHouseEnabled")
    @ConfigEntry(id = "golemHouseEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "golemHouseEnabled")
    public static boolean golemHouseEnabled = true;
    @ConfigEntry(id = "noteblockLabEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "noteblockLabEnabled")
    public static boolean noteblockLabEnabled = true;
    @ConfigEntry(id = "noteblockShipEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "noteblockShipEnabled")
    public static boolean noteblockShipEnabled = true;
}
