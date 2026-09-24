# Baked-in fonts for the Gotenberg renderer

Drop **static** font files (`.ttf` / `.otf`) here to bake them into the `buurman-gotenberg`
image — they are copied to `/usr/share/fonts/buurman/` and indexed by fontconfig at build time.

Use this for:

- Static Satoshi weights (`.otf`/`.ttf`), if you prefer system-installed brand fonts over the
  per-request `@font-face` web-font delivery.
- Extra fallback families not available via apt (e.g. `fonts-noto-cjk` for CJK, Noto Arabic/Hebrew
  for future RTL locales).

Do **not** put variable WOFF2 files here — fontconfig does not index WOFF2 reliably. The brand
font lives at `backend/buurman-documents/src/main/resources/fonts/Satoshi-Variable.woff2` and is
shipped to Chromium per request by `GotenbergDocumentRenderer` (it attaches the WOFF2 as a sibling
asset and injects a matching `@font-face` into the HTML head) — not baked into this image.
