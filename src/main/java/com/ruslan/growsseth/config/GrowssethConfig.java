package com.ruslan.growsseth.config;


import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

@Config(
    value = "ruins-of-growsseth",
    categories = {
        ClientConfig.class,
        ResearcherConfig.class,
        WebConfig.class,
        QuestConfig.class,
        StructureConfig.class,
        WorldPresetConfig.class
    }
)
public final class GrowssethConfig {
    public static final String T_PREF = "growsseth.config.";

    @ConfigEntry(id = "serverLanguage", type = EntryType.STRING, translation = T_PREF + "serverLanguage.name")
    public static String serverLanguage = "it_it";

    @ConfigEntry(id = "modLootInVanillaStructures", type = EntryType.BOOLEAN, translation = T_PREF + "modLootInVanillaStructures.name")
    public static boolean modLootInVanillaStructures = false;
}