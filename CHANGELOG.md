# Changelog

## CURRENT

## 0.13.1

Tweaked
- Creepers won't explode inside the Researcher's tent, instead of exploding without dealing damage (configurable)
- Books and signs templates won't be loaded when placing structures with structure blocks, to allow easier editing in dev

Fixed
- The Researcher will now stop strolling when players approach him
- All goodbye dialogues are now immediate, to always trigger even if there is still a delay from a previous message
- Fixed rare bug that made the researcher think that players stole his work tools (but wasn't true)
- Fixed some dynamic dialogues triggering at the wrong quest stages
- `givestructmap` command now works again

## 0.13.0

Added
- Researcher now keeps diaries even without Single Researcher mode on: each researcher will only
show diaries for its own structures, and only after you find them *after* he sold the map to you
(they won't count times you entered the structure before)

Changed
- Dialogues now are interrupted if you leave the researcher mid-dialogue, with a special
interruption dialogue
- Internal change of dialogue structure, dialogue conditions (like quest stages requiring
dialogues, event counts, etc.) are now triggered at completion of dialogue, and not start
- Removed immediate property from dialogues, as it now depends on the events
- Final quest step timer changed to 2 days

## 0.12.1

Added
- `gquest <researcher|@e> info triggers [(TICK|LOAD)]` command to show the status of the next stage's quest triggers, used internally for debugging

Fixed
- Golem houses not having maps in Growsseth world preset
- Researcher dialogue triggers vs dead players (especially the "player returns after getting killed by researcher" dialogue)
- `glocate` for fixed structure placement (ie Growsseth Preset or Gamemaster mode)
- Quest: try avoiding a zombie duplication issue
- Quest: fix final stages not properly triggering
- structure debug option crashing the game

## 0.12.0

Added
- [1.21] Neoforge support! Many changes have been made under the hood, so if you encounter any issues (even in the Fabric version), please let us know!
    - Neoforge version has feature parity, only missing thing is the "live connection" part of the Gamemaster mode due to dependency issues
- `gdata` command, used to convert dialogue and places files to the new format (see below + wiki for details)

Tweaked
- [1.21] Since the development tools for 1.21.0 are not supported anymore, future versions of the mod (this one included) will only be on version 1.21.1 (or higher)
- Dialogues now store their text in separate data entries, still server-side to sync the dialogue timing for all players
- `gquest` command can now list all the quest stages available for the selected entity
- Growsseth preset locations titles are now localized


## 0.11.7

Fixed
- [1.21] Fixed our village houses not spawning when the Lithostitched library is installed
- Fixed the "Ruins of Growsseth" prefix not appearing inside logs from our mod


## 0.11.6

Fixed
- [1.21] Fixed crash with mods using the `spawn_condition` feature of the Lithostitched library

Note: the village houses from our mod will stop spawning if Lithostitched is loaded (MC 1.21 only), we are working on a fix


## 0.11.5

Tweaked
- Slightly increased compression for music discs audio, significantly reducing the size of the mod!


## 0.11.4

Fixed
- Fixed an issue where the final part of the researcher's quest would not trigger


## 0.11.3

Fixed
- Resolved crash when playing with mods that add entities able to place blocks


## 0.11.2

Added
- [Web Console] The Live Update service (websocket connection) is now manageable from Egobalego at Home and fully supported by the mod
- [Web Console] Added a simple update checker to be notified when a new version of the webapp is available
- New command `/gtemplate`, replaces `/booktemplates` and supports sign templates
- Added a note in the World Creation Tab specifying that the "Growsseth" preset may not generate the exact same world as in the Twitch series

Fixed
- [Web Console] fixed a bug introduced with the last release that prevented the last_id.txt file from being loaded correctly (if you are using that version, please update!)
- Fixed simple notifications appearing in-game incorrectly as custom ones (but without an icon)
- Fixed the Live Update service not closing newly created sockets after failing to connect


## 0.11.1

Added
- Right-clicking on the researcher mid-dialogue now skips the current chat message
- [Web Console] Added dark theme to Egobalego at Home (download it from the GitHub releases!)

Tweaked
- The first advancement now requires interacting with the researcher directly, instead of just reaching his tent
- Improved terrain adaptation for most of the structures, should fix the instances where some structures would spawn suspended in midair
- Added logging to painting placement fixes during structure generation, to prevent confusion when using mods that fix the same problem

Fixed
- Fixed an instance of the researcher's tent notes not using templates (they always appeared in italian)
- Lowered the water level of the conduit church nbt, to avoid the appearance of water blobs when it spawns too high up
- Corrected the durability values for certain items found in the enchanting tower and conduit church


## 0.11.0

Added
- Common tags convention for structure spawns, making the mod compatible with other biome mods
- Added dialogue and diary variations for players who complete the main quest without meeting the Researcher
- [Data] `groups` and `groupUseLimit` fields for dialogues data, allowing to make dialogues mutually exclusive
- [Data] Templates now support signs and hanging signs
- Researcher now changes expression when becoming aggressive
- Sound for music disc "Ancora Qui"
- Subtitles for mod sounds
- [1.21] 1.21.1 support

Tweaked
- The Conduit Church structure can now spawn with signs in both italian and english, making the mod fully localized
- The English translation has been improved (it's no longer wonky)
- The Conduit Church has been updated to also spawn on deep lukewarm oceans
- Golem houses no longer need custom streets to spawn (they instead use vanilla ones), improving compatibility with other mods
- If a player steals the researcher's tools, he will accuse only that player when trying to trade, instead of everyone
- Creeper's explosions inside researcher tents are now harmless even to entities, to avoid breaking the donkey's leash
- The researcher will try to get out from walls when suffocating, and if not possible he will teleport back to the tent (if tp is enabled)
- The researcher will not take any damage from suffocation if below 50% health and anti-cheat is enabled
- [Data] Researcher's loot table is now editable by datapack

Fixed
- Various bugs related to researcher's dialogue and persistence introduced in version 0.10 have been fixed
- Fixed researcher behavior when players steal his tools. He now only gets angry when left without lecterns or cartography tables
- The researcher's skin type and sounds are now separated from vanilla to prevent issues when resource packs override them
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
