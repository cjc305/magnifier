#!/usr/bin/env python3
"""
Magnifier — Play store asset generator.

Produces:
  A. docs/play-store/assets/hi-res-icon-512.png       — Play 512x512 hi-res icon
  B. docs/play-store/assets/feature-graphic-1024x500.png — Play feature graphic
  E. app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
       ic_launcher.webp + ic_launcher_round.webp + ic_launcher_foreground.webp
     — legacy raster fallbacks for API < 26

All visuals derive from the same parametric drawing as the adaptive
vector icon (drawable/ic_launcher_background.xml + ic_launcher_foreground.xml),
so the launcher icon, hi-res icon, feature graphic, and legacy webp all
look the same.

Re-run with:
  python tools/generate_assets.py
"""

from __future__ import annotations
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent

# Amber gradient from launcher background (drawable/ic_launcher_background.xml)
GRAD_START = (0xFF, 0xD5, 0x4F, 0xFF)  # #FFD54F top-left
GRAD_END   = (0xFF, 0x8F, 0x00, 0xFF)  # #FF8F00 bottom-right
FG_WHITE   = (0xFF, 0xF8, 0xEC, 0xFF)  # warm cream — matches NoirPalette.OnSurface

# Vector viewport is 108x108. All foreground coordinates below use that
# coordinate system; the draw functions scale to the target canvas.
VECTOR_VIEW = 108


def diagonal_gradient_bg(size: tuple[int, int]) -> Image.Image:
    """Per-pixel linear gradient from top-left to bottom-right."""
    w, h = size
    img = Image.new("RGBA", size, 0)
    px = img.load()
    denom = max(w + h - 2, 1)
    sr, sg, sb, sa = GRAD_START
    er, eg, eb, ea = GRAD_END
    for y in range(h):
        for x in range(w):
            t = (x + y) / denom
            r = int(sr + (er - sr) * t)
            g = int(sg + (eg - sg) * t)
            b = int(sb + (eb - sb) * t)
            px[x, y] = (r, g, b, 255)
    return img


def draw_magnifier(
    img: Image.Image,
    cx: float,
    cy: float,
    scale: float,
    color: tuple[int, int, int, int] = FG_WHITE,
) -> None:
    """Draw the magnifier glyph (white ring + handle).

    cx, cy are the canvas center; scale = pixels per vector unit. Vector
    geometry is the same as drawable/ic_launcher_foreground.xml:
      lens center at (44, 44), outer r=22, inner r=15
      handle from (61.8, 61.8) to (84.1, 84.1), stroke width 10, round cap.

    The vector viewport center is (54, 54); we offset so the supplied
    (cx, cy) becomes the visual center of the glyph instead.
    """
    draw = ImageDraw.Draw(img)
    # Vector → canvas: point p_canvas = (cx, cy) + (p_vec - (54,54)) * scale
    def vx(x): return cx + (x - VECTOR_VIEW / 2) * scale
    def vy(y): return cy + (y - VECTOR_VIEW / 2) * scale

    # Lens center & radii
    lcx, lcy = vx(44), vy(44)
    outer_r = 22 * scale
    inner_r = 15 * scale
    mid_r = (outer_r + inner_r) / 2
    ring_w = outer_r - inner_r
    draw.ellipse(
        (lcx - mid_r, lcy - mid_r, lcx + mid_r, lcy + mid_r),
        outline=color,
        width=int(round(ring_w)),
    )

    # Handle: stroke width 10 vector units, round caps
    stroke_w = int(round(10 * scale))
    h1 = (vx(61.8), vy(61.8))
    h2 = (vx(84.1), vy(84.1))
    draw.line([h1, h2], fill=color, width=stroke_w)
    # PIL ImageDraw.line doesn't render caps — add filled circles at both ends
    half = stroke_w / 2
    for (hx, hy) in (h1, h2):
        draw.ellipse(
            (hx - half, hy - half, hx + half, hy + half),
            fill=color,
        )


def make_icon(size: int) -> Image.Image:
    """Adaptive-icon-style composite at NxN: amber gradient + magnifier glyph."""
    img = diagonal_gradient_bg((size, size))
    scale = size / VECTOR_VIEW
    draw_magnifier(img, cx=size / 2, cy=size / 2, scale=scale)
    return img


def circle_clip(img: Image.Image) -> Image.Image:
    """Return a copy of img masked to a centered circle (transparent outside)."""
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, w - 1, h - 1), fill=255)
    out = Image.new("RGBA", (w, h), 0)
    out.paste(img, (0, 0), mask)
    return out


