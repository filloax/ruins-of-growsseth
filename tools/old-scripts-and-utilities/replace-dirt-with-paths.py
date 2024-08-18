# Little script for substituting dirt blocks with paths in golem streets (not needed anymore since we removed the streets)

from nbt import nbt
import os

os.chdir(os.path.dirname(__file__))

structures_folder = "../../../src/main/resources/data/growsseth/structures/village/"
total_processed_blocks = 0

for path, dirs, files in os.walk(structures_folder):
    for file in files:
        # we want to avoid processing the desert streets, since they don't have dirt or path blocks
        if (file.endswith("street.nbt") or file.endswith("street_zombie.nbt")) and not "desert" in file:
            print("Processing", file)
            
            nbtfile = nbt.NBTFile(os.path.join(path, file))
            processed_blocks = 0
            
            # In the files we want to process the dirt block is associated to state 0, and the dirt path to state 3:
            for block in nbtfile["blocks"]:
                if (block["state"].valuestr() == "0"):
                    block["state"] = nbt.TAG_Int(2)
                    processed_blocks += 1
                    total_processed_blocks += 1
                        
            if processed_blocks > 0:
                nbtfile.write_file()
                print(f"-> Processed {processed_blocks} blocks\n")
            else:
                print("-> No blocks needed to be processed\n")

print(f"Processed {total_processed_blocks} total blocks\n")
