import os
from nbt import nbt

# This script adds empty loot tables to the structurs NBTs in order to then set them easily with the Vscode NBT Viewer

# Only for the datapack for now
# structures_folder = "../../Mod Fabric/src/main/resources/data/growsseth/structures/"
structures_folder = "../../Datapack e Resource Pack/Datapack Strutture/data/growsseth/structures/"
total_processed_chests = 0

for path, dirs, files in os.walk(structures_folder):
    for file in files:
        if file.endswith(".nbt"):
            print("Processing", file)
            
            nbtfile = nbt.NBTFile(os.path.join(path, file))
            processed_chests = 0
            
            for block in nbtfile["blocks"]:
                if "nbt" in block:
                    block_entity = block["nbt"]
                    
                    if (block_entity['id'].value == "minecraft:chest" or block_entity['id'].value == "minecraft:barrel") and block_entity.get('LootTable') == None:
                        block_entity["LootTable"] = nbt.TAG_String("minecraft:empty")
                        processed_chests += 1
                        total_processed_chests += 1
                        
            if processed_chests > 0:
                nbtfile.write_file()
                print(f"-> Processed {processed_chests} chests (or barrels)\n")
            else:
                print("-> No chests (or barrels) needed to be processed\n")

print(f"Processed {total_processed_chests} total chests\n")
