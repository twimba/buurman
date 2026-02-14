#!/usr/bin/env python3
"""
Flagsmith bootstrap script — runs as a one-shot Docker container.
Idempotent: safe to run on every `make dev`.

Creates admin account via Djoser registration API, project "Buurman",
Development + Production environments, and writes the server-side
environment key into the bind-mounted .env file.
"""

import json
import os
import re
import sys
import urllib.request
import urllib.error

API = os.environ.get("FLAGSMITH_API_URL", "http://flagsmith:8000")
EMAIL = os.environ.get("FLAGSMITH_ADMIN_EMAIL", "buurmy@buurman.io")
PASSWORD = os.environ.get("FLAGSMITH_ADMIN_PASSWORD", "buurmy")
PROJECT_NAME = "Buurman"
ENV_FILE = "/.env"

# Parse flag keys from FeatureFlags.java — no manual list to maintain
FEATURE_FLAGS_FILE = "/FeatureFlags.java"
_FLAG_PATTERN = re.compile(r'public static final String \w+\s*=\s*"([^"]+)"')


def parse_flags():
    try:
        with open(FEATURE_FLAGS_FILE) as f:
            return _FLAG_PATTERN.findall(f.read())
    except FileNotFoundError:
        log(f"WARNING: {FEATURE_FLAGS_FILE} not found, no flags to create.")
        return []


def log(msg):
    print(f"[flagsmith-setup] {msg}", flush=True)


def api_call(method, path, data=None, token=None):
    url = f"{API}{path}"
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Token {token}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode()
        log(f"  HTTP {e.code} from {method} {path}: {body}")
        return json.loads(body) if body else {}


# --- 1. Check if already initialised (try to log in) ---
log("Checking if Flagsmith is already initialised...")
login_resp = api_call("POST", "/api/v1/auth/login/", {"email": EMAIL, "password": PASSWORD})

if "key" in login_resp:
    token = login_resp["key"]
    log("Already initialised — admin login successful.")
else:
    # --- 2. Register admin via Djoser REST API ---
    log("Registering admin account...")
    reg_resp = api_call("POST", "/api/v1/auth/users/", {
        "email": EMAIL,
        "password": PASSWORD,
        "re_password": PASSWORD,
        "first_name": "Admin",
        "last_name": "User",
    })
    log(f"Register response: {reg_resp}")

    # --- 3. Log in to get auth token ---
    log("Logging in...")
    login_resp = api_call("POST", "/api/v1/auth/login/", {"email": EMAIL, "password": PASSWORD})
    token = login_resp.get("key")
    if not token:
        log(f"ERROR: Failed to obtain auth token: {login_resp}")
        sys.exit(1)
    log("Login successful.")

# --- 4. Create project (skip if exists) ---
log("Checking for existing projects...")
projects = api_call("GET", "/api/v1/projects/", token=token)

project_id = None
for p in (projects if isinstance(projects, list) else []):
    if p.get("name") == PROJECT_NAME:
        project_id = p["id"]
        log(f"Project '{PROJECT_NAME}' already exists (id={project_id}).")
        break

if not project_id:
    # Need an organisation first
    log("Checking for existing organisations...")
    orgs = api_call("GET", "/api/v1/organisations/", token=token)
    org_list = orgs.get("results", orgs) if isinstance(orgs, dict) else orgs
    org_id = org_list[0]["id"] if org_list else None

    if not org_id:
        log("Creating organisation 'Buurman'...")
        org_resp = api_call("POST", "/api/v1/organisations/", {"name": "Buurman"}, token=token)
        org_id = org_resp.get("id")
        log(f"Organisation created (id={org_id}).")

    log(f"Creating project '{PROJECT_NAME}' in organisation {org_id}...")
    proj_resp = api_call("POST", "/api/v1/projects/", {"name": PROJECT_NAME, "organisation": org_id}, token=token)
    project_id = proj_resp.get("id")
    if not project_id:
        log(f"ERROR: Failed to create project: {proj_resp}")
        sys.exit(1)
    log(f"Project created (id={project_id}).")

# --- 5. Create environment if needed ---
log(f"Fetching environments for project {project_id}...")
envs_resp = api_call("GET", f"/api/v1/environments/?project={project_id}", token=token)
env_list = envs_resp.get("results", []) if isinstance(envs_resp, dict) else envs_resp

ENV_NAME = "Local"
env_names = {e["name"] for e in env_list}
if ENV_NAME not in env_names:
    log(f"Creating {ENV_NAME} environment...")
    env_resp = api_call("POST", "/api/v1/environments/", {"name": ENV_NAME, "project": project_id}, token=token)
    log(f"{ENV_NAME} environment created: api_key={env_resp.get('api_key')}")
    env_list.append(env_resp)

# Find Local environment's client-side key (used to manage server-side keys)
client_key = None
for env in env_list:
    if env.get("name") == ENV_NAME:
        client_key = env.get("api_key")
        break

