package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("quest")
public final class QuestConfig {
    @ConfigEntry(id = "finalQuestStartName", type = EntryType.STRING, translation = T_PREF + "finalQuestStartName.name")
    public static String finalQuestStartName = "researcher_end_quest_start";
    @ConfigEntry(id = "finalQuestZombieName", type = EntryType.STRING, translation = T_PREF + "finalQuestZombieName.name")
    public static String finalQuestZombieName = "researcher_end_quest_zombie";
    @ConfigEntry(id = "finalQuestLeaveName", type = EntryType.STRING, translation = T_PREF + "finalQuestLeaveName.name")
    public static String finalQuestLeaveName = "researcher_end_quest_leave";
    @ConfigEntry(id = "researcherCuredDiscount", type = EntryType.FLOAT, translation = T_PREF + "researcherCuredDiscount.name")
    @Comment(
            value = "Price multiplier for the researcher's trades after curing him. Only affects emeralds.",
            translation = T_PREF + "researcherCuredDiscount.comment"
    )
    public static float researcherCuredDiscount = 0.5f;
}
