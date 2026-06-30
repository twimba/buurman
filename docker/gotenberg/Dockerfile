# Custom Gotenberg image — headless-Chromium PDF renderer for buurman booklets.
#
# Adds Noto fonts so every supported locale (incl. Greek `el`) renders without tofu, and curl for
# the container healthcheck. The brand font (Satoshi) is a variable WOFF2 that fontconfig does not
# index reliably; it is therefore delivered per render request as an @font-face web font by the
# backend renderer (GotenbergDocumentRenderer), which Chromium loads natively. Drop any STATIC
# brand/fallback fonts (Satoshi *.ttf/*.otf, Noto CJK, …) into ./fonts to bake them in as well.
FROM gotenberg/gotenberg:8

USER root

# Noto Sans covers Latin + extended diacritics (de/nordic/pl), Greek and Cyrillic — the no-tofu
# fallback family referenced after 'Satoshi' in the booklet CSS font stack.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl fonts-noto-core fonts-noto-ui-core \
    && rm -rf /var/lib/apt/lists/*

# Optional static fonts baked into the image (see header note).
COPY fonts/ /usr/share/fonts/buurman/

RUN fc-cache -f

USER gotenberg
