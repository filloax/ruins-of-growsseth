package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("modcompat")
public class ModCompatConfig {
    @ConfigEntry(id = "cobblemonResearcherRewardEnabled", translation = T_PREF + "cobblemonResearcherRewardEnabled.name")
    public static boolean cobblemonResearcherRewardEnabled = true;

    @ConfigEntry(id = "cobblemonResearcherRewardCooldownDays", translation = T_PREF + "cobblemonResearcherRewardCooldownDays.name")
    public static float cobblemonResearcherRewardCooldownDays = 1.0f;
}
