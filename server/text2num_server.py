from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse
from text_to_num import text2num, alpha2digit


hostName = '192.168.3.14'
serverPort = 4200

class NumServer(BaseHTTPRequestHandler):

    def do_GET(self):

        message = parse_qs(urlparse(self.path).query).get('message', None)[0]
        print(message)

        parsed_message = alpha2digit(message, "de")

        self.send_response(200)
        self.send_header('Content-Type', 'text/plain')
        self.end_headers()
        self.wfile.write(str(parsed_message).encode())
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
