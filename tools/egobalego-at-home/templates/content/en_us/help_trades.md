This is the section for trades, which are divided into two categories: **preset** and **custom**.

**Preset** trades are prepackaged trades present within the mod's data, ready for activation without needing to set anything. To see exactly what they do or modify them, you can find them in the researcher's trade folder of the datapack (data/growsseth/growsseth_researcher_trades).

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

In **"gives"** goes the item you want to give with the trade (see below for item format), while in **"wants"** go the items you want the researcher to ask for in return (maximum two, like vanilla villagers). The following fields are optional:

*   **"priority"** indicates how high the new trade should go in the researcher's menu (all trades are sorted by this value; it's advisable to study the datapack to understand it);
*   **"noNotification"** (if set to _true_) doesn't show the notification in-game when the trade is created;
*   **"replace"** (if set to _true_) replaces other trades with lower priority that offer the same item;
*   **"maxUses"** indicates the maximum number of times the item of the trade can be purchased.

In place of the comments with "<u>TradeItemEntry</u>", you need to insert a JSON object with this format:

    {
        "id": "minecraft:stick",
        "amount": 5,
    }

In **"id"** goes the item ID you want to give with the trade, while in **"amount"** goes the quantity offered (if the item is in "gives") or requested (if the item is in "wants").

<u>Note 1</u>: if the item sold is a treasure map (for vanilla or mod structures), you need to add to the item values an "map" element with this format:

        "map": {
            "name": "Map for the stronghold",
            // CHOOSE:
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

In **"name"** goes the title of the map, then you need to leave one (two for coordinates) of the following elements (and **remove the others**):

*   **"structure"** points the map to the structure spawned **naturally** with the indicated ID (the nearest one is taken);
*   **"fixedStructure"** works like structure but for structures spawned **via website**;
*   **"x"** and **"z"** point the map to **specific coordinates**.

The following fields are optional:

*   **"description"** is the description of the item (what appears below the name when you hover over it) and can consist of multiple lines (if you want to use a single line, you can also use a string instead of the list);
*   **"scale"** is the map zoom, the higher the value, the wider the area shown. It ranges from 1 to 4 like vanilla maps.

<u>Note 2</u>: if you want to sell one of the **researcher's diaries** present in the mod, you need to add to the item values this element:

        "diaryId": "enchantment_dictionary"

In **"diaryID"** simply goes the ID of the diary in the mod's data. To see which ones are available or add your own, you can look at the datapack (data/growsseth/growsseth_researcher_diary/it_it).

To conclude, here's an example of a trade where 15 TNT is offered for 3 sticks and 10 carrots:

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

<u>Note</u>: researcher trades (unlike other events that are triggered only once) are loaded into the game every time the mod receives data from the site, so once activated, they shouldn't be touched until you decide to modify or deactivate them.
