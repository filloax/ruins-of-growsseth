package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("researcher")
public final class ResearcherConfig {
    @ConfigEntry(id = "immortalResearcher", type = EntryType.BOOLEAN, translation = T_PREF + "immortalResearcher.name")
    public static boolean immortalResearcher = false;
    @ConfigEntry(id = "researcherTeleports", type = EntryType.BOOLEAN, translation = T_PREF + "researcherTeleports.name")
    public static boolean researcherTeleports = true;
    @ConfigEntry(id = "researcherAntiCheat", type = EntryType.BOOLEAN, translation = T_PREF + "researcherAntiCheat.name")
    public static boolean researcherAntiCheat = true;
    @ConfigEntry(id = "researcherInteractsWithMobs", type = EntryType.BOOLEAN, translation = T_PREF + "researcherInteractsWithMobs.name")
    public static boolean researcherInteractsWithMobs = true;
    @ConfigEntry(id = "researcherStrikesFirst", type = EntryType.BOOLEAN, translation = T_PREF + "researcherStrikesFirst.name")
    public static boolean researcherStrikesFirst = false;
    @ConfigEntry(id = "researcherWritesDiaries", type = EntryType.BOOLEAN, translation = T_PREF + "researcherWritesDiaries.name")
    public static boolean researcherWritesDiaries = true;
    @ConfigEntry(id = "persistentResearcher", type = EntryType.BOOLEAN, translation = T_PREF + "persistentResearcher.name")
    public static boolean persistentResearcher = true;   // makes all researcher share the same memory
    @ConfigEntry(id = "researcherBorrowPenalty", type = EntryType.FLOAT, translation = T_PREF + "researcherBorrowPenalty.name")
    public static float researcherBorrowPenalty = 2f;
    @ConfigEntry(id = "disableNpcDialogues", type = EntryType.BOOLEAN, translation = T_PREF + "disableNpcDialogues.name")
    public static boolean disableNpcDialogues = false;
    @ConfigEntry(id = "researcherCuredDiscount", type = EntryType.FLOAT, translation = T_PREF + "researcherCuredDiscount.name")
    public static float researcherCuredDiscount = 0.5f;
}
