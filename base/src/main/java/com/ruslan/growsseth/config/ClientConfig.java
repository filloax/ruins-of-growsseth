package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("client")
public class ClientConfig {
    @ConfigEntry(id = "enableLocationTitles", translation = T_PREF + "enableLocationTitles.name")
    @Comment(
            value = "Shows the title of the places you are visiting when exploring the Growsseth world preset.",
            translation = T_PREF + "enableLocationTitles.comment"
    )
    public static boolean enableLocationTitles = true;

    @ConfigEntry(id = "locationTitlesMode", translation = T_PREF + "locationTitlesMode.name")
    public static TitleMode locationTitlesMode = TitleMode.TITLE;

    @ConfigEntry(id = "newTradeNotifications", translation = T_PREF + "newTradeNotifications.name")
    @Comment(
            value = "Shows a small toast when the researcher unlocks new trades. Works only for web and progress trades.",
            translation = T_PREF + "newTradeNotifications.comment"
    )
    public static boolean newTradeNotifications = true;

    @ConfigEntry(id = "disableNpcDialogues", translation = T_PREF + "disableNpcDialogues.name")
    public static boolean disableNpcDialogues = false;

    @ConfigEntry(id = "mapCornerIcons", translation = T_PREF + "mapCornerIcons.name")
    public static boolean mapCornerIcons = true;
}