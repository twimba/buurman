#!/usr/bin/env python3
"""
Bundle split OpenAPI source files into a single app.yaml.

Reads openapi/src/app.yaml (root with $ref path entries + schemas),
resolves path $refs by inlining content from openapi/src/paths/*.yaml,
and writes the bundled result to openapi/app.yaml.

Usage: python3 scripts/bundle_openapi.py
"""

import re
import os
import sys
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_ROOT = os.path.join(BASE_DIR, 'openapi', 'src', 'app.yaml')
OUTPUT = os.path.join(BASE_DIR, 'openapi', 'app.yaml')


def resolve_path_ref(ref_value, src_dir):
    """Resolve a $ref like 'paths/foo.yaml#/key' relative to src_dir.

    Returns the content lines (at indent 4) to inline under the path entry.
    """
    # Parse: 'paths/foo.yaml#/key'
    match = re.match(r"^'?([^#']+)#/([^']+)'?$", ref_value)
    if not match:
        raise ValueError(f"Cannot parse $ref: {ref_value}")

    file_path = os.path.join(src_dir, match.group(1))
    key = match.group(2)

    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Referenced file not found: {file_path}")

    # Read the file and find the key
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Find the key at indent 0
    key_pattern = re.compile(rf'^{re.escape(key)}:\s*$')
    start_idx = None
    for i, line in enumerate(lines):
        if key_pattern.match(line):
            start_idx = i + 1
            break

    if start_idx is None:
        raise KeyError(f"Key '{key}' not found in {file_path}")

    # Collect all lines until the next key at indent 0 or EOF
    content_lines = []
    for i in range(start_idx, len(lines)):
        line = lines[i]
        # Next key at indent 0 (non-blank line with no leading spaces)
        if line.strip() and not line[0].isspace():
            break
        content_lines.append(line)

    # Remove trailing blank lines
    while content_lines and content_lines[-1].strip() == '':
        content_lines.pop()

    return content_lines


def main():
    src_dir = os.path.dirname(SRC_ROOT)

    with open(SRC_ROOT, 'r') as f:
        lines = f.readlines()

    output_lines = []
    i = 0
    refs_resolved = 0

    while i < len(lines):
        line = lines[i]

        # Check if this is a path $ref line (4 spaces + $ref: 'paths/...')
        ref_match = re.match(r"^    \$ref: (.+)$", line)
        if ref_match:
            ref_value = ref_match.group(1).strip()
            # Only resolve path refs (not schema refs)
            if 'paths/' in ref_value:
                try:
                    content = resolve_path_ref(ref_value, src_dir)
                    # Re-indent content: source is at indent 2, need indent 4
                    for content_line in content:
                        if content_line.strip() == '':
                            output_lines.append('\n')
                        else:
                            output_lines.append('  ' + content_line)
                    refs_resolved += 1
                    i += 1
                    continue
                except (ValueError, FileNotFoundError, KeyError) as e:
                    print(f"ERROR: {e}", file=sys.stderr)
                    return 1

        output_lines.append(line)
        i += 1

    with open(OUTPUT, 'w') as f:
        f.writelines(output_lines)

    print(f"Bundled {refs_resolved} path refs into {OUTPUT}")
    print(f"Output: {len(output_lines)} lines")
    return 0


if __name__ == '__main__':
    exit(main())
