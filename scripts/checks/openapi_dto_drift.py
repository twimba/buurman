#!/usr/bin/env python3
"""Compare every OpenAPI schema's property names against the Java DTO of the same name.

Why this exists: the controllers implement openapi-generator interfaces
(`PropertyController implements PropertiesApi`) but bind the HAND-WRITTEN DTOs in
`buurman-common`. So code generation verifies paths and verbs, never field names.
The spec and the DTOs can therefore drift silently, and the first sign is a 400 at
runtime with a field the client never sent -- or, on the read side, a JSON key the
frontend reads that the server never emits, which fails silently as `undefined`.

Jackson has no naming strategy configured, so a JSON key equals the record
component name unless @JsonProperty overrides it.

Usage: python3 scripts/checks/openapi_dto_drift.py [--quiet]
Exit 1 if any drift is found.
"""
import re
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "openapi" / "app.yaml"
DTO_ROOTS = [
    ROOT / "backend" / "buurman-common" / "src" / "main" / "java" / "com" / "buurman" / "dto",
]

# Schemas that deliberately have no hand-written DTO counterpart.
IGNORE_SCHEMAS = set()


def split_top_level(text):
    """Split a record's component list on commas that are not nested."""
    parts, depth, buf = [], 0, []
    for ch in text:
        if ch in "<([{":
            depth += 1
        elif ch in ">)]}":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(buf))
            buf = []
        else:
            buf.append(ch)
    if "".join(buf).strip():
        parts.append("".join(buf))
    return parts


def component_name(component):
    """Take the parameter name from one record component, honouring @JsonProperty."""
    explicit = re.search(r'@JsonProperty\(\s*"([^"]+)"', component)
    if explicit:
        return explicit.group(1)
    # Strip annotations (with any parenthesised args) then take the last identifier.
    cleaned = re.sub(r"@\w+(\s*\([^()]*(\([^()]*\))?[^()]*\))?", " ", component)
    ids = re.findall(r"\b([A-Za-z_]\w*)\b", cleaned)
    return ids[-1] if ids else None


def strip_comments(source):
    """Remove // and /* */ comments.

    Required, not cosmetic: a comment such as "(see PropertyService#createProperty)"
    contains parentheses that unbalance the brace matching below and silently
    truncate the component list, which makes real fields look missing.
    """
    source = re.sub(r"/\*.*?\*/", " ", source, flags=re.S)
    source = re.sub(r"//[^\n]*", " ", source)
    return source


def parse_record(source):
    """Return the component names of the first record declaration, or None."""
    source = strip_comments(source)
    m = re.search(r"\brecord\s+(\w+)\s*\(", source)
    if not m:
        return None, None
    start = m.end() - 1
    depth = 0
    for i in range(start, len(source)):
        if source[i] == "(":
            depth += 1
        elif source[i] == ")":
            depth -= 1
            if depth == 0:
                body = source[start + 1 : i]
                break
    else:
        return m.group(1), None
    names = [component_name(c) for c in split_top_level(body)]
    return m.group(1), [n for n in names if n]


def main():
    quiet = "--quiet" in sys.argv
    spec = yaml.safe_load(SPEC.read_text())
    schemas = spec.get("components", {}).get("schemas", {}) or {}

    dtos = {}
    for root in DTO_ROOTS:
        for path in root.rglob("*.java"):
            name, comps = parse_record(path.read_text())
            if name and comps:
                dtos[name] = (comps, path.relative_to(ROOT))

    problems = []
    compared = 0
    for schema_name, schema in sorted(schemas.items()):
        if schema_name in IGNORE_SCHEMAS or not isinstance(schema, dict):
            continue
        if schema_name not in dtos:
            continue
        props = schema.get("properties")
        if not isinstance(props, dict):
            continue
        comps, path = dtos[schema_name]
        compared += 1
        spec_keys, dto_keys = set(props), set(comps)
        only_spec = sorted(spec_keys - dto_keys)
        only_dto = sorted(dto_keys - spec_keys)
        if only_spec or only_dto:
            problems.append((schema_name, path, only_spec, only_dto))

    if not quiet:
        print(f"Compared {compared} OpenAPI schemas against their Java DTOs.")
    for schema_name, path, only_spec, only_dto in problems:
        print(f"\n{schema_name}  ({path})")
        if only_spec:
            print(f"  in spec, NOT in DTO  -> client sends/reads a key the server ignores: {only_spec}")
        if only_dto:
            print(f"  in DTO, NOT in spec  -> server field the client never learns about: {only_dto}")
    if problems:
        print(f"\n{len(problems)} schema(s) drifted.")
    elif not quiet:
        print("OK - every compared schema matches its DTO.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
