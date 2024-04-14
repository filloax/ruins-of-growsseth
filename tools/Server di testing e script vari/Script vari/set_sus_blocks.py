from nbt import nbt
import os

os.chdir(os.path.dirname(__file__))

# This script is deprecated because the new conduit ruins have special characters in the signs, use the notebook instead

file = "../../Mod Fabric/src/main/resources/data/growsseth/structures/ruins/conduit_ruins/main.nbt"
loot_table = "growsseth:conduit_ruins_archaeology"

nbtfile = nbt.NBTFile(file)

count = 0

for block in nbtfile["blocks"]:
    if "nbt" in block:
        block_entity = block["nbt"]
        if block_entity['id'].value == "minecraft:brushable_block":
            block_entity["LootTable"] = nbt.TAG_String(loot_table)
            count += 1

nbtfile.write_file()

print(f"Converted {count} sus blocks")
