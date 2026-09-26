#!/usr/bin/env python3
"""Serve website/ the way Cloudflare Workers Assets does.

Workers Assets uses html_handling="auto-trailing-slash", so the deployed site
answers /support and 307s /support.html -> /support. A plain
`python3 -m http.server` does the opposite, which means local preview would
404 on every link. This keeps local and production honest about each other.

Usage: python3 scripts/serve-website.py [port]
"""

import functools
import http.server
import os
import socketserver
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "website")


class AssetsHandler(http.server.SimpleHTTPRequestHandler):
    def send_head(self):
        path = self.path.split("?", 1)[0].split("#", 1)[0]

        # /index.html -> /   and   /support.html -> /support
        if path.endswith(".html"):
            target = "/" if path == "/index.html" else path[: -len(".html")]
            self.send_response(307)
            self.send_header("Location", target)
            self.end_headers()
            return None

        # /support -> support.html, when that file exists
        if not path.endswith("/"):
            candidate = os.path.join(ROOT, path.lstrip("/") + ".html")
            if os.path.isfile(candidate):
                self.path = path + ".html"

        return super().send_head()

    def log_message(self, fmt, *args):
        sys.stderr.write("  %s\n" % (fmt % args))


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8088
    handler = functools.partial(AssetsHandler, directory=ROOT)
    socketserver.ThreadingTCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer(("127.0.0.1", port), handler) as httpd:
        print(f"Serving website/ at http://localhost:{port}")
        print("Extensionless URLs resolve the way Cloudflare serves them.")
        print("Press Ctrl-C to stop.")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print()


if __name__ == "__main__":
    main()