if not client_key:
    log(f"WARNING: Could not find {ENV_NAME} environment.")
    sys.exit(0)

# --- 5b. Create or find server-side API key ---
SERVER_KEY_NAME = "Backend"
log("Checking for existing server-side API keys...")
existing_keys = api_call("GET", f"/api/v1/environments/{client_key}/api-keys/", token=token)
key_list = existing_keys if isinstance(existing_keys, list) else existing_keys.get("results", [])

server_key = None
for k in key_list:
    if k.get("name") == SERVER_KEY_NAME:
        server_key = k.get("key")
        log(f"Server-side key '{SERVER_KEY_NAME}' already exists.")
        break

if not server_key:
    log(f"Creating server-side API key '{SERVER_KEY_NAME}'...")
    key_resp = api_call("POST", f"/api/v1/environments/{client_key}/api-keys/", {
        "name": SERVER_KEY_NAME,
    }, token=token)
    server_key = key_resp.get("key")
    if server_key:
        log(f"Created server-side API key '{SERVER_KEY_NAME}'.")
    else:
        log(f"WARNING: Failed to create server-side API key: {key_resp}")
        sys.exit(0)

log(f"Local server-side key: {server_key[:8]}...")

# --- 6. Create feature flags (skip existing) ---
log("Syncing feature flags...")
desired_flags = parse_flags()
log(f"  Flags from FeatureFlags.java: {desired_flags}")
existing_features = api_call("GET", f"/api/v1/projects/{project_id}/features/", token=token)
feature_list = existing_features.get("results", existing_features) if isinstance(existing_features, dict) else existing_features
existing_names = {f["name"] for f in feature_list}

for flag_name in desired_flags:
    if flag_name in existing_names:
        log(f"  Flag '{flag_name}' already exists, skipping.")
    else:
        resp = api_call("POST", f"/api/v1/projects/{project_id}/features/", {
            "name": flag_name,
            "default_enabled": True,
            "type": "FLAG",
        }, token=token)
        if resp.get("id"):
            log(f"  Created flag '{flag_name}' (default: enabled).")
        else:
            log(f"  WARNING: Failed to create flag '{flag_name}': {resp}")

# --- 7. Create segments for role-based targeting ---
SEGMENTS = [
    {"name": "Team Admins", "description": "Users with TEAM_ADMIN role", "trait": "role", "value": "TEAM_ADMIN"},
    {"name": "Team Editors", "description": "Users with TEAM_EDITOR role", "trait": "role", "value": "TEAM_EDITOR"},
    {"name": "Team Viewers", "description": "Users with TEAM_VIEWER role", "trait": "role", "value": "TEAM_VIEWER"},
]

log("Syncing segments...")
existing_segments = api_call("GET", f"/api/v1/projects/{project_id}/segments/", token=token)
seg_list = existing_segments.get("results", existing_segments) if isinstance(existing_segments, dict) else existing_segments
existing_seg_names = {s["name"] for s in seg_list} if isinstance(seg_list, list) else set()

for seg in SEGMENTS:
    if seg["name"] in existing_seg_names:
        log(f"  Segment '{seg['name']}' already exists, skipping.")
    else:
        resp = api_call("POST", f"/api/v1/projects/{project_id}/segments/", {
            "name": seg["name"],
            "description": seg["description"],
            "project": project_id,
            "rules": [{
                "type": "ALL",
                "rules": [{
                    "type": "ALL",
                    "conditions": [{
                        "property": seg["trait"],
                        "operator": "EQUAL",
                        "value": seg["value"],
                    }],
                }],
                "conditions": [],
            }],
        }, token=token)
        if resp.get("id"):
            log(f"  Created segment '{seg['name']}'.")
        else:
            log(f"  WARNING: Failed to create segment '{seg['name']}': {resp}")

# --- 8. Write key to .env file (for local dev where backend runs on host) ---
if not os.path.isfile(ENV_FILE):
    log(f"WARNING: .env file not found at {ENV_FILE}, skipping key write.")
    sys.exit(0)

with open(ENV_FILE, "r") as f:
    lines = f.readlines()

found = False
updated = False
new_lines = []
for line in lines:
    if line.startswith("FLAGSMITH_SERVER_SIDE_KEY="):
        found = True
        current = line.strip().split("=", 1)[1]
        if current == server_key:
            log(".env already has correct FLAGSMITH_SERVER_SIDE_KEY.")
            new_lines.append(line)
        else:
            new_lines.append(f"FLAGSMITH_SERVER_SIDE_KEY={server_key}\n")
            updated = True
    else:
        new_lines.append(line)

if not found:
    new_lines.append(f"FLAGSMITH_SERVER_SIDE_KEY={server_key}\n")
    updated = True

if updated:
    with open(ENV_FILE, "w") as f:
        f.writelines(new_lines)
    log(f"Wrote FLAGSMITH_SERVER_SIDE_KEY={server_key} to .env.")

log("Done.")
