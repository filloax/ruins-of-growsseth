from flask import Flask, json
import os, json, importlib

app = Flask(__name__)
dirname = os.path.dirname(__file__)

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


@app.route('/')
def home():
    return "Egobalego server is running!"

@app.route('/server_data', methods=['GET'])
def send_data():
    try:
        with open('server_data.json', 'r') as f:
            data = f.read()
            server_data = json.loads(data)
            return server_data
    except Exception as e:
        print("Error, server data could not be loaded:", e)
        return []


SERVER_DATA_FILE = "server_data.json"

if __name__ == '__main__':
    import_or_install("flask")

    if not os.path.exists("server_data.json"):
        with open("server_data.json", 'w', encoding='utf-8') as f:
            f.write("[]")
            print("\nServer data file was created! You can start editing it now.\n")

    app.run()
