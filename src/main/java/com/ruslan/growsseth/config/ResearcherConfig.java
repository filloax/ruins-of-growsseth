package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("researcher")
public final class ResearcherConfig {
    @ConfigEntry(id = "singleResearcher", type = EntryType.BOOLEAN, translation = T_PREF + "singleResearcher.name")
    @Comment(
            value = "Share memory between all researcher entities (narratively assume it's the 'same' Researcher moving around)"
                + "which is the case by default."
                + "Will affect dialogues and trades (multiple researchers will always have a random selection of trades that to not change)."
                + "In Growsseth world preset, this will always be true.",
            translation = T_PREF + "singleResearcher.comment"
    )
    public static boolean singleResearcher = true;
    @ConfigEntry(id = "singleResearcherProgressTrades", type = EntryType.BOOLEAN, translation = T_PREF + "singleResearcherRandomTrades.name")
    @Comment(
            value = "True by default, ignored if singleResearcher is false. If enabled, the researcher will initially only sell one kind of map, "
                    + "and unlock more structures as you return to him after finding the previous one. In this mode, the researcher's 'quest' will automatically progress (see quest config section)."
                    + "if disabled, each instance of the researcher will have up to N random maps (see randomTradeMaxMaps) for sale, with no quest progress."
                    + "Always true with a fixed order in Growsseth world preset.",
            translation = T_PREF + "singleResearcherRandomTrades.comment"
    )
    public static boolean singleResearcherProgressTrades = false;
    @ConfigEntry(id = "randomTradeNumMaps", type = EntryType.OBJECT, translation = T_PREF + "randomTradeNumMaps.name")
    @Comment(
            value = "Max map amount the Researcher sells. Used only when singleResearcher is false or singleResearcherProgressTrades is false.",
            translation = T_PREF + "randomTradeNumMaps.comment"
    )
    public static final RangeConfig randomTradeNumMaps = new RangeConfig(2, 2);
    @ConfigEntry(id = "randomTradeNumItems", type = EntryType.OBJECT, translation = T_PREF + "randomTradeNumItems.name")
    @Comment(
            value = "Max misc item amount the Researcher sells. Used only when singleResearcher is false or singleResearcherProgressTrades is false.",
            translation = T_PREF + "randomTradeNumItems.comment"
    )
    public static final RangeConfig randomTradeNumItems = new RangeConfig(2, 2);
    @ConfigEntry(id = "immortalResearcher", type = EntryType.BOOLEAN, translation = T_PREF + "immortalResearcher.name")
    public static boolean immortalResearcher = false;
    @Comment(
        value = "Researcher teleports back to original position, and avoids interacting with nether portals to prevent cheese",
        translation = T_PREF + "researcherTeleports.comment"
    )
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
    @ConfigEntry(id = "researcherBorrowPenalty", type = EntryType.FLOAT, translation = T_PREF + "researcherBorrowPenalty.name")
    public static float researcherBorrowPenalty = 2f;
    @ConfigEntry(id = "disableNpcDialogues", type = EntryType.BOOLEAN, translation = T_PREF + "disableNpcDialogues.name")
    public static boolean disableNpcDialogues = false;
    @ConfigEntry(id = "researcherCuredDiscount", type = EntryType.FLOAT, translation = T_PREF + "researcherCuredDiscount.name")
    public static float researcherCuredDiscount = 0.5f;
}
