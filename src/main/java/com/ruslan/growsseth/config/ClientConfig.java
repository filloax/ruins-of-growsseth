package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("client")
public class ClientConfig {
    @Comment(value = "Enable location titles shown in Growsseth world preset", translation = T_PREF + ".enableLocationTitles.comment")
    @ConfigEntry(id = "enableLocationTitles", type = EntryType.BOOLEAN, translation = T_PREF + ".enableLocationTitles.name")
    public static boolean enableLocationTitles = true;

    @ConfigEntry(id = "newTradeNotifications", type = EntryType.BOOLEAN, translation = T_PREF + "newTradeNotifications.name")
    public static boolean newTradeNotifications = true;
}
