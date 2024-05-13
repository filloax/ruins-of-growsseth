package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("misc")
public class MiscConfig {
    @ConfigEntry(id = "modLootInVanillaStructures", type = EntryType.BOOLEAN, translation = T_PREF + "modLootInVanillaStructures.name")
    @Comment(value = "If changed ingame, takes effect on world reload", translation = T_PREF + "needsWorldReload.comment")
    public static boolean modLootInVanillaStructures = false;

    @ConfigEntry(id = "zombieGuberSpawnChance", type = EntryType.FLOAT, translation = T_PREF + "zombieGuberSpawnChance.name")
    public static float zombieGuberSpawnChance = 0.2f;

    @ConfigEntry(id = "disableNpcDialogues", type = EntryType.BOOLEAN, translation = T_PREF + "disableNpcDialogues.name")
    public static boolean disableNpcDialogues = false;
}
