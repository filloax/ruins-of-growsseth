package com.ruslan.growsseth.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;

import static com.ruslan.growsseth.config.GrowssethConfig.T_PREF;

@Category("misc")
public class MiscConfig {
    @ConfigEntry(id = "modLootInVanillaStructures", translation = T_PREF + "modLootInVanillaStructures.name")
    @Comment(value = "If changed ingame, takes effect on world reload", translation = T_PREF + "needsWorldReload.comment")
    public static boolean modLootInVanillaStructures = false;

    @ConfigEntry(id = "dialogueWordsPerMinute", translation = T_PREF + "dialogueWordsPerMinute.name")
    @Comment(value = "Reading speed (in words per minute) of chat messages, used for multiline npc dialogues. Increase it to make npcs 'talk' faster, decrease it to make them 'talk' slower. Set it to 0 to receive all messages together.", translation = T_PREF + "dialogueWordsPerMinute.comment")
    public static int dialogueWordsPerMinute = 120;

    @ConfigEntry(id = "zombieGuberSpawnChance", translation = T_PREF + "zombieGuberSpawnChance.name")
    public static float zombieGuberSpawnChance = 0.2f;
}
