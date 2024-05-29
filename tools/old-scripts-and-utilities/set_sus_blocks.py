from nbt import nbt
import os

os.chdir(os.path.dirname(__file__))

files = [
    "../../../src/main/resources/data/growsseth/structures/ruins/conduit_ruins.nbt",
    "../../../src/main/resources/data/growsseth/structures/ruins/conduit_church/main.nbt"
]
loot_table = "growsseth:conduit_ruins_archaeology"


for file in files:
    count = 0
    nbtfile = nbt.NBTFile(file)
    for block in nbtfile["blocks"]:
        if "nbt" in block:
            block_entity = block["nbt"]
            if block_entity['id'].value == "minecraft:brushable_block":
                block_entity["LootTable"] = nbt.TAG_String(loot_table)
                count += 1
    nbtfile.write_file()
    print(f"Converted {count} sus blocks")
