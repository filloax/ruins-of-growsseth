package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("structures")
public class StructureConfig {
    @ConfigEntry(id = "researcherTentEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "researcherTentEnabled.name")
    public static boolean researcherTentEnabled = true;
    @ConfigEntry(id = "caveCampEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "caveCampEnabled.name")
    public static boolean caveCampEnabled = true;
    @ConfigEntry(id = "beekeeperHouseEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "beekeeperHouseEnabled.name")
    public static boolean beekeeperHouseEnabled = true;
    @ConfigEntry(id = "conduitChurchEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "conduitChurchEnabled.name")
    public static boolean conduitChurchEnabled = true;
    @ConfigEntry(id = "enchantTowerEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "enchantTowerEnabled.name")
    public static boolean enchantTowerEnabled = true;
    @ConfigEntry(id = "abandonedForgeEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "abandonedForgeEnabled.name")
    public static boolean abandonedForgeEnabled = true;
    @ConfigEntry(id = "noteblockLabEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "noteblockLabEnabled.name")
    public static boolean noteblockLabEnabled = true;
    @ConfigEntry(id = "golemHouseEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "golemHouseEnabled.name")
    @Comment(value = "If changed ingame, takes effect on world reload", translation = T_PREF + "needsWorldReload.comment")
    public static boolean golemHouseEnabled = true;
    @ConfigEntry(id = "conduitRuinsEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "conduitRuinsEnabled.name")
    public static boolean conduitRuinsEnabled = true;
    @ConfigEntry(id = "noteblockShipEnabled", type = EntryType.BOOLEAN, translation = T_PREF + "noteblockShipEnabled.name")
    public static boolean noteblockShipEnabled = true;
}
