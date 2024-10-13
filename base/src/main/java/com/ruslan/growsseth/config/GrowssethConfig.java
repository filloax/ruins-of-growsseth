package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

@Config(
    value = "ruins-of-growsseth",
    categories = {
        MiscConfig.class,
        ClientConfig.class,
        ResearcherConfig.class,
        WebConfig.class,
        QuestConfig.class,
        StructureConfig.class,
        WorldPresetConfig.class,
        DebugConfig.class
    }
)
@ConfigInfo(
        icon = "wrench",
        title = "Ruins of Growsseth",
        description = "A highly configurable structures mod to guide towards vanilla features",
        descriptionTranslation = "growsseth.config.web.description",
        links = {
                @ConfigInfo.Link(
                        value = "https://modrinth.com/mod/ruins-of-growsseth",
                        icon = "modrinth",
                        text = "Modrinth"
                ),
                @ConfigInfo.Link(
                        value = "https://www.curseforge.com/minecraft/mc-mods/ruins-of-growsseth",
                        icon = "curseforge",
                        text = "Curseforge"
                ),
                @ConfigInfo.Link(
                        value = "https://github.com/filloax/ruins-of-growsseth",
                        icon = "github",
                        text = "Github"
                )
        }
)
public final class GrowssethConfig {
    public static final String T_PREF = "growsseth.config.";

    @ConfigEntry(id = "serverLanguage", translation = T_PREF + "serverLanguage.name")
    @Comment(
            // Different from the translated string, since the 'client' info is needed only for those who edit the json directly
            value = "Choose between 'en_us' and 'it_it'. Used for npc dialogues, researcher diaries and structure books. "
                + "Defaults to 'client', which will autoselect the language depending on client language on first launch, or default to en_us in servers. "
                + "Will not affect already generated structures. "
                + "Additional languages may be added via datapack.",
            translation = T_PREF + "serverLanguage.comment"
    )
    public static String serverLanguage = "client";
}