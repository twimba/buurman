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


def spec_schemas(text):
    """{schema name: [direct property names]} for components.schemas.

    Hand-rolled rather than via PyYAML so this runs on a bare CI runner with no
    pip install, exactly like scripts/bundle_openapi.py.
    Only keys indented 8 spaces inside a schema's `properties:` block count, so
    a nested object's own properties are not mistaken for the parent's.
    """
    schemas = {}
    current = None
    in_props = False
    for line in text.split("\n"):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        indent = len(line) - len(line.lstrip())
        m = re.match(r"^ {4}([A-Za-z_]\w*):\s*$", line)
        if m:
            current = m.group(1)
            schemas.setdefault(current, [])
            in_props = False
            continue
        if current is None:
            continue
        if indent <= 4 and line.strip().endswith(":") and indent < 4:
            current = None
            in_props = False
            continue
        if re.match(r"^ {6}properties:\s*$", line):
            in_props = True
            continue
        if in_props and indent <= 6:
            in_props = False
        if in_props:
            pm = re.match(r"^ {8}([A-Za-z_]\w*):", line)
            if pm:
                schemas[current].append(pm.group(1))
    return schemas


def main():
    quiet = "--quiet" in sys.argv
    raw = SPEC.read_text()
    schemas = {k: {"properties": {p: {} for p in v}} for k, v in spec_schemas(raw).items() if v}

    # A simple class name can occur more than once -- the app and the backoffice each have
    # their own NotificationStatsResponse. This spec describes the app API, so a DTO under a
    # `backoffice` package serves the other spec and must not stand in for the app's DTO.
    # Keying on the simple name alone silently picked one of the two, and because the
    # backoffice record happened to match this spec, a real mismatch in the app's record
    # was reported as OK for as long as both existed.
    candidates = {}
    for root in DTO_ROOTS:
        for path in root.rglob("*.java"):
            if "backoffice" in path.relative_to(root).parts:
                continue
            name, comps = parse_record(path.read_text())
            if name and comps:
                candidates.setdefault(name, []).append((comps, path.relative_to(ROOT)))

    problems = []
    ambiguous = []
    compared = 0
    for schema_name, schema in sorted(schemas.items()):
        if schema_name in IGNORE_SCHEMAS or not isinstance(schema, dict):
            continue
        if schema_name not in candidates:
            continue
        props = schema.get("properties")
        if not isinstance(props, dict):
            continue
        # Two DTOs left with the same name and different fields: there is no way to know
        # which one the spec means, so comparing against either would be a coin toss
        # reported as a verdict. Say so instead of guessing.
        found = candidates[schema_name]
        if len({tuple(c) for c, _ in found}) > 1:
            ambiguous.append((schema_name, [p for _, p in found]))
            continue
        comps, path = found[0]
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
    for schema_name, paths in ambiguous:
        print(f"\n{schema_name}  -- ambiguous, not verified")
        print("  several DTOs share this name with different fields; rename one or add a")
        print("  @Schema/schemaMapping so the spec names exactly one:")
        for path in paths:
            print(f"    {path}")
    if problems or ambiguous:
        print(f"\n{len(problems)} schema(s) drifted, {len(ambiguous)} ambiguous.")
    elif not quiet:
        print("OK - every compared schema matches its DTO.")
    return 1 if problems or ambiguous else 0


if __name__ == "__main__":
    sys.exit(main())
