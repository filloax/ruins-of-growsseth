import os
from flask import Flask, render_template, request
import json
import argparse
import webbrowser
from mistune import create_markdown

parser = argparse.ArgumentParser()
parser.add_argument("--open", action='store_true', help="Open browser page on start")
parser.add_argument("-P", "--port", type=int, default=5000, help="Server port")
parser.add_argument("--debug", action='store_true', help="Flask debug mode")
parser.add_argument("lang", type=str, help='Website language')


app = Flask(__name__)

@app.route('/')
def home():
    return render_template(
        'home.html', name="home", 
        help_title = translations[lang]["help_home"],
        help_content = md_content(lang + "/help_home"),
        translations = translations.get(lang, translations[lang])
    )

@app.route('/commands.html')
def commands():
    return render_template(
        'commands.html', name="commands",
        help_title = translations[lang]["help_commands"],
        help_content = md_content(lang + "/help_commands"),
        translations = translations.get(lang, translations[lang])
    )

@app.route('/trades.html')
def trades():
    return render_template(
        'trades.html', name="trades",
        help_title = translations[lang]["help_trades"],
        help_content = md_content(lang + "/help_trades"),
        translations = translations.get(lang, translations[lang])
    )

@app.route('/communications.html')
def messages():
    return render_template(
        'communications.html', name="communications",
        help_title = translations[lang]["help_communications"],
        help_content = md_content(lang + "/help_communications"),
        translations = translations.get(lang, translations[lang])
    )

@app.route('/quest-steps.html')
def quest_steps():
    return render_template(
        'quest-steps.html', name="quest-steps",
        help_title = translations[lang]["help_quest"],
        help_content = md_content(lang + "/help_quest"),
        translations = translations.get(lang, translations[lang])
    )

@app.route('/structures.html')
def structures():
    return render_template(
        'structures.html', name="structures",
        help_title = translations[lang]["help_structures"],
        help_content = md_content(lang + "/help_structures"),
        translations = translations.get(lang, translations[lang])
    )


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


dirname = os.path.dirname(__file__)

def md_content(name: str):
    with open(os.path.join(dirname, "templates", "content", f'{name}.md'), 'r', encoding='UTF-8') as f:
        parser = create_markdown(escape=False, plugins=['strikethrough', 'footnotes', 'table'])
        return parser(f.read())

def load_translations():
    global translations
    translations_path = os.path.join(dirname, 'translations.json')
    with open(translations_path, 'r', encoding='utf-8') as f:
        translations = json.load(f)


translations = {}
server_data = []
last_id = 0
lang = ""


if __name__ == '__main__':
    args = parser.parse_args()
    args_open: bool = args.open
    args_port: int = args.port
    args_debug: bool = args.debug
    args_lang: bool = args.lang
    
    load_translations()
    load_data()
    lang = args_lang
    
    try:
        with open("last_id.txt", "r") as f:
            try:
                last_id = int(f.read())
            except ValueError:
                print("Could not parse the content of last_id.txt to integer, will be reset to zero.")
    except FileNotFoundError:
        print("Could not find the file last_id.txt, it will be created the first time you add something.")
    except Exception as e:
        print("Last index could not be loaded and was reset, changes will apply on the next edit:", e)

    if args_open:
        webbrowser.open(f'http://localhost:{args_port}')

    app.run(debug=args_debug, port=args_port)
