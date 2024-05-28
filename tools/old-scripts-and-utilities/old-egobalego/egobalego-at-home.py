import importlib
import os
import http.server
import socketserver
import threading
import webbrowser

HTTP_PORT = 8000
REST_PORT = 5000
WEBSITE_MOCK_FILE = "website_mock.json"

class CustomHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.path = 'index.html'
        elif self.path == '/get_data':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            json_data = self.get_json_data()
            self.wfile.write(json.dumps(json_data).encode())
            return
        return http.server.SimpleHTTPRequestHandler.do_GET(self)


    def do_POST(self):
        if self.path == '/submit':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length).decode('utf-8')
            form_data = json.loads(post_data)

            with open(WEBSITE_MOCK_FILE, 'w', encoding='utf-8') as f:
                json.dump(form_data, f, indent=4)

            # Respond with a 204 No Content status (success, no data in response)
            self.send_response(204)
            self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b'Not Found')


    def get_json_data(self):
        out = {}
        with open(WEBSITE_MOCK_FILE, 'r', encoding='utf-8') as f:
            json_data = json.load(f)
        for entry in json_data:
            out[entry["structureID"]] = entry
        #print("out:" out)
        return out
    

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
    if not os.path.exists("./index.html"):
        print(f"Creazione pagina web")
        with open("./index.html", 'w', encoding='utf-8') as f:
            f.write(SAMPLE_WEBSITE_PAGE)
        print(f"./index.html creato")

    print("AVVIO SERVER FAKE-SITO")

    api_thread = threading.Thread(target=api.run, kwargs={'port': REST_PORT})
    api_thread.daemon = True
    api_thread.start()

    # Create a socket server with the request handler
    with socketserver.TCPServer(("", HTTP_PORT), CustomHTTPRequestHandler) as httpd:
        webbrowser.open(f'http://localhost:{HTTP_PORT}')
        print(f"Sito mock su http://localhost:{HTTP_PORT}")
        httpd.serve_forever()


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


SAMPLE_WEBSITE_PAGE = """
<!DOCTYPE html>
<html lang="it">

<head>
    <meta name="description" content="Project EgoBalego finto per provare mod" />
    <meta charset="utf-8">
    <title>EgoBalego At Home</title>
    <link rel="icon" href="data:;base64,iVBORw0KGgo=">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="author" content="Filloax, Krozzzt">
    <style>
        .container {
            text-transform: lowercase;
            font-family: 'Comic Sans MS';
            text-align: center;
        }
    </style>

    <script>
        document.addEventListener("DOMContentLoaded", function () {
            fetch("/get_data").then((response) => {
                response.json().then((mockData) => {
                    console.log("Received data:", mockData);

                    document.querySelectorAll('input[name="structureID"]').forEach((event) => {
                        var structureID = event.value;
                        Object.keys(mockData).forEach((key) => {
                            if (key.includes(structureID)) {
                                eventData = mockData[key];
                            }
                        });

                        var specialEventType = checkIfSpecialEvent(structureID);
                        if (specialEventType !== null)
                            manageSpecialEventReceived(event, specialEventType, eventData.structureID);

                        var nameInput = event.parentElement.querySelector("[name='name']");
                        var xInput = event.parentElement.querySelector("[name='x']");
                        var yInput = event.parentElement.querySelector("[name='y']");
                        var zInput = event.parentElement.querySelector("[name='z']");
                        var rotationInput = event.parentElement.querySelector("[name='rotation']");

                        event.checked = eventData.active;
                        if (nameInput !== null) {
                            nameInput.value = eventData.name;
                        }
                        if (xInput !== null) {
                            xInput.value = eventData.x;
                            yInput.value = eventData.y;
                            zInput.value = eventData.z;
                        }
                        if (eventData.rotation !== undefined) {
                            rotationInput.value = eventData.rotation;
                        }
                    });
                });
            });
            // Automatically send data to json when editing anything:
            document.querySelectorAll("input,textarea,select").forEach((el) => {
                el.addEventListener("change", function () {
                    submitForm();
                });
            });
        });

        function manageSpecialEventReceived(event, specialEventType, structureID) {
            // The golem house, communications and sell events keep event data the structureID field
            switch (specialEventType) {
                case "golem_variant":
                    variantInput = event.parentElement.querySelector("[name='golem_variant']");
                    variantInput.value = structureID.split("/")[1];
                    break;
                case "communication":
                    idInput = event.parentElement.querySelector("[name='id']");
                    commParameters = structureID.split("/")
                    if (commParameters.length > 2)
                        idInput.value = commParameters[1] + "/" + commParameters[2] + "/" + commParameters[3];
                    else
                        idInput.value = commParameters[1];
                    break;
                case "sell":
                    var tradeData = structureID.split("/");
                    itemIdInput = event.parentElement.querySelector("[name='item_id']");
                    itemIdInput.value = tradeData[1] + "/" + tradeData[2];
                    priceInput = event.parentElement.querySelector("[name='price']");
                    if (tradeData.length == 5)
                        priceInput.value = tradeData[3] + "/" + tradeData[4];
                    else
                        priceInput.value = tradeData[3];
                    break;
            }
        }

        function submitForm() {
            var mockData = [];
            var events = Array.from(document.querySelectorAll('input[name="structureID"]'));
            events.forEach(function callback(event, index) {
                var structureID = event.value;
                var specialEventType = checkIfSpecialEvent(structureID);
                if (specialEventType !== null)
                    structureID = manageSpecialEventToSend(event, specialEventType, structureID);

                var nameInput = event.parentElement.querySelector("[name='name']") || "";
                var xInput = event.parentElement.querySelector("[name='x']") || "";
                var yInput = event.parentElement.querySelector("[name='y']") || "";
                var zInput = event.parentElement.querySelector("[name='z']") || "";
                var rotationInput = event.parentElement.querySelector("[name='rotation']");

                eventData = {
                    id: index,
                    structureID: structureID,
                    name: nameInput.value || "...",
                    x: parseInt(xInput.value) || 0,
                    y: parseInt(yInput.value) || 0,
                    z: parseInt(zInput.value) || 0,
                    active: event.checked,
                };
                if (rotationInput !== null && rotationInput.value !== "auto")
                    // the "automatic" rotation does not need to be specified
                    eventData["rotation"] = rotationInput.value;

                mockData.push(eventData);
            });

            fetch("/submit", {
                method: "POST",
                body: JSON.stringify(mockData),
                headers: {
                    "Content-Type": "application/json",
                },
            });
            console.log("Returned data:", mockData);
        }

        function manageSpecialEventToSend(event, specialEventType, structureID) {
            var newStructureId = structureID;
            switch (specialEventType) {
                case "golem_variant":
                    variantInput = event.parentElement.querySelector("[name='golem_variant']");
                    newStructureId = structureID + variantInput.value;
                    break;
                case "communication":
                    idInput = event.parentElement.querySelector("[name='id']");
                    newStructureId = structureID + idInput.value;
                    break;
                case "sell":
                    itemIdInput = event.parentElement.querySelector("[name='item_id']");
                    priceInput = event.parentElement.querySelector("[name='price']");
                    newStructureId = structureID + itemIdInput.value + "/" + priceInput.value;
                    break;
            }
            return newStructureId;
        }

        function checkIfSpecialEvent(structureID) {
            var specialEventType = null;
            if (structureID.includes("golem_variants/"))
                specialEventType = "golem_variant";
            else if (structureID.includes("toast") || structureID.includes("rdialogue"))
                specialEventType = "communication";
            else if (structureID.includes("sell/"))
                specialEventType = "sell";
            return specialEventType;
        }
    </script>

</head>

<body>

    <div class="container">

        <h1>Egobalego At Home©</h1>

        <form onsubmit="submitForm(); return false;">
            <br><br>Tutti i dati sono aggiornati nel mock in tempo reale (per i testi bisogna cliccare fuori dalla casella)
            <br>cambiando gli id si possono ripetere i comandi ingame<br><br>

            <h3>Strutture</h3>
            <label><input type="checkbox" name="structureID" value="growsseth:researcher_tent">Tenda Ricercatore
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° oraria</option>
                    <option value="counterclockwise_90">90° antioraria</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:cave_camp">Accampamento Caverna
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:marker">Stendardo segnaletico
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:enchant_tower">Torre
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:golem_variants/">Casa Golem
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>variante:
                <select name="golem_variant">
                    <option value="desert_golem_house">deserto</option>
                    <option value="plains_golem_house">pianura</option>
                    <option value="savanna_golem_house">savana</option>
                    <option value="snowy_golem_house">neve</option>
                    <option value="taiga_golem_house">taiga</option>
                    <option value="zombie_desert_golem_house">deserto zombie</option>
                    <option value="zombie_plains_golem_house">pianura zombie</option>
                    <option value="zombie_savanna_golem_house">savana zombie</option>
                    <option value="zombie_snowy_golem_house">neve zombie</option>
                    <option value="zombie_taiga_golem_house">taiga zombie</option>
                </select>
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:beekeeper_house">Casa Apicoltore
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:noteblock_lab">Lab Noteblock
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br><br>
            <label><input type="checkbox" name="structureID" value="growsseth:conduit_ruins">Rovine Conduit
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em">
                <br><br>rotazione:
                <select name="rotation">
                    <option value="none">nessuna</option>
                    <option value="clockwise_90">90° orario</option>
                    <option value="counterclockwise_90">90° antiorario</option>
                    <option value="180">180°</option>
                    <option value="auto">automatica</option>
                </select> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br>

            <h3>Quest</h3>
            <label><input type="checkbox" name="structureID" value="event:researcher_end_quest_start">Ricercatore Malato</label><br><br>
            <label><input type="checkbox" name="structureID" value="event:researcher_end_quest_zombie">Ricercatore Zombie</label><br><br>
            <label><input type="checkbox" name="structureID" value="event:researcher_end_quest_leave">Ricercatore Slogga</label>
            <br><br>

            <h3>Comunicazione</h3>
            <label><input type="checkbox" name="structureID" value="event:toast/">Notifica con titolo:
                <input type="text" name="id" placeholder="titolo"> (usare "namespace/nome_item/titolo" per dare un'icona), contenuto (opzionale):
                <input type="text" name="name" placeholder="contenuto">
            </label><br><br>
            <label><input type="checkbox" name="structureID" value="event:rdialogue/">Dialogo con id:
                <input type="text" name="id" style="width: 7em" placeholder="id">, contenuto:
                <textarea name="name" rows="1" style="resize:none" placeholder="contenuto"></textarea> (andare a capo per separare i messaggi in chat)
            </label><br><br>

            <h3>Mercato</h3>
            <label><input type="checkbox" name="structureID" value="event:sell/">Vendi
                <input type="text" name="item_id" placeholder="namespace/id_item"> a
                <input type="text" name="price" style="width: 4em" placeholder="prezzo"> smeraldi (aggiungere /h al prezzo per non mandare la notifica)
            </label><br><br>
            <label><input type="checkbox" name="structureID" value="event:sell_enchant_dictionary">Vendi dizionario di enchantese</label><br><br>
            <label><input type="checkbox" name="structureID" value="event:sell_trim_template">Vendi trim</label><br><br>
            <label><input type="checkbox" name="structureID" value="event:sell_sherd">Vendi sherd</label><br><br>
            <label><input type="checkbox" name="structureID" value="event:sell_all_extras">Vendi tutto</label><br><br>

            <h3>Emergenze</h3>
            <label><input type="checkbox" name="structureID" value="event:tpResearcher">Teletrasporta Manfredo a
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em"> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br>
            <label><input type="checkbox" name="structureID" value="event:rmTent">Rimuovi la tenda in
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em"> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br>
            <label><input type="checkbox" name="structureID" value="event:spawnResearcher">Spawna un Manfredo a
                &nbsp x:<input type="number" name="x" style="width: 4em">
                &nbsp y:<input type="number" name="y" style="width: 4em">
                &nbsp z:<input type="number" name="z" style="width: 4em"> / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br>
            <label><input type="checkbox" name="structureID" value="event:rmResearcher">Rimuovi Manfredi / id:
                <input type="text" name="name" placeholder="id comando">
            </label><br><br>
            <br>
        </form>

    </div>

</body>

</html>
"""


if __name__ == '__main__':
    main()
