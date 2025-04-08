import os
from pathlib import Path
from nbt import nbt
import shutil

# Config constants
VILLAGE_STRUCTURE_PATH = Path(__file__).resolve().parent / "../../../src/main/resources/data/growsseth/structures/village"
DIRT_STATE_ID = 0         # the block state ID corresponding to dirt
DIRT_PATH_STATE_ID = 2    # the block state ID corresponding to dirt path (corrected per your comment, was 3)
DRY_RUN = False           # set True to preview changes only
MAKE_BACKUP = True        # backup original NBT files before overwrite
VERBOSE = True

total_processed_blocks = 0
total_files_modified = 0

for file_path in VILLAGE_STRUCTURE_PATH.rglob("*.nbt"):
    if ("street.nbt" in file_path.name or "street_zombie.nbt" in file_path.name) and "desert" not in file_path.name:
        if VERBOSE:
            print(f"Processing '{file_path}'...")
        try:
            nbtfile = nbt.NBTFile(str(file_path))
        except Exception as e:
            print(f"Failed to load NBT file '{file_path}': {e}")
            continue
        
        processed_blocks = 0
        for block in nbtfile["blocks"]:
            if block["state"].valuestr() == str(DIRT_STATE_ID):
                block["state"] = nbt.TAG_Int(DIRT_PATH_STATE_ID)
                processed_blocks += 1
                total_processed_blocks += 1
        
        if processed_blocks > 0:
            total_files_modified += 1
            if MAKE_BACKUP and not DRY_RUN:
                shutil.copy(file_path, file_path.with_suffix(".nbt.bak"))
            if not DRY_RUN:
                try:
                    nbtfile.write_file(str(file_path))
                    print(f"  -> Converted {processed_blocks} dirt blocks to paths.")
                except Exception as e:
                    print(f"Failed to write updated NBT file '{file_path}': {e}")
            else:
                print(f"  (Dry run) Would convert {processed_blocks} blocks.")
        else:
            if VERBOSE:
                print("  -> No dirt blocks to convert.")

print(f"\nTotal blocks processed: {total_processed_blocks}")
print(f"Total files modified: {total_files_modified}")
print(f"Dry run mode: {DRY_RUN}")
