# Buurman marketing website

Static site served by Cloudflare Workers (`wrangler.toml`). No build step — the
files in this directory are what ships.

## Run it locally

```bash
make website                 # http://localhost:8088, opens your browser
make website PORT=9000       # pick another port
make website-preview         # serve it through wrangler, exactly as Cloudflare will
```

`make website` runs `scripts/serve-website.py`, which mimics Cloudflare's
`html_handling`: `/support` is the page and `/support.html` 307s to it, exactly
as the deployed site behaves. Use `make website-preview` before a deploy if you
changed `.assetsignore` or anything about 404 handling.

## URLs

The site is served from the apex, `https://buurman.io`. Workers Assets serves
pages without the `.html` extension, so internal links, canonicals and
`sitemap.xml` all use `/support`, not `/support.html` — linking to the `.html`
form costs a 307 and makes canonicals point at a redirect.

## Layout

```
index.html        Landing page: hero, trust strip, how-it-works, screenshot
                  showcase, features, pricing, CTA
changelog.html    Public release notes
support.html      Support channels + FAQ (FAQPage structured data)
contact.html      Contact form (mailto-based, see assets/js/contact-form.js)
privacy.html terms.html refund.html
robots.txt sitemap.xml
.assetsignore     Files excluded from the Cloudflare deploy

assets/css/       main.css (shared) + home.css, pages.css, changelog.css
assets/js/        main.js (scroll reveal + demo-click analytics),
                  pricing.js (billing toggle), contact-form.js, konami.js
assets/fonts/     Satoshi (self-hosted, preloaded)
assets/logo/      Favicons, OG images, logo-wordmark.png (header logo)
assets/screenshots/  Real app screenshots, AVIF + JPEG fallback
```

## Screenshots

`assets/screenshots/app-*.{avif,jpg}` are real captures of the demo workspace at
`app.buurman.io/login?demo=true`, not mockups. To refresh them:

1. Open the demo, set the viewport to **1680×1020** (dashboard) or **1240×800**
   (the four showcase screens), at 2× device pixel ratio.
2. Hide scrollbars so they don't appear in the capture:
   `*{scrollbar-width:none} *::-webkit-scrollbar{width:0;height:0}`
3. Capture, then convert:
   ```bash
   sips -s format jpeg -s formatOptions 80 raw.png --out app-<name>.jpg
   sips -s format avif -s formatOptions 70 raw.png --out app-<name>.avif
   ```
4. Keep the `width`/`height` attributes in `index.html` in sync with the real
   pixel dimensions, or you reintroduce layout shift.

The mobile hero shot (`app-dashboard-mobile.*`) is captured at 390×844 @2× and
cropped to 760px wide.

## Demo link analytics

Every link into the demo carries `data-demo-cta="<placement>"`. `main.js` turns a
click into a GA4 `demo_open` event with that placement, so you can see which
position actually drives trials. Add the attribute to any new demo link.

## Deploy

```bash
npx wrangler deploy          # from this directory
```
