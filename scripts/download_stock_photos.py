#!/usr/bin/env python3
"""Download royalty-free stock photos from Unsplash for demo data."""

import json
import os
import sys
import time
import urllib.request
import urllib.error

BASE_DIR = os.path.join(
    os.path.dirname(__file__),
    "..",
    "backend",
    "buurman-demo-data",
    "src",
    "main",
    "resources",
    "demo",
    "photos",
)

# Search queries mapped to directory names
CATEGORIES = {
    "exteriors": "house exterior building facade",
    "living-rooms": "living room interior design",
    "kitchens": "modern kitchen interior",
    "bathrooms": "modern bathroom interior",
    "bedrooms": "bedroom interior design",
    "offices": "modern office workspace",
    "retail": "retail store shop interior",
    "warehouses": "warehouse industrial building",
    "agricultural": "farm agricultural land barn",
    "mixed-use": "mixed use building street view",
}

PHOTOS_PER_CATEGORY = 6


def search_unsplash(query, per_page=12):
    """Search Unsplash internal API for photos."""
    url = f"https://unsplash.com/napi/search/photos?query={urllib.parse.quote(query)}&per_page={per_page}"
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())


def download_photo(photo_url, dest_path):
    """Download a photo from Unsplash CDN."""
    # Build a cropped, compressed URL
    base_url = photo_url.split("?")[0]
    dl_url = f"{base_url}?w=800&h=600&fit=crop&auto=format&q=80"
    req = urllib.request.Request(dl_url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        data = resp.read()
    with open(dest_path, "wb") as f:
        f.write(data)
    return len(data)


def main():
    import urllib.parse

    total = 0
    for category, query in CATEGORIES.items():
        cat_dir = os.path.normpath(os.path.join(BASE_DIR, category))
        os.makedirs(cat_dir, exist_ok=True)

        # Remove existing files
        for f in os.listdir(cat_dir):
            os.remove(os.path.join(cat_dir, f))

        print(f"\n--- {category} (query: '{query}') ---")

        try:
            data = search_unsplash(query, per_page=20)
        except Exception as e:
            print(f"  ERROR searching: {e}")
            continue

        results = data.get("results", [])
        # Filter to only images.unsplash.com (skip premium/plus)
        usable = []
        for r in results:
            raw_url = r.get("urls", {}).get("raw", "")
            if "images.unsplash.com" in raw_url:
                usable.append(raw_url)
            if len(usable) >= PHOTOS_PER_CATEGORY:
                break

        if len(usable) < PHOTOS_PER_CATEGORY:
            # If not enough non-premium, also include plus.unsplash.com
            for r in results:
                raw_url = r.get("urls", {}).get("raw", "")
                if raw_url and raw_url not in usable:
                    usable.append(raw_url)
                if len(usable) >= PHOTOS_PER_CATEGORY:
                    break

        for i, url in enumerate(usable[:PHOTOS_PER_CATEGORY], 1):
            name = category.rstrip("s") if not category.endswith("ses") else category.replace("-", "_")
            # Simpler naming
            prefix = category.rstrip("s")
            if category == "warehouses":
                prefix = "warehouse"
            elif category == "offices":
                prefix = "office"
            elif category == "living-rooms":
                prefix = "living-room"
            elif category == "mixed-use":
                prefix = "mixed-use"
            elif category == "bathrooms":
                prefix = "bathroom"
            elif category == "bedrooms":
                prefix = "bedroom"
            elif category == "kitchens":
                prefix = "kitchen"
            elif category == "exteriors":
                prefix = "exterior"
            elif category == "agricultural":
                prefix = "agricultural"
            elif category == "retail":
                prefix = "retail"

            dest = os.path.join(cat_dir, f"{prefix}-{i}.jpg")
            try:
                size = download_photo(url, dest)
                print(f"  [{i}/{PHOTOS_PER_CATEGORY}] {prefix}-{i}.jpg ({size // 1024}KB)")
                total += 1
            except Exception as e:
                print(f"  [{i}/{PHOTOS_PER_CATEGORY}] FAILED: {e}")

        # Be nice to the API
        time.sleep(0.5)

    print(f"\nDone! Downloaded {total} photos total.")


if __name__ == "__main__":
    main()
