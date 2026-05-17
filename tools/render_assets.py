#!/usr/bin/env python3
"""One-shot rasterizer for magnifier-app launch assets.

Produces, from the same vector logic as `ic_launcher_foreground.xml` /
`ic_launcher_background.xml`:

  - mipmap-{mdpi..xxxhdpi}/ic_launcher.webp     (square legacy icon)
  - mipmap-{mdpi..xxxhdpi}/ic_launcher_round.webp (round legacy icon)
  - docs/play-store/assets/hi-res-icon-512.png   (Play listing)
  - docs/play-store/assets/feature-graphic-1024x500.png (Play listing)

Run: `python tools/render_assets.py` from repo root.
Requires: Pillow >= 9.

The legacy mipmap webp files are read by Android only on API < 26
(< 0.3% of Play installs today) — adaptive-icon XML in mipmap-anydpi-v26
takes precedence on API 26+. We refresh them anyway so the green-robot
default never ships.
"""
from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

REPO = Path(__file__).resolve().parent.parent
OUT_PLAY = REPO / "docs" / "play-store" / "assets"
OUT_PLAY.mkdir(parents=True, exist_ok=True)

# Densities and base px sizes (matches Android mdpi=48dp baseline)
MIPMAP_SIZES = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

AMBER_LIGHT = (0xFF, 0xD5, 0x4F)   # gradient top-left
AMBER_DARK  = (0xFF, 0x8F, 0x00)   # gradient bottom-right
WHITE       = (255, 255, 255, 255)
TEXT_BROWN  = (0x1C, 0x0A, 0x00)   # deep warm brown for text on amber bg


def make_gradient(w: int, h: int, c1, c2) -> Image.Image:
    """Diagonal linear gradient top-left → bottom-right."""
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        ty = y / h
        for x in range(w):
            t = (x / w + ty) / 2
            r = int(c1[0] * (1 - t) + c2[0] * t)
            g = int(c1[1] * (1 - t) + c2[1] * t)
            b = int(c1[2] * (1 - t) + c2[2] * t)
            px[x, y] = (r, g, b)
    return img


def render_launcher(size: int, shape: str = "square") -> Image.Image:
    """Render magnifier launcher icon at given size with mask applied."""
    bg = make_gradient(size, size, AMBER_LIGHT, AMBER_DARK).convert("RGBA")

    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(fg)
    # Source viewport: 108×108 (matches ic_launcher_foreground.xml)
    s = size / 108.0
    lcx, lcy = 44 * s, 44 * s
    outerR, innerR = 22 * s, 15 * s
    hx1, hy1 = 61.8 * s, 61.8 * s
    hx2, hy2 = 84.1 * s, 84.1 * s
    handleW = max(1, int(10 * s))

    # Lens ring: outer disc minus inner disc (transparent hole)
    d.ellipse((lcx - outerR, lcy - outerR, lcx + outerR, lcy + outerR), fill=WHITE)
    d.ellipse((lcx - innerR, lcy - innerR, lcx + innerR, lcy + innerR), fill=(0, 0, 0, 0))

    # Handle: line + round end-caps (PIL has no native round-cap on line)
    d.line((hx1, hy1, hx2, hy2), fill=WHITE, width=handleW)
    half_w = handleW / 2
    d.ellipse((hx1 - half_w, hy1 - half_w, hx1 + half_w, hy1 + half_w), fill=WHITE)
    d.ellipse((hx2 - half_w, hy2 - half_w, hx2 + half_w, hy2 + half_w), fill=WHITE)

    composed = Image.alpha_composite(bg, fg)

    # Mask: circular for round variant, squircle (rounded square) for square
    mask = Image.new("L", (size, size), 0)
    md = ImageDraw.Draw(mask)
    if shape == "round":
        md.ellipse((0, 0, size, size), fill=255)
    else:
        radius = int(size * 0.22)
        md.rounded_rectangle((0, 0, size, size), radius=radius, fill=255)
    composed.putalpha(mask)
    return composed


def _load_font(path_candidates: list[str], size: int) -> ImageFont.FreeTypeFont:
    for p in path_candidates:
        try:
            return ImageFont.truetype(p, size)
        except (OSError, IOError):
            continue
    return ImageFont.load_default()


def render_feature_graphic() -> Image.Image:
    w, h = 1024, 500
    img = make_gradient(w, h, AMBER_LIGHT, AMBER_DARK).convert("RGBA")
    d = ImageDraw.Draw(img)

    # Left: icon
    icon_size = 280
    icon_x, icon_y = 80, (h - icon_size) // 2
    icon = render_launcher(icon_size, shape="square")
    img.paste(icon, (icon_x, icon_y), icon)

    # Right: text. Use Microsoft JhengHei for zh-TW on Windows;
    # fall back to bundled Space Grotesk for Latin if available.
    zh_candidates = [
        "C:/Windows/Fonts/msjh.ttc",
        "C:/Windows/Fonts/msjhbd.ttc",
        "C:/Windows/Fonts/msyh.ttc",
    ]
    space_grotesk_path = str(REPO / "app" / "src" / "main" / "res" / "font" / "space_grotesk.ttf")

    title_font = _load_font(zh_candidates, 78)
    sub_zh_font = _load_font(zh_candidates, 30)
    sub_en_font = _load_font([space_grotesk_path] + zh_candidates, 26)

    text_x = icon_x + icon_size + 60
    title_y = h // 2 - 90
    d.text((text_x, title_y), "數位放大鏡", fill=TEXT_BROWN, font=title_font)
    d.text((text_x, title_y + 100), "1× → 10× 放大 + 手電筒", fill=TEXT_BROWN, font=sub_zh_font)
    d.text((text_x, title_y + 150), "Fully offline · No ads · Open source", fill=TEXT_BROWN, font=sub_en_font)

    return img


def main() -> None:
    # 1. Mipmap legacy icons (replaces Android Studio green robot for API < 26)
    for density, size in MIPMAP_SIZES.items():
        out_dir = REPO / "app" / "src" / "main" / "res" / f"mipmap-{density}"
        out_dir.mkdir(parents=True, exist_ok=True)

        sq = render_launcher(size, shape="square")
        sq.save(out_dir / "ic_launcher.webp", "webp", quality=95, method=6)

        rd = render_launcher(size, shape="round")
        rd.save(out_dir / "ic_launcher_round.webp", "webp", quality=95, method=6)

        # Old ic_launcher_foreground.webp is dead since adaptive icon now
        # references @drawable/ic_launcher_foreground (vector). Delete to
        # reclaim ~10 KB across all densities.
        old_fg = out_dir / "ic_launcher_foreground.webp"
        if old_fg.exists():
            old_fg.unlink()

        print(f"mipmap-{density}: {size}×{size} ic_launcher.webp + ic_launcher_round.webp written")

    # 2. Play hi-res icon (512×512)
    hires_path = OUT_PLAY / "hi-res-icon-512.png"
    render_launcher(512, shape="square").save(hires_path, "PNG", optimize=True)
    print(f"hi-res icon: {hires_path}")

    # 3. Play feature graphic (1024×500)
    feat_path = OUT_PLAY / "feature-graphic-1024x500.png"
    render_feature_graphic().save(feat_path, "PNG", optimize=True)
    print(f"feature graphic: {feat_path}")


if __name__ == "__main__":
    main()
