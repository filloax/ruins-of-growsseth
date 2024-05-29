"""
For testing, deprecated
"""
import socket
import argparse
from datetime import datetime

parser = argparse.ArgumentParser()

parser.add_argument("-p", "--port", type=int, default=20000)
parser.add_argument("--log", type=str, default=None)

args = parser.parse_args()

port: int = args.port
log: str = args.log

if log:
    # clear log
    with open(log, 'w', encoding='utf-8') as f:
        print("", file=f)

def printl(*args, **kwargs):
    print(*args, **kwargs)
    if log:
        now = datetime.now()
        dt_string = "[" + now.strftime("%d/%m/%Y %H:%M:%S") + "] "
        with open(log, 'a', encoding='utf-8') as f:
            print(dt_string, *args, file=f, **kwargs)

# Function to handle a single client connection
def handle_client(client_socket: socket.socket):
    peername = client_socket.getpeername()
    printl(f"[*] Accepted connection from {peername}")

    while True:
        try:
            # Wait for user input
            message = input("Enter a message to send (or 'exit' to quit): ")

            printl("input: " + message)

            # Check if the user wants to exit
            if message.lower() == 'exit':
                break

            # Send the message to the client
            client_socket.send(message.encode('utf-8'))

            # Receive data from the client
            data = client_socket.recv(1024)
            if not data:
                printl(f"[*] Connection with {peername} closed by the client.")
                break
            else:
                printl(f"Received from {peername}: {data.decode('utf-8')}")

        except Exception as e:
            printl(f"Error communicating with {peername}: {str(e)}")
            break

    client_socket.close()
    printl(f"[*] Connection with {peername} closed")

# Create a socket
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.bind(('0.0.0.0', port))
server.listen(1)  # Allow only one client at a time
server.settimeout(1.0)

printl(f"[*] Listening on 0.0.0.0:{port}")

try:
    while True:
        try:
            client_socket, addr = server.accept()
            handle_client(client_socket)
            printl(f"[*] Listening on 0.0.0.0:{port}")
        except KeyboardInterrupt:
            printl("Interrupted by user")
            break
        except IOError as msg:
            continue  
except KeyboardInterrupt:
    printl("Interrupted by user")
