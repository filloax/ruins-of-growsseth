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

app = Flask(__name__)

@app.route('/')
def home():
    return render_template(
        'home.html', name="home", 
        help_title="Aiuto",
        help_content=md_content("help_home"),
    )

@app.route('/commands.html')
def commands():
    return render_template(
        'commands.html', name="commands",
        help_title="Aiuto comandi",
        help_content=md_content("help_commands"),
    )

@app.route('/trades.html')
def trades():
    return render_template(
        'trades.html', name="trades",
        help_title="Aiuto scambi",
        help_content=md_content("help_trades"),
    )

@app.route('/communications.html')
def messages():
    return render_template(
        'communications.html', name="communications",
        help_title="Aiuto comunicazioni",
        help_content=md_content("help_communications"),
    )

@app.route('/quest-steps.html')
def quest_steps():
    return render_template(
        'quest-steps.html', name="quest-steps",
        help_title="Aiuto quest",
        help_content=md_content("help_quest"),
    )

@app.route('/structures.html')
def structures():
    return render_template(
        'structures.html', name="structures",
        help_title="Aiuto strutture",
        help_content=md_content("help_structures"),
    )


@app.route('/data_receiver', methods=['POST'])
def receive_data():
    if request.method == 'POST':
        update_data()
    return "Data sent correctly to the server!"

@app.route('/server_data', methods=['GET'])
def send_data():
    return server_data

@app.route('/last_id', methods=['GET'])
def send_last_id():
    return str(last_id)


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
        return markdown_to_html(f.read())

def markdown_to_html(markdown_text):
    parser = create_markdown(escape=False, plugins=['strikethrough', 'footnotes', 'table'])
    return parser(markdown_text)

server_data = []
last_id = 0


if __name__ == '__main__':
    args = parser.parse_args()
    args_open: bool = args.open
    args_port: int = args.port
    args_debug: bool = args.debug
    
    try:
        with open('server_data.json', 'r') as f:
            data = f.read()
            server_data = json.loads(data)
    except FileNotFoundError:
        print("Could not find the file server_data.json, it will be created the first time you add something.")
    except Exception as e:
        print("Server data could not be loaded and was reset, changes will apply on the next edit:", e)

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
