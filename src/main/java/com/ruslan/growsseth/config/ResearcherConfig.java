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
            value = "Shares memory between all researcher entities (narratively assume it's the 'same' Researcher moving around)."
                + "Applies on first spawn or with the command '/greset'."
                + "Will affect dialogues and trades (multiple researchers will always have a random selection of trades that do not change)."
                + "In Growsseth world preset this will always be true.",
            translation = T_PREF + "singleResearcher.comment"
    )
    public static boolean singleResearcher = true;

    @ConfigEntry(id = "singleResearcherProgress", type = EntryType.BOOLEAN, translation = T_PREF + "singleResearcherProgress.name")
    @Comment(
            value = "Ignored if single researcher is false. If enabled, the researcher will initially only sell one kind of map, "
                    + "and unlock more structures as you return to him after finding the previous one.\nIn this mode, the researcher's 'quest' will automatically progress (see quest config section)."
                    + "If disabled, each instance of the researcher will have up to N random maps (see randomTradeMaxMaps) for sale, with no quest progress. "
                    + "Always true with a fixed order in Growsseth world preset.",
            translation = T_PREF + "singleResearcherProgress.comment"
    )
    public static boolean singleResearcherProgress = true;

    @ConfigEntry(id = "immortalResearcher", type = EntryType.BOOLEAN, translation = T_PREF + "immortalResearcher.name")
    @Comment(
            value = "Applies Resistance V and Regeneration V to the researcher, and prevents him from fighting the player.",
            translation = T_PREF + "immortalResearcher.comment"
    )
    public static boolean immortalResearcher = false;

    @ConfigEntry(id = "researcherWritesDiaries", type = EntryType.BOOLEAN, translation = T_PREF + "researcherWritesDiaries.name")
    @Comment(
            value = "When the player visits a structure of the mod the researcher will generate a diary " +
                    "related to it in the tent's lectern. If another diary is on the lectern it will be moved " +
                    "to the tent chest, and even if that is full it will drop on the ground. Does not work if single researcher is off.",
            translation = T_PREF + "researcherWritesDiaries.comment"
    )
    public static boolean researcherWritesDiaries = true;

    @ConfigEntry(id = "researcherBorrowPenalty", type = EntryType.FLOAT, translation = T_PREF + "researcherBorrowPenalty.name")
    @Comment(
            value = "Trade price multiplier for borrowing the researcher's donkey. Only affects emeralds.",
            translation = T_PREF + "researcherBorrowPenalty.comment"
    )
    public static float researcherBorrowPenalty = 2f;

    @ConfigEntry(id = "researcherTeleports", type = EntryType.BOOLEAN, translation = T_PREF + "researcherTeleports.name")
    @Comment(
            value = "The researcher teleports back to his original position after five minutes away from it, and avoids interacting with Nether and End portals to prevent cheese.",
            translation = T_PREF + "researcherTeleports.comment"
    )
    public static boolean researcherTeleports = true;

    @ConfigEntry(id = "researcherAntiCheat", type = EntryType.BOOLEAN, translation = T_PREF + "researcherAntiCheat.name")
    @Comment(
            value = "If the player tries to cheat while fighting the researcher he will drink a turtle master potion and enable a passive regen to prevent him from dying.",
            translation = T_PREF + "researcherAntiCheat.comment"
    )
    public static boolean researcherAntiCheat = true;

    @ConfigEntry(id = "researcherInteractsWithMobs", type = EntryType.BOOLEAN, translation = T_PREF + "researcherInteractsWithMobs.name")
    @Comment(
            value = "Zombies, skeletons, raiders and vexes will be hostile towards the researcher, and he will react to them and other mobs who attack him. If changed ingame, takes effect on world reload.",
            translation = T_PREF + "researcherInteractsWithMobs.comment"
    )
    public static boolean researcherInteractsWithMobs = true;

    @ConfigEntry(id = "researcherStrikesFirst", type = EntryType.BOOLEAN, translation = T_PREF + "researcherStrikesFirst.name")
    @Comment(
            value = "The researcher will attack the mobs mentioned in the previous setting (if enabled) even when they are not attacking him. If changed ingame, takes effect on world reload.",
            translation = T_PREF + "researcherStrikesFirst.comment"
    )
    public static boolean researcherStrikesFirst = false;

    @ConfigEntry(id = "webTrades", type = EntryType.BOOLEAN, translation = T_PREF + "webTrades.name")
    @Comment(
            value = "Whether to apply gamemaster-controlled trades when web data sync is enabled.",
            translation = T_PREF + "webTrades.comment"
    )
    public static boolean webTrades = true;

    @ConfigEntry(id = "tradesRestockTime", type = EntryType.FLOAT, translation = T_PREF + "tradesRestockTime.name")
    @Comment(
            value = "If >0, the time (in fraction of ingame days) it takes for the researcher to refresh his trade uses.",
            translation = T_PREF + "tradesRestockTime.comment"
    )
    public static float tradesRestockTime = 1;

    @ConfigEntry(id = "randomTradeNumMaps", type = EntryType.OBJECT, translation = T_PREF + "randomTradeNumMaps.name")
    @Comment(
            value = "Max map amount the researcher sells. Used only when single researcher (or trade progress for single researcher) is disabled.",
            translation = T_PREF + "randomTradeNumMaps.comment"
    )
    public static final RangeConfig randomTradeNumMaps = new RangeConfig(2, 2);

    @ConfigEntry(id = "randomTradeNumItems", type = EntryType.OBJECT, translation = T_PREF + "randomTradeNumItems.name")
    @Comment(
            value = "Max misc item amount the researcher sells. Used for random trades included in all trade modes except web (limited by progress for progress modes).",
            translation = T_PREF + "randomTradeNumItems.comment"
    )
    public static final RangeConfig randomTradeNumItems = new RangeConfig(2, 2);

    @ConfigEntry(id = "randomTradesRefreshTime", type = EntryType.FLOAT, translation = T_PREF + "randomTradesRefreshTime.name")
    @Comment(
            value = "If >0, the time (in fraction of ingame days) it takes for the researcher to change his random trades when single researcher (or trade progress for single researcher) is disabled.",
            translation = T_PREF + "randomTradesRefreshTime.comment"
    )
    public static float randomTradesRefreshTime = 0;
}