# ── A: hi-res icon 512x512 ─────────────────────────────────────────────

def make_hi_res_icon() -> None:
    img = make_icon(512)
    out = ROOT / "docs/play-store/assets/hi-res-icon-512.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out, "PNG", optimize=True)
    print(f"  hi-res icon -> {out} ({out.stat().st_size:,} bytes)")


# ── B: feature graphic 1024x500 ────────────────────────────────────────

def make_feature_graphic() -> None:
    W, H = 1024, 500
    bg = diagonal_gradient_bg((W, H))
    # Magnifier glyph dominates the left third — bigger than v1, vertically centered.
    draw_magnifier(bg, cx=220, cy=H // 2, scale=3.2)

    draw = ImageDraw.Draw(bg)

    sg_path = ROOT / "app/src/main/res/font/space_grotesk.ttf"
    f_en_xl = _font(str(sg_path), 88)
    f_tag   = _font(str(sg_path), 28)
    f_zh    = _font("C:/Windows/Fonts/NotoSansTC-VF.ttf", 110, fallback=f_en_xl)

    text_x = 460
    # Title block centered vertically: title + en + tag total ≈ 280px tall
    # Start at (H - 280) / 2 ≈ 110
    draw.text((text_x, 90),  "數位放大鏡",
              font=f_zh, fill=(255, 248, 236, 255))
    draw.text((text_x, 240), "Magnifier",
              font=f_en_xl, fill=(58, 24, 0, 255))
    # Tagline split into two lines so it fits within W=1024 with 28-px font
    draw.text((text_x, 360), "1× to 10× zoom  •  Flashlight",
              font=f_tag, fill=(58, 24, 0, 220))
    draw.text((text_x, 400), "Fully offline  •  No tracking  •  No ads",
              font=f_tag, fill=(58, 24, 0, 220))

    out = ROOT / "docs/play-store/assets/feature-graphic-1024x500.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    bg.save(out, "PNG", optimize=True)
    print(f"  feature graphic -> {out} ({out.stat().st_size:,} bytes)")


def _font(path: str, size: int, fallback: ImageFont.FreeTypeFont | None = None) -> ImageFont.FreeTypeFont:
    """Best-effort font loader — falls back to default or supplied font."""
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        if fallback is not None:
            return fallback
        return ImageFont.load_default()


# ── E: legacy mipmap webp ──────────────────────────────────────────────

# Standard Android launcher icon densities
DENSITIES = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi":  144,
    "xxxhdpi": 192,
}


def make_legacy_mipmaps() -> None:
    base_dir = ROOT / "app/src/main/res"
    # Render once at the largest size, then downsample with high-quality filter
    master = make_icon(192)
    master_round = circle_clip(master)

    for density, px in DENSITIES.items():
        d = base_dir / f"mipmap-{density}"
        d.mkdir(parents=True, exist_ok=True)

        sq = master.resize((px, px), Image.LANCZOS)
        rd = master_round.resize((px, px), Image.LANCZOS)
        # Foreground sized the same — it represents the inner image used by
        # legacy adaptive icon XML fallbacks that read this resource.
        fg = sq

        # WebP lossless for icons (sharp edges, small palettes — lossless wins
        # over lossy at these sizes)
        sq.save(d / "ic_launcher.webp", "WEBP", lossless=True, quality=100)
        rd.save(d / "ic_launcher_round.webp", "WEBP", lossless=True, quality=100)
        fg.save(d / "ic_launcher_foreground.webp", "WEBP", lossless=True, quality=100)
        print(
            f"  mipmap-{density:<7s} {px:>3d}px  "
            f"sq={(d / 'ic_launcher.webp').stat().st_size:,}  "
            f"rd={(d / 'ic_launcher_round.webp').stat().st_size:,}  "
            f"fg={(d / 'ic_launcher_foreground.webp').stat().st_size:,}"
        )


# ── Driver ─────────────────────────────────────────────────────────────

def main() -> None:
    print("A: hi-res icon 512x512")
    make_hi_res_icon()
    print()
    print("B: feature graphic 1024x500")
    make_feature_graphic()
    print()
    print("E: legacy mipmap webp (5 densities x 3 variants)")
    make_legacy_mipmaps()
    print()
    print("Done.")


if __name__ == "__main__":
    main()
