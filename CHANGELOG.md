# Changelog

## 0.11.0

Added
- Common tags convention for structure spawns, making the mod compatible with other biome mods
- Dialogues and diaries variations for players who complete the main quest without meeting the Researcher, changed dialogue for another situation
- [Data] `groups` and `groupUseLimit` fields for dialogues data, allowing to make dialogues mutually exclusive
- [Data] Templates now support signs and hanging signs
- Researcher now changes expression when aggressive
- Sound for music disc "Ancora Qui"
- [1.21] 1.21.1 support

Tweaked
- The Conduit Church structure now can spawn with signs in both italian and english, making the mod fully localized
- The English translation has been improved (it's no longer wonky)
- The Conduit Church has been updated to also spawn on deep lukewarm oceans
- Golem houses no longer need custom streets to spawn, and instead spawn from vanilla ones, improving compatibility with other mods
- If a player stole the researcher's work tools, he will accuse only them when trying to trade instead of everyone
- Creeper's explosions inside researcher tents are now harmless even to entities, to avoid breaking the donkey's leash
- [Data] Researcher's loot table is now editable by datapack
- Researcher tries to get out from walls when suffocating

Fixed
- Various bugs related to researcher's dialogue and persistence introduced in version 0.10 have been fixed
- Fixed researcher behavior for players stealing his work tools, now he gets angry only when left without lecterns or cartography tables
- Researcher's type skin and sounds are now separated from vanilla, to avoid them breaking when resource packs override them
- The researcher can roam even if spectator players are nearby, and won't talk to them
- `dialogueWordsPerMinute` option now also skips dialogues with specified duration
- [1.21] fixed researcher being leashable


## 0.10.1

Added
- Sound for music disc "Il Tesoro di Caco Caco"

Tweaked
- `/glocate stop` subcommand to stop ongoing glocate runs you started
- Maps for village houses and glocate for jigsaws lead to the exact
    position of the house/structure part in question
- When F3 + H is on, maps show the position of the target in the tooltip

Fixed
- [1.21] Decorated pots crashing


## 0.10.0

Added
- Add all remaining discs except CacoCaco and Ancora Qui
- Advancement for when you find all structures
- Ruins map now have their target icon shown on the GUI map icon
- Researcher: new dialogue event when finding new tent for the first time
- `/gmaster` command to tweak gamemaster website connections

Tweaked
- Ballata disc fragments now found in End Cities
- Add more structure voids to various structures
- Reduced armor trim spawn chance in forge
- Wither skull not guaranteed in forge
- Researcher climbs on top of Powder Snow

Fixed
- Fix some dialogues and messages
- Fix researcher sync when two far away tents are loaded at the same time because of high simulation distance


## 0.9.0

Initial Beta release.
