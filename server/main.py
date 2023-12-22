from http.server import BaseHTTPRequestHandler, HTTPServer
import logging
import socket
import json
from pathlib import Path


from json_saver import write_json_to_file


hostName = "0.0.0.0"
serverPort = 8000


def get_ip_address():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = s.getsockname()[0]
    s.close()
    return ip


class MyServer(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.path = '/index.html'
        try:
            if self.path != ".py":
                f = open(self.path[1:]).read()
                self.send_response(200)
                self.end_headers()
                self.wfile.write(bytes(f, 'utf-8'))
            else:
                f = self.path + " - File Not Found"
                self.send_error(404, f)
        except:
            f = self.path + " - File Not Found"
            self.send_error(404, f)

    def do_POST(self):
        if self.path == '/':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length).decode('utf-8')
            write_json_to_file(post_data)

            self.send_response(200)
            self.end_headers()

        elif self.path == '/previous':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length).decode('utf-8')
            json_data = json.loads(post_data)
            file_name = json_data['file']
            base_path = Path(__file__).parent
            file_path = (base_path / "entries" / file_name).resolve()
            try:
                with open(file_path, 'r') as file:
                    write_json_to_file(file.read().replace('\n', ''))
                self.send_response(200)
                self.end_headers()
            except FileNotFoundError:
                self.send_response(404)
                self.end_headers()


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    webServer = HTTPServer((hostName, serverPort), MyServer)
    print("Server started http://%s:%s" % (get_ip_address(), serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")
    