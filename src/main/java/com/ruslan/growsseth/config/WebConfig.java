package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.types.options.EntryType;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("web")
public final class WebConfig {
    @ConfigEntry(id = "webDataSync", type = EntryType.BOOLEAN, translation = T_PREF + "webDataSync.name")
    @Comment(value = "CAREFUL: check the Wiki on GitHub for how to use this. Enables gamemaster-mode, allowing the " +
            "player you decide to connect to to control the mod's features and more. Requires reloading world.",
            translation = T_PREF + "webDataSync.comment")
    public static boolean webDataSync = false;
    @ConfigEntry(id = "dataSyncUrl", type = EntryType.STRING, translation = T_PREF + "dataSyncUrl.name")
    @Comment(value = "Only connect to trusted sources!",
            translation = T_PREF + "dataSyncUrl.comment")
    public static String dataSyncUrl = "http://localhost:5000";
    @ConfigEntry(id = "dataSyncEndpoint", type = EntryType.STRING, translation = T_PREF + "dataSyncEndpoint.name")
    public static String dataSyncEndpoint = "server_data";
    @ConfigEntry(id = "dataSyncApiKey", type = EntryType.STRING, translation = T_PREF + "dataSyncApiKey.name")
    public static String dataSyncApiKey = "";
    @ConfigEntry(id = "dataSyncReloadTime", type = EntryType.FLOAT, translation = T_PREF + "dataSyncReloadTime.name")
    @Comment(value = "How much time (in minutes) must pass between each server query. Must be at least 10 seconds.", translation = T_PREF + "dataSyncReloadTime.comment")
    public static float dataSyncReloadTime = 1f;

    @ConfigEntry(id = "liveUpdateService", type = EntryType.BOOLEAN, translation = T_PREF + "liveUpdateService.name")
    @Comment(value = "WIP. If changed ingame, takes effect on world reload.", translation = T_PREF + "needsWorldReload.comment")
    public static boolean liveUpdateService = false;
    @ConfigEntry(id = "liveUpdateUrl", type = EntryType.STRING, translation = T_PREF + "liveUpdateUrl.name")
    @Comment(value = "WIP")
    public static String liveUpdateUrl = "";
    @ConfigEntry(id = "liveUpdatePort", type = EntryType.INTEGER, translation = T_PREF + "liveUpdatePort.name")
    @Comment(value = "WIP")
    public static int liveUpdatePort = -1;

    @ConfigEntry(id = "remoteCommandExecution", type = EntryType.BOOLEAN, translation = T_PREF + "remoteCommandExecution.name")
    @Comment(value = "Allow the gamemaster to execute any arbitrary command.", translation = T_PREF + "remoteCommandExecution.comment")
    public static boolean remoteCommandExecution = false;
}
