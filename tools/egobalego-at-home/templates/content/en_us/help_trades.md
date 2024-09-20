This is the section for the researcher's trades, which are divided into two categories: **preset** and **custom**.

**Preset** trades are prepackaged trades present within the mod's data, ready for activation without having to set anything. To see exactly what they are or edit them, you can find them in the researcher's trade folder of the datapack (_data/growsseth/growsseth_researcher_trades_). They internally follow the same format as the custom trades.

**Custom** trades are freely configurable trades. They offer much more freedom than preset ones but require knowledge of JSON syntax.  
They follow this template:

    {
        "gives": //TradeItemEntry,
        "wants": [
            //TradeItemEntry1,
            //TradeItemEntry2 (optional)
        ],
        // OPTIONAL:
        "priority": 0,
        "noNotification": false,
        "replace": false,
        "maxUses": 1
    }

**"gives"** specifies the item that will be sold (see below for its format), while **"wants"** contains the items you want the researcher to ask for in return (two items max, like for vanilla villagers). The following fields are optional:

*   **"priority"** indicates how high the new trade should appear in the researcher's trade interface (all trades are sorted by this value; it's advisable to study the datapack to understand it);
*   **"noNotification"** (if set to _true_) prevents the in-game notification from appearing when the trade is created;
*   **"replace"** (if set to _true_) makes the trade replace others that are offering the same item but have a lower priority;
*   **"maxUses"** indicates how many times the item in "gives" can be bought before the trade is locked.

In place of the comments with "<u>TradeItemEntry</u>", you need to insert a JSON object with this format:

    {
        "id": "minecraft:stick",
        "amount": 5,
    }

**"id"** is the ID of the item, while **"amount"** is the quantity offered (if the item is in "gives") or requested (if the item is in "wants").

<u>Note 1</u>: if the item sold is a treasure map (for vanilla or modded structures), you need to add to the item entries a "map" element with this format:

        "map": {
            "name": "Map for something",
            // CHOOSE BETWEEN:
            "structure": "minecraft:stronghold",
            "fixedStructure": "growsseth:cave_camp",
            "x": 0,
            "z": 0,
            // OPTIONAL:
            "description": [
                "Line 1",
                "Line 2",
            ],
            "scale": 3,
        },

**"name"** is the title of the map, then you need to leave one (two if you use the coordinates) of the following elements (and **remove the others**):

*   **"structure"** points the map to a structure spawned **naturally** that has the specified ID (the nearest one is taken);
*   **"fixedStructure"** works like "structure" but for structures spawned **by the gamemaster**;
*   **"x"** and **"z"** point the map to **specific coordinates**.

The following fields are optional:

*   **"description"** is the description of the item (what appears under the item's name when you hover the mouse over it) and can consist of multiple lines (if you want to use a single line, you can also use a string instead of the list in the example);
*   **"scale"** is the map zoom, the higher the value, the wider the area shown. It ranges from 1 to 4 like for vanilla maps.

<u>Note 2</u>: if you want to sell one of the **researcher's diaries** available in the mod, you need to add this element to the item entries:

        "diaryId": "enchantment_dictionary"

In **"diaryID"** goes the ID of the diary in the mod's data. To see which ones are available or add your own, you can look at the datapack (_data/growsseth/growsseth_researcher_diary/en_us_).

To conclude, here's an example of a trade where 15 TNTs are offered for 3 sticks and 10 carrots:

    {
        "gives":{
            "id":"minecraft:tnt",
            "amount":15
        },
        "wants":[
            {
                "id":"minecraft:stick",
                "amount":3
            },
            {
                "id":"minecraft:carrot",
                "amount":10
            }
        ],
        "priority":100
    }

<u>Note</u>: researcher trades (unlike the other events, which are triggered only once) are loaded into the game every time the mod receives data from the site, once activated they shouldn't be touched until you decide to edit or deactivate them.
