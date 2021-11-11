from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse
from text_to_num import text2num, alpha2digit
import json


hostName = '0.0.0.0'
serverPort = 4200

class NumServer(BaseHTTPRequestHandler):

    def do_GET(self):

        if (parse_qs(urlparse(self.path).query).get('message', None) != None):
            message = parse_qs(urlparse(self.path).query).get('message', None)[0]
            print(message)
            parsed_message = alpha2digit(message, "de")

            self.send_response(200)
            # self.send_header('Content-Type', 'text/plain')
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            # self.wfile.write(str(parsed_message).encode())

            json_string = {'message' : parsed_message, 'language' : 'de'}

            self.wfile.write(json.dumps(json_string).encode(encoding='utf-8'))

        return



if __name__ == "__main__":
    webServer = HTTPServer((hostName, serverPort), NumServer)
    print("Server started http://%s:%s" % (hostName, serverPort))

    try:
        webServer.serve_forever()
    except KeyboardInterrupt:
        pass

    webServer.server_close()
    print("Server stopped.")
