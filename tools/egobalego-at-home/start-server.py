import os, json, argparse, webbrowser, requests
from flask import Flask, render_template, request
from flask_socketio import SocketIO, emit
from mistune import create_markdown


parser = argparse.ArgumentParser()
parser.add_argument("--open", action='store_true', help="Open browser page on start")
parser.add_argument("-P", "--port", type=int, default=5000, help="Server port")
parser.add_argument("--debug", action='store_true', help="Flask debug mode")
parser.add_argument("--lang", type=str, default="en_us", help='Website language')

dirname = os.path.dirname(__file__)

app = Flask(__name__)
socketio = SocketIO(app, cors_allowed_origins="*")


# App Routes

@app.route('/')
def home():
    return render_custom_template('home')

@app.route('/commands.html')
def commands():
    return render_custom_template('commands')

@app.route('/trades.html')
def trades():
    return render_custom_template('trades')

@app.route('/communications.html')
def messages():
    return render_custom_template('communications')

@app.route('/quest-steps.html')
def quest_steps():
    return render_custom_template('quest-steps', help_key="help_quest")

@app.route('/structures.html')
def structures():
    return render_custom_template('structures')

@app.route('/websocket.html')
def websocket():
    return render_custom_template('websocket')


def render_custom_template(page_name, help_key = None):
    if help_key is None:
        help_key = f"help_{page_name}"
    params = {
        'name': page_name,
        'theme': color_theme,
        'help_title': translations[lang][help_key],
        'help_content': md_content(lang + f"/{help_key}"),
        'translations': translations.get(lang, translations[lang])
    }
    if page_name == "home":
        params["update_available"] = update_available
    return render_template(f"{page_name}.html", **params)


@app.route('/data_receiver', methods=['POST'])
def receive_data():
    if request.method == 'POST':
        update_data()
    return "Data sent correctly to the server!"

@app.route('/server_data', methods=['GET'])
def send_data():
    load_data()
    return server_data

@app.route('/last_id', methods=['GET'])
def send_last_id():
    return str(last_id)

@app.route('/switch_theme', methods=['GET'])
def switch_color_theme():
    global color_theme
    color_theme = "dark" if color_theme == "light" else "light"
    update_color_theme()
    return "Theme was switched!"


# SocketIO Events

mod_connected_to_socket = False

@socketio.on('mod_connect')
def handle_connect():
    global mod_connected_to_socket
    mod_connected_to_socket = True
    print("Mod connected to websocket")
    emit("mod_connect", broadcast=True)

@socketio.on('mod_disconnect')
def handle_disconnect():
    global mod_connected_to_socket
    mod_connected_to_socket = False
    print("Mod disconnected from websocket")
    emit("mod_disconnect", broadcast=True)

@socketio.on('is_mod_connected')
def handle_disconnect():
    return mod_connected_to_socket

@socketio.on('reload')
def reload_minecraft():
    print("Sending reload event")
    emit("reload", broadcast=True)

@socketio.on('rdialogue')
def send_researcher_dialogue(data):
    print("Sending rdialogue event:", data)
    emit("rdialogue", data, broadcast=True)

@socketio.on('toast')
def send_toast(data):
    print("Sending toast event:", data)
    emit("toast", data, broadcast=True)

@socketio.on('cmd')
def send_command(data):
    print("Sending cmd event:", data)
    emit("cmd", data, broadcast=True)

@socketio.on('mod_response')
def handle_mod_response(response):
    print('Received response from the mod: ' + response)
    emit("mod_response", response, broadcast=True)


# Misc functions

def load_data():
    global server_data
    try:
        with open('server_data.json', 'r') as f:
            data = f.read()
            server_data = json.loads(data)
    except FileNotFoundError:
        print("Could not find the file server_data.json, it will be created the first time you add something.")
    except Exception as e:
        print("Server data could not be loaded and was reset, changes will apply on the next edit:", e)

def update_data():
    received_data = request.json
    if "add" in received_data.keys():
        add_data(received_data["add"])
    if "remove" in received_data.keys():
        remove_data(received_data["remove"])
    update_database()

def add_data(items_to_add):
    global last_id
    for new_object in items_to_add:
        found = False
        if len(server_data) == 0:
            last_id += 1
            server_data.append(new_object)
        else:
            for object in server_data:
                if object["id"] == new_object["id"]:
                    found = True
                    break
            if found:
                server_data.remove(object)
                server_data.append(new_object)
            else:
                last_id += 1
                server_data.append(new_object)

def remove_data(items_to_remove):
    for new_object in items_to_remove:
        for object in server_data:
            if object["id"] == new_object["id"]:
                server_data.remove(object)

def update_database():
    with open("last_id.txt", "w") as f:
        f.write(str(last_id))
    with open('server_data.json', 'w') as f:
        json.dump(server_data, f, indent=4)

def update_color_theme():
    with open("color_theme.txt", "w") as f:
        f.write(color_theme)

def md_content(name: str):
    with open(os.path.join(dirname, "templates", "content", f'{name}.md'), 'r', encoding='UTF-8') as f:
        parser = create_markdown(escape=False, plugins=['strikethrough', 'footnotes', 'table'])
        return parser(f.read())

def load_translations():
    global translations
    translations_path = os.path.join(dirname, 'translations.json')
    with open(translations_path, 'r', encoding='utf-8') as f:
        translations = json.load(f)

def load_last_id():
    global last_id
    last_id_file = "last_id.txt"
    try:
        with open(last_id_file, "r") as f:
            try:
                last_id = int(f.read())
            except ValueError:
                print(f"Could not parse the content of {last_id_file} to integer, will be reset to zero.")
    except FileNotFoundError:
        print(f"Could not find the file {last_id_file}, it will be created the first time you add something.")
    except Exception as e:
        print("Last index could not be loaded and was reset, changes will apply on the next edit:", e)

def load_color_theme():
    global color_theme
    color_theme_file = "color_theme.txt"
    try:
        with open(color_theme_file, "r") as f:
            color_theme = f.read()
            if (color_theme not in ["light","dark"]):
                print(f"The theme '{color_theme}' from the file {color_theme_file} is not valid, will be reset to light.")
                color_theme = "light"
                update_color_theme()
    except FileNotFoundError:
        print(f"Could not find the file {color_theme_file}, it will be created and the theme will be set to light.")
        update_color_theme()

def check_for_updates():
    github_version_url = 'https://raw.githubusercontent.com/filloax/ruins-of-growsseth/refs/heads/dev/1.21.0/tools/egobalego-at-home/app_version'
    try:
        req = requests.get(github_version_url)
        with open ("app_version") as f:
            local_version = f.read()
        if req.status_code == requests.codes.ok:
            github_version = req.text
            if float(local_version) < float(github_version):
                global update_available
                update_available = True
                print("An update was found.")
            else:
                print("No updates available.")
        else:
            print('Error during update check: file not found on GitHub.')
    except Exception as e:
        print("Error during update check:", e)


translations = {}
server_data = []
last_id = 0
color_theme = "light"
update_available = False
lang = ""


if __name__ == '__main__':
    args = parser.parse_args()
    args_open: bool = args.open
    args_port: int = args.port
    args_debug: bool = args.debug
    args_lang: bool = args.lang

    load_translations()
    load_data()
    load_color_theme()
    check_for_updates()
    lang = args_lang

    if args_open:
        webbrowser.open(f'http://localhost:{args_port}')

    socketio.run(app, debug=args_debug, port=args_port)
