#!/usr/bin/env python3
import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


AUDIO_BYTES = b"ID3\x03\x00\x00\x00\x00\x00\x00"


class Handler(BaseHTTPRequestHandler):
    server_version = "KMediaHeaderSmoke/1.0"

    def do_GET(self):
        self._log_request()

        status = 206 if self.headers.get("Range") else 200
        self.send_response(status)
        self.send_header("Content-Type", "audio/mpeg")
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(len(AUDIO_BYTES)))
        if status == 206:
            self.send_header("Content-Range", f"bytes 0-{len(AUDIO_BYTES) - 1}/{len(AUDIO_BYTES)}")
        self.end_headers()
        self.wfile.write(AUDIO_BYTES)

    def log_message(self, format, *args):
        return

    def _log_request(self):
        event = {
            "method": self.command,
            "path": self.path,
            "authorization": self.headers.get("Authorization") is not None,
            "xPlaybackToken": self.headers.get("X-Playback-Token") is not None,
            "range": self.headers.get("Range"),
            "userAgent": self.headers.get("User-Agent"),
        }
        print(json.dumps(event, ensure_ascii=True), flush=True)


def main():
    parser = argparse.ArgumentParser(description="Capture KMedia playback request headers.")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=18081)
    args = parser.parse_args()

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"listening on http://{args.host}:{args.port}/audio.mp3", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("stopped", flush=True)
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
