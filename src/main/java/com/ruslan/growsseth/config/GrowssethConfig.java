package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

@Config(
    value = "ruins-of-growsseth",
    categories = {
        MiscConfig.class,
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
    @Comment(
            value = "Choose between 'en_us' and 'it_it'. Used for npc dialogues, researcher diaries and structure books.",
            translation = T_PREF + "serverLanguage.comment"
    )
    public static String serverLanguage = "it_it";
}