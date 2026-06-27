# Baked-in fonts for the Gotenberg renderer

Drop **static** font files (`.ttf` / `.otf`) here to bake them into the `buurman-gotenberg`
image — they are copied to `/usr/share/fonts/buurman/` and indexed by fontconfig at build time.

Use this for:

- Static Satoshi weights (`.otf`/`.ttf`), if you prefer system-installed brand fonts over the
  per-request `@font-face` web-font delivery.
- Extra fallback families not available via apt (e.g. `fonts-noto-cjk` for CJK, Noto Arabic/Hebrew
  for future RTL locales).

Do **not** put variable WOFF2 files here — fontconfig does not index WOFF2 reliably. The brand
font (`Satoshi-Variable.woff2`) is delivered to Chromium as an `@font-face` web font by
`GotenbergDocumentRenderer`/the booklet templates instead.
