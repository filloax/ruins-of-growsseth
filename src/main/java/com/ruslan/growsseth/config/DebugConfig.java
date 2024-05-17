package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("debug")
public class DebugConfig {
    @ConfigEntry(id = "debugNpcDialogues", type = EntryType.BOOLEAN, translation = T_PREF + "debugNpcDialogues.name")
    public static boolean debugNpcDialogues = false;

    @ConfigEntry(id = "structuresDebugMode", type = EntryType.BOOLEAN, translation = T_PREF + "structuresDebugMode.name")
    @Comment(value = "Make structures wildly more likely, for dev usage.", translation = T_PREF + "structuresDebugMode.comment")
    public static boolean structuresDebugMode = false;
}
