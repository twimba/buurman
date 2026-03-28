#!/usr/bin/env python3
"""Buurman Hub — workspace directory & service status dashboard.

Detects active workspaces from ~/.buurman/workspaces/w*.lock files,
checks Docker container and host port status, and serves a dashboard.

Usage:
    python3 scripts/hub/server.py                  # default port 3333
    HUB_PORT=8080 python3 scripts/hub/server.py    # custom port
"""

import http.server
import json
import os
import socket
import subprocess
from pathlib import Path

PORT = int(os.environ.get("HUB_PORT", 3333))
HUB_DIR = Path(__file__).parent
WORKSPACES_DIR = Path.home() / ".buurman" / "workspaces"

SERVICES = [
    {"key": "app",        "label": "App",        "container": "app",        "category": "app"},
    {"key": "api",        "label": "API",        "container": "backend",    "category": "backend"},
    {"key": "backoffice", "label": "Backoffice", "container": "backoffice", "category": "app"},
    {"key": "keycloak",   "label": "Keycloak",   "container": "keycloak",   "category": "auth"},
    {"key": "mailpit",    "label": "Mailpit",    "container": "mailpit",    "category": "devtools"},
    {"key": "prometheus", "label": "Prometheus", "container": "prometheus", "category": "monitoring"},
    {"key": "grafana",    "label": "Grafana",    "container": "grafana",    "category": "monitoring"},
]

# Host port formulas for dev-mode detection (backend/app/backoffice on host)
HOST_PORTS = {
    "app":        lambda ws: 5173 + ws * 10,
    "api":        lambda ws: 8081 + ws * 100,
    "backoffice": lambda ws: 5174 + ws * 10,
}


def _is_port_open(port, host="127.0.0.1", timeout=0.3):
    """Check if a TCP port is listening on the given host."""
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except (OSError, socket.timeout):
        return False


def _get_docker_containers():
    """Return a dict of running Docker container names to their state."""
    try:
        result = subprocess.run(
            ["docker", "ps", "--format", "{{.Names}}\t{{.State}}"],
            capture_output=True, text=True, timeout=5,
        )
        containers = {}
        for line in result.stdout.strip().split("\n"):
            if "\t" in line:
                name, state = line.split("\t", 1)
                containers[name] = state
        return containers
    except Exception:
        return {}


def _get_git_branch(workspace_path):
    """Read the current git branch from the workspace directory."""
    try:
        git_path = Path(workspace_path) / ".git"
        if not git_path.exists():
            return None

        # Handle worktrees: .git is a file containing "gitdir: <path>"
        if git_path.is_file():
            gitdir_line = git_path.read_text().strip()
            if gitdir_line.startswith("gitdir: "):
                git_path = Path(gitdir_line[8:])

        head_file = git_path / "HEAD"
        if head_file.exists():
            content = head_file.read_text().strip()
            if content.startswith("ref: refs/heads/"):
                return content[16:]
    except Exception:
        pass
    return None


def get_workspaces():
    """Scan lock files and Docker containers to build workspace data."""
    if not WORKSPACES_DIR.exists():
        return []

    docker_containers = _get_docker_containers()
    workspaces = []

    for lock_file in sorted(WORKSPACES_DIR.glob("w*.lock")):
        try:
            num = int(lock_file.stem[1:])
        except ValueError:
            continue

        workspace_path = lock_file.read_text().strip()
        name = Path(workspace_path).name if workspace_path else f"workspace-{num}"
        https_port = 443 + num * 1000
        project_name = f"buurman-w{num}"
        branch = _get_git_branch(workspace_path) if workspace_path else None

        services = []
        for svc in SERVICES:
            url = f"https://w{num}-{svc['key']}.local.buurman.io:{https_port}"
            container_name = f"{project_name}-{svc['container']}-1"

            status = "unknown"
            if docker_containers.get(container_name) == "running":
                status = "docker"
            elif svc["key"] in HOST_PORTS:
                if _is_port_open(HOST_PORTS[svc["key"]](num)):
                    status = "host"

            services.append({
                "key": svc["key"],
                "label": svc["label"],
                "url": url,
                "category": svc["category"],
                "status": status,
            })

        workspaces.append({
            "number": num,
            "name": name,
            "path": workspace_path,
            "branch": branch,
            "port": https_port,
            "services": services,
        })

    return workspaces


class HubHandler(http.server.SimpleHTTPRequestHandler):
    """HTTP handler: serves index.html and /api/workspaces endpoint."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(HUB_DIR), **kwargs)

    def do_GET(self):
        if self.path == "/api/workspaces":
            body = json.dumps(get_workspaces()).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-cache")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        else:
            if self.path == "/":
                self.path = "/index.html"
            super().do_GET()

    def log_message(self, _format, *_args):
        pass


if __name__ == "__main__":
    print(f"\033[1;36mBuurman Hub\033[0m  http://localhost:{PORT}")
    print(f"Scanning {WORKSPACES_DIR}")
    print("Press Ctrl+C to stop.\n")
    try:
        http.server.HTTPServer.allow_reuse_address = True
        with http.server.HTTPServer(("", PORT), HubHandler) as server:
            server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")
