This is the communication section, which is divided into three main categories: **notifications**, **researcher dialogues**, and **researcher/structure diaries/books**.

**Notifications** are the simplest to understand: they have a **title**, an **icon** (if the corresponding type is selected), and a **text**. Title and text are self-explanatory, while the icon is specified by the item ID you want to use preceded by its namespace (you can also use items from other mods if you wish).  
<u>Note</u>: notifications with an icon have a black background like the advancements, while those without it have a blue background like the chat warnings in unsafe mode.

With **researcher dialogues**, you can decide what you want the researcher to say in chat when approached (or when the data gets synched if he's already near the player). For a simple dialogue, write its content and split it into multiple messages with **\n** (the symbol for line breaks), like in this example:

    Hello colleague, blah blah blah...\nBy the way, have you heard about...

The game automatically decides the duration for which a message blocks the next one from appearing, but you can manually set it for specific messages if you have a basic understanding of **JSON** syntax.  
To set a duration, you need to insert the messages into a list, and put the one you want to control inside a JSON object with the content and duration in seconds, like this:

    [
        "Hello colleague, can you tell me the time?",
        {
            "content": "§lHmm...§r",
            "duration": 3.0
        }
        "Oh right, I can't read the chat.",
    ]

<u>Note</u>: as seen above, you can use Minecraft formatting codes for italics, bold, etc. (those enclosing the text with §letter). Also, remember that changing the duration of a message does not affect how long it remains on the screen before fading out, but only how long it takes for the next one to appear!

Finally, we reach the category of **diaries/books**, which includes three different actions:

*   Having the researcher write a **new diary** when the player approaches him;
*   **Replacing one of the diaries** that the researcher writes when approached after the player explores a structure (those about his adventures in the respective structure);
*   **Replacing the farewell diary** that the researcher writes when leaving once the quest is completed (the one found in the chest with the goat horn gift);
*   **Replacing one of the books** found in the mod's structures.

In the latter two cases, in addition to the title, author (only for structure books), and content, you will also need to choose which structure you are referring to.

The content is that of the pages, for which you can use **\n** for line breaks and **%PAGEBREAK%** to go to the next page, like in this example:

    Dear diary.\n\nToday I saw a creeper.%PAGEBREAK%It was very §ogreen§r.",

<u>Note</u>: As with dialogues, you can use the game's formatting rules.
