from nbt_structure_utils import NBTStructure, Vector, Cuboid, BlockData
from nbt_structure_utils.nbt_structure import EMPTY_SPACE
from nbt.nbt import TAG_Compound, TAG_Byte, TAG_List, TAG_String
from nbt import nbt
import os
import json

thisdir = os.path.dirname(__file__)
rootdir = os.path.abspath(os.path.join(thisdir, os.pardir, os.pardir))
structdir = os.path.join(rootdir, "Mod Fabric", "src", "main", "resources", "data", "growsseth", "structures")
outdir    = os.path.join(rootdir, "Mod Fabric", "src", "main", "resources", "data", "growsseth", "growsseth_structure_books", "it_it")

done_per_file = {}
converted = set()

def get_id(file: str):
    global done_per_file
    
    num = done_per_file.get(file, 0)
    done_per_file[file] = num + 1
    
    path_rel = os.path.relpath(file, structdir)
    name = '_'.join(path_rel.split(os.sep)[1:]).replace(".nbt", "")
        
    return f'{name}_{num}'

def page_text(page: TAG_String) -> str:
    value = page.value
    try:
        page_data = json.loads(value)
        return page_data["text"]
    except:
        return value

def conv_item(file: str, item_data: TAG_Compound):
    global converted
    if item_data["id"].value != "minecraft:written_book" and item_data["id"].value != "minecraft:writable_book":
        return item_data
    
    if "tag" not in item_data:
        item_data["tag"] = TAG_Compound()
                
    if "pages" in item_data["tag"]:
        og_pages = item_data["tag"]["pages"]
    else:
        return

    id = get_id(file)
    
    outfile = os.path.join(outdir, f"{id}.json")
    with open(outfile, 'w', encoding='utf-8') as f:
        out_data = {
            'pages': [page_text(page) for page in og_pages]
        }
        if "title" in item_data["tag"]:
            out_data['name'] = item_data["tag"]["title"].value
        if "author" in item_data["tag"]:
            out_data['author'] = item_data["tag"]["author"].value
        if item_data["id"].value == "minecraft:writable_book":
            out_data['writable'] = TAG_Byte(True)
        json.dump(out_data, f, indent=4, ensure_ascii=False)
        print(f"Saved {os.path.relpath(outfile, rootdir)}")

    pages = TAG_List(type=TAG_String, name="pages")

    pages.insert(0, TAG_String(f"%%TEMPLATE%%\n{id}"))
        
    item_data["tag"]["pages"] = pages
    
    converted.add(file)
    
    return item_data

def conv_tileentity(file: str, nbt_data: TAG_Compound):
    if "Book" in nbt_data:
        nbt_data["Book"] = conv_item(file, nbt_data["Book"])
    elif "Items" in nbt_data:
        for i, item in enumerate(nbt_data["Items"]):
            nbt_data["Items"][i] = conv_item(file, item)

for root, subdirs, files in os.walk(structdir):
    for file in files:
        if file.endswith("_edit.nbt"):
            continue
        # print(f"Handling {file}...")
        filepath = os.path.join(root, file)
        nbtfile = nbt.NBTFile(filepath)
        # out_file = filepath.replace(".nbt", "_edit.nbt")
        
        blocks = nbtfile["blocks"]
        
        for pos in blocks:
            if "nbt" in pos:
                nbt_data = pos["nbt"]
                if "Book" in nbt_data or "Items" in nbt_data:
                    conv_tileentity(filepath, nbt_data)
                           
        if filepath in converted:
            nbtfile.write_file()
