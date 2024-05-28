import socket
import argparse

parser = argparse.ArgumentParser()

parser.add_argument("-p", "--port", type=int, default=20001)
parser.add_argument("-H", "--host", type=str, default="filloax.ddns.net")

args = parser.parse_args()

port = args.port
host = args.host

server_address = (host, port)

print(f"Connecting to {host}:{port}...")

# Create a socket and connect to the server
client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect(server_address)

print("Connected!")

try:
    while True:
        # Get user input
        message = input("Enter a message to send to the server (or 'exit' to quit): ")

        if message.lower() == 'exit':
            break

        # Send the message to the server
        client_socket.send(f'{message}\n'.encode())

        print("Sent:", message)

        # Receive and print the server's response
        response = client_socket.recv(1024).decode()
        print("Server response:", response)

finally:
    # Close the socket
    client_socket.close()
