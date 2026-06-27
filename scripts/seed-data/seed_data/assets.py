"""Small, valid file payloads for document/photo uploads.

The backend validates uploads by magic bytes (URLConnection.guessContentTypeFromStream)
and an allow-list of MIME types: application/pdf, image/jpeg, image/png, image/gif.
These minimal-but-valid blobs satisfy that without bundling real fixtures.
"""

from __future__ import annotations

import base64

# 1x1 baseline JPEG (JFIF). Magic FF D8 FF -> detected as image/jpeg.
_JPEG_B64 = (
    "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAMCAgICAgMCAgIDAwMDBAYEBAQEBAgGBgUGCQgKCgkICQkKDA8M"
    "CgsOCwkJDRENDg8QEBEQCgwSExIQEw8QEBD/2wBDAQMDAwQDBAgEBAgQCwkLEBAQEBAQEBAQEBAQEBAQEBAQ"
    "EBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBD/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAA"
    "AAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEA"
    "AAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCdABmX/9k="
)

JPEG_BYTES = base64.b64decode(_JPEG_B64)


def pdf_bytes(title: str) -> bytes:
    """Return a minimal valid single-page PDF embedding `title` as visible text."""
    safe = "".join(c for c in title if 32 <= ord(c) < 127).replace("(", "").replace(")", "")
    stream = f"BT /F1 18 Tf 72 720 Td ({safe}) Tj ET"
    objects = [
        "<< /Type /Catalog /Pages 2 0 R >>",
        "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
        "/Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>",
        f"<< /Length {len(stream)} >>\nstream\n{stream}\nendstream",
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
    ]

    out = "%PDF-1.4\n"
    offsets = []
    for i, obj in enumerate(objects, start=1):
        offsets.append(len(out.encode("latin-1")))
        out += f"{i} 0 obj\n{obj}\nendobj\n"

    xref_pos = len(out.encode("latin-1"))
    out += f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n"
    for off in offsets:
        out += f"{off:010d} 00000 n \n"
    out += (
        f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\n"
        f"startxref\n{xref_pos}\n%%EOF"
    )
    return out.encode("latin-1")
