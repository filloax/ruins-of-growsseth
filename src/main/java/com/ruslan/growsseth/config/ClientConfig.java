package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("client")
public class ClientConfig {
    @ConfigEntry(id = "enableLocationTitles", type = EntryType.BOOLEAN, translation = T_PREF + "enableLocationTitles.name")
    @Comment(
            value = "Shows the title of the places you are visiting when exploring the Growsseth world preset.",
            translation = T_PREF + "enableLocationTitles.comment"
    )
    public static boolean enableLocationTitles = true;

    @ConfigEntry(id = "locationTitlesMode", type = EntryType.ENUM, translation = T_PREF + "locationTitlesMode.name")
    public static TitleMode locationTitlesMode = TitleMode.TITLE;

    @ConfigEntry(id = "newTradeNotifications", type = EntryType.BOOLEAN, translation = T_PREF + "newTradeNotifications.name")
    @Comment(
            value = "Shows a small toast when the researcher unlocks new trades. Works only for web and progress trades.",
            translation = T_PREF + "newTradeNotifications.comment"
    )
    public static boolean newTradeNotifications = true;

    @ConfigEntry(id = "disableNpcDialogues", type = EntryType.BOOLEAN, translation = T_PREF + "disableNpcDialogues.name")
    public static boolean disableNpcDialogues = false;
}
