This is the communication section, which is divided into three main categories: **notifications**, **researcher dialogues**, and **researcher/structure diaries/books**.

**Notifications** are the simplest: they have a **title**, an **icon** (if the corresponding type is selected), and a **text**. Title and text are self-explanatory, while the icon is indicated by the item ID you want to use preceded by its namespace, so you can also use items from other mods if you wish.  
<u>Note</u>: notifications with an icon have a black background like objectives, while those without have a blue background like chat warnings in unsafe mode.

With **researcher dialogues**, you can decide what you want the researcher to say in chat when approached. For a simple dialogue, just write its content separated into multiple messages with **\\n** (the symbol for line breaks), like in this example:

    Hello colleague, bla bla bla...\nBy the way, have you heard about...

The game automatically decides the duration for which a message blocks the appearance of the next one, but you can manually set it for specific messages if you have a basic understanding of **JSON** syntax.  
To set a duration, you need to separate the messages into a list, and insert the one you want to control into a JSON object with the content and duration in seconds, like this:

    [
        "Hello colleague, can you tell me the time?",
        {
            "content": "§lHmm...§r",
            "duration": 3.0
        }
        "Oh right, I can't read the chat.",
    ]

<u>Note</u>: as seen above, you can use Minecraft formatting codes for italics, bold, etc. (those enclosed the text with §letter). Also, remember that changing the duration of a message does not affect how long it remains on the screen before fading out, but only how long it takes for the next one to appear!

Finally, we come to the category of **diaries/books**, which includes three different actions:

*   Having the researcher write a **new diary** when the player approaches;
*   **Replacing one of the diaries** that the researcher writes when approached after exploring a structure (those about his adventures in the respective structure);
*   **Replacing the farewell diary** that the researcher writes when leaving once the quest is completed (the one found in the crate with the horn);
*   **Replacing one of the books** found in the structures.

In the latter two cases, in addition to the title, author (only for structure books), and content, you will also need to choose which structure you are referring to.

The content consists of the pages, for which you can use **\\n** for line breaks and **%PAGEBREAK%** to go to the next page, like in this example:

    Dear diary.\n\nToday I saw a creeper.%PAGEBREAK%It was very §green§r.",

<u>Note</u>: As with dialogues, you can use the game's formatting rules.
