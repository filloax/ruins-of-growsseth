"""DEPRECATED
Now uses data directly in mod"""

import json
import os

def main():
    scriptdir = os.path.abspath(os.path.dirname(__file__))
    targdir = os.path.abspath(os.path.join(scriptdir, os.path.pardir, "Mod Fabric/src/main/resources/assets/growsseth/lang"))
    os.makedirs(targdir, exist_ok=True)

    diary_dir = os.path.join(scriptdir, "data", "diary")
    langs = list(filter(lambda f: os.path.isdir(os.path.join(diary_dir, f)), os.listdir(diary_dir)))

    data = {}

    for lang in langs:
        lang_dir = os.path.join(diary_dir, lang)
        data[lang] = {}
        for struct_file in os.listdir(lang_dir):
            with open(os.path.join(lang_dir, struct_file), 'r', encoding='UTF-8') as f:
                conv = []
                this_data = json.load(f)

            for page in this_data["pages"]:
                if type(page) is str:
                    conv.append(page)
                else:
                    conv.append('\n'.join(page))

            data[lang][struct_file.replace(".json", "")] = conv

    for lang in data:
        langf = os.path.join(targdir, f"{lang}.json")
        langdata = {}
        if os.path.exists(langf):
            with open(langf, 'r', encoding='UTF-8') as f:
                langdata = json.load(f)

        str_pages = data[lang]
        for struct in str_pages:
            for key, value in getentries(struct, str_pages[struct]):
                langdata[key] = value

        with open(langf, 'w', encoding='UTF-8') as f:
            json.dump(langdata, f, indent=4, ensure_ascii=False)
            print(f"Dumped {lang}")
        
def getentries(structName: str, pagestrings: list[str]):
    for i, page in enumerate(pagestrings):
        yield (f"growsseth.diary.growsseth.{structName}.page{i}", page)

if __name__ == '__main__':
    main()