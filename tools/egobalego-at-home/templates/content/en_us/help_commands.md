This is the commands section. The commands are divided into two categories: **manual commands** and **operations**.

**Manual commands** are quite self-explanatory; they allow you to execute any Minecraft command that a player with cheats enabled can run. Simply enter the command you would use in the game (without "/" at the start), and it will run as soon as the mod receives the content from the site.  
<u>Note</u>: Received commands will be executed only once, whether they succeed or not. Also, as they can be potentially dangerous, the mod setting _"remoteCommandExecution"_ needs to be enabled for you to use them.

**Operations** (also called "emergencies") are specific commands of the mod, each performing a specific task:

**Remove researchers** removes <u>ALL</u> researchers that were spawned before the reception of the command, with a one-minute grace period to avoid issues in case more have been spawned in the meantime (for example, by creating a new tent).  
<u>Note</u>: removal occurs when the game actually loads them, so it's better leave the command enabled once created, in order to be safe.

**Teleport researcher** teleports the first researcher that the game loads to the given coordinates, and resets his "starting position" (used to prevent them from wandering off, detect the tent, etc.) to the teleportation point.

**Spawn researcher** spawns a researcher <u>WITHOUT</u> tent at the given coordinates (to spawn a new one with the tent, use the structure section).

**Remove tent** removes the tent at the set coordinates as soon as it's loaded, and the donkey if not borrowed by the player.

**Remove tent and leave gift** removes the researcher, the tent and the donkey (if leashed), and spawns a chest with the goodbye diary and the researcher's horn, without needing the quest. It triggers on the first researcher loaded.
