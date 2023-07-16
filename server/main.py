from http.server import BaseHTTPRequestHandler, HTTPServer
import time
import logging
import json
import dateutil.parser
from dateutil import tz
import socket
import fcntl
import struct

hostName = "0.0.0.0"
serverPort = 8000

import socket
def get_ip_address():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(("8.8.8.8", 80))
    ip = s.getsockname()[0]
    s.close()
    return ip


class MyServer(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length).decode('utf-8')

        entries = json.loads(post_data)
        for entry in entries:
            utc = dateutil.parser.isoparse(entry['entryTime'])
            to_zone = tz.gettz(entry['timeZone'])
            at_zone = utc.astimezone(to_zone)
            print('------------------------')
            print(at_zone.strftime('%b %d %H:%M'))
            print('------------------------')
            lines = entry['text'].split("\n")
            for line in lines:
                print("- " + line)
            print('------------------------')
            print()

        print("\n\n\n\n\n\n\n\n\n\n\n\n\n\n")
        self.send_response(200)
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

    