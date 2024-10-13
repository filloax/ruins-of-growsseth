package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("structures")
public class StructureConfig {
    @ConfigEntry(id = "researcherTentEnabled", translation = T_PREF + "researcherTentEnabled.name")
    public static boolean researcherTentEnabled = true;
    @ConfigEntry(id = "caveCampEnabled", translation = T_PREF + "caveCampEnabled.name")
    public static boolean caveCampEnabled = true;
    @ConfigEntry(id = "beekeeperHouseEnabled", translation = T_PREF + "beekeeperHouseEnabled.name")
    public static boolean beekeeperHouseEnabled = true;
    @ConfigEntry(id = "conduitChurchEnabled", translation = T_PREF + "conduitChurchEnabled.name")
    public static boolean conduitChurchEnabled = true;
    @ConfigEntry(id = "enchantTowerEnabled", translation = T_PREF + "enchantTowerEnabled.name")
    public static boolean enchantTowerEnabled = true;
    @ConfigEntry(id = "abandonedForgeEnabled", translation = T_PREF + "abandonedForgeEnabled.name")
    public static boolean abandonedForgeEnabled = true;
    @ConfigEntry(id = "noteblockLabEnabled", translation = T_PREF + "noteblockLabEnabled.name")
    public static boolean noteblockLabEnabled = true;
    @ConfigEntry(id = "golemHouseEnabled", translation = T_PREF + "golemHouseEnabled.name")
    @Comment(value = "If changed ingame, takes effect on world reload", translation = T_PREF + "needsWorldReload.comment")
    public static boolean golemHouseEnabled = true;
    @ConfigEntry(id = "conduitRuinsEnabled", translation = T_PREF + "conduitRuinsEnabled.name")
    public static boolean conduitRuinsEnabled = true;
    @ConfigEntry(id = "noteblockShipEnabled", translation = T_PREF + "noteblockShipEnabled.name")
    public static boolean noteblockShipEnabled = true;
    @ConfigEntry(id = "researcherTentSimpleEnabled", translation = T_PREF + "researcherTentSimpleEnabled.name")
    @Comment(value = "Warning: with this version the final quest might work incorrectly. It's also recommended to enable only one variant.", translation = T_PREF + "researcherTentSimpleEnabled.comment")
    public static boolean researcherTentSimpleEnabled = false;


    @ConfigButton(text = "Disable all")
    public static final Runnable disableAllStructures = () -> toggleAll(false);

    @ConfigButton(text = "Enable all")
    public static final Runnable enableAllStructures = () -> toggleAll(true);

    private static void toggleAll(boolean choice) {
        researcherTentEnabled = choice;
        caveCampEnabled = choice;
        beekeeperHouseEnabled = choice;
        conduitChurchEnabled = choice;
        enchantTowerEnabled = choice;
        abandonedForgeEnabled = choice;
        noteblockLabEnabled = choice;
        golemHouseEnabled = choice;
        conduitRuinsEnabled = choice;
        noteblockShipEnabled = choice;
        // The tent with no cellar should always be enabled manually
        if (!choice)
            researcherTentSimpleEnabled = false;
    }
}
