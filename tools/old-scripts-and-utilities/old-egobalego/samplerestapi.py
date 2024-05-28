import importlib
import os

def import_or_install(package):
    try:
        importlib.import_module(package)
    except ImportError:
        print(f"{package} is not installed. Installing...")
        try:
            import pip
        except ImportError:
            # For Python 3.4 and below, pip is not bundled with Python, so use the deprecated `imp` module
            import imp
            imp.find_module('pip')
            from pip import __version__ as pip_version
        else:
            from pip import __version__ as pip_version

        if pip_version >= "10.0.0":
            from pip._internal import main as pip_main
        else:
            from pip import main as pip_main

        pip_main(['install', package])
        print(f"{package} has been installed.")

import_or_install("flask")
from flask import Flask, json, request

api = Flask(__name__)

scriptdir = os.path.abspath(os.path.dirname(__file__))

def read(fname: str):
  with open(os.path.join(scriptdir, fname), 'r', encoding='utf-8') as f:
    out = f.read()
  return out

@api.route('/quests', methods=['GET'])
def get_quests():
  return read("quests.json")

@api.route('/struct', methods=['GET'])
def get_struct():
  return read("struct.json")

WEBSITE_MOCK_FILE = "website_mock.json"

@api.route('/structures/mod/active', methods=['GET'])
def get_website_mock():
  print("Headers:", dict(request.headers))
  return read(WEBSITE_MOCK_FILE)

def main():
  os.chdir(os.path.dirname(__file__))
  if not os.path.exists(WEBSITE_MOCK_FILE):
    print(f"Creazione {WEBSITE_MOCK_FILE}")
    with open(WEBSITE_MOCK_FILE, 'w', encoding='utf-8') as f:
      f.write(SAMPLE_WEBSITE_JSON)
    print(f"{WEBSITE_MOCK_FILE} creato")

  print("AVVIO SERVER FAKE-SITO")
  print("Modificare website-mock.json per cambiare i dati inviati alla mod\n(Non serve riavviare questo script)")
  api.run()

SAMPLE_WEBSITE_JSON = """
[
    {
        "id": 0,
        "structureID": "growsseth:researcher_tent",
        "name": "researcher_tent",
        "x": 1374,
        "y": 0,
        "z": 162,
        "active": true,
        "rotation": "none"
    },
    {
        "id": 1,
        "structureID": "growsseth:cave_camp",
        "name": "cave_camp",
        "x": 934,
        "y": 38,
        "z": 340,
        "active": true,
        "rotation": "none"
    },
    {
        "id": 2,
        "structureID": "growsseth:marker",
        "name": "cave_camp_marker",
        "x": 940,
        "y": 68,
        "z": 347,
        "active": true,
        "rotation": "none"
    },
    {
        "id": 3,
        "structureID": "growsseth:enchant_tower",
        "name": "enchant_tower",
        "x": 3281,
        "y": 176,
        "z": -642,
        "active": false,
        "rotation": "180"
    },
    {
        "id": 4,
        "structureID": "growsseth:golem_variants/savanna_golem_house",
        "name": "golem_variants/savanna_golem_house",
        "x": -1293,
        "y": 70,
        "z": -2026,
        "active": false,
        "rotation": "180"
    },
    {
        "id": 5,
        "structureID": "growsseth:beekeeper_house",
        "name": "beekeeper_house",
        "x": 1427,
        "y": 70,
        "z": 192,
        "active": false,
        "rotation": "none"
    },
    {
        "id": 6,
        "structureID": "growsseth:noteblock_lab",
        "name": "noteblock_lab",
        "x": 2260,
        "y": 69,
        "z": -1674,
        "active": false,
        "rotation": "none"
    },
    {
        "id": 7,
        "structureID": "growsseth:conduit_ruins",
        "name": "conduit_ruins",
        "x": -1284,
        "y": 32,
        "z": -3152,
        "active": false,
        "rotation": "none"
    },
    {
        "id": 8,
        "structureID": "event:researcher_end_quest_start",
        "name": "...",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 9,
        "structureID": "event:researcher_end_quest_zombie",
        "name": "...",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 10,
        "structureID": "event:researcher_end_quest_leave",
        "name": "...",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 11,
        "structureID": "event:toast/Messaggio di prova",
        "name": "S\u00ec, tutto questo viene effettivamente aggiornato in diretta.",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 12,
        "structureID": "event:rdialogue/api-test",
        "name": "Ehi fra mi sono spostato\nBel wither, 10/10",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 13,
        "structureID": "event:sell/minecraft/paper/1",
        "name": "...",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 14,
        "structureID": "event:sell_all_extras",
        "name": "...",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 15,
        "structureID": "event:tpResearcher",
        "name": "tp res 1",
        "x": -40,
        "y": 70,
        "z": 0,
        "active": false
    },
    {
        "id": 16,
        "structureID": "event:rmTent",
        "name": "rm tent 1",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    },
    {
        "id": 17,
        "structureID": "event:spawnResearcher",
        "name": "spawn res",
        "x": 0,
        "y": 150,
        "z": 0,
        "active": false
    },
    {
        "id": 18,
        "structureID": "event:rmResearcher",
        "name": "rm res 1",
        "x": 0,
        "y": 0,
        "z": 0,
        "active": false
    }
]
"""


if __name__ == '__main__':
  main()