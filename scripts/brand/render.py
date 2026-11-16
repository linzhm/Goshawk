#!/usr/bin/env python3
"""Rebuild Goshawk's SVG and platform icons from one vector master.

python3 -m venv /tmp/goshawk-brand
/tmp/goshawk-brand/bin/pip install -r scripts/brand/requirements.txt
/tmp/goshawk-brand/bin/python scripts/brand/render.py
On macOS, iconutil also writes the ICNS package.
"""
from pathlib import Path
import io
import platform
import subprocess
import tempfile

import cairosvg
from PIL import Image
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen

ROOT = Path(__file__).resolve().parents[2]
BRAND = ROOT / "docs/brand"
RES = ROOT / "branding/goshawk/resources/image"
DEPLOY = ROOT / "branding/goshawk/deploy/package"
INK, PAPER, SLATE, AMBER = "#192A35", "#F4F2ED", "#31566A", "#D49A44"
# A goshawk in profile: rounded crown, pronounced supercilium, hooked beak,
# tapered nape. The negative shapes remain open in every one-colour rendition.
OUTLINE = "M29 135 L40 105 C27 87 28 59 43 42 C60 23 86 20 106 30 C116 35 122 43 126 50 L143 56 C155 60 159 69 154 82 L143 72 L127 73 C124 89 113 100 98 105 L96 135 Z"
BROW = "M43 66 C61 46 85 42 110 50 L124 57 L99 55 C77 52 60 58 43 74 Z"
FEATHERS = "M44 94 L79 106 L66 113 L48 106 Z M49 116 L73 124 L62 130 L51 126 Z"


def mark(colour=INK, eye=AMBER):
    return (f'<path fill="{colour}" fill-rule="evenodd" d="{OUTLINE} {BROW} {FEATHERS}"/>'
            f'<path fill="{eye}" d="M105 61 L116 61 L112 67 L106 66 Z"/>')


def svg(body, w=160, h=160):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
            f'viewBox="0 0 {w} {h}"><title>Goshawk</title>{body}</svg>\n')


def save(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text)


def raster(text, path, size):
    path.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(bytestring=text.encode(), write_to=str(path),
                    output_width=size, output_height=size)


font = instantiateVariableFont(TTFont(Path(__file__).with_name("SpaceGrotesk.ttf")), {"wght": 600})
glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
units = font["head"].unitsPerEm


def lettering(text, x, baseline, size, colour=INK):
    paths, cursor = [], 0
    for char in text:
        name = cmap[ord(char)]
        pen = SVGPathPen(glyphs)
        glyphs[name].draw(pen)
        paths.append(f'<path transform="translate({cursor} 0)" d="{pen.getCommands()}"/>')
        cursor += glyphs[name].width
    return (f'<g fill="{colour}" transform="translate({x} {baseline}) scale({size/units} {-size/units})">'
            + "".join(paths) + "</g>")


BRAND.mkdir(parents=True, exist_ok=True)
for name, colour, eye in [("goshawk-mark", INK, AMBER), ("goshawk-mark-mono", INK, INK),
                          ("goshawk-mark-inverse", PAPER, AMBER)]:
    save(BRAND / f"{name}.svg", svg(mark(colour, eye)))

lockup = svg(f'<g transform="translate(0 -2) scale(.7)">{mark()}</g>'
             + lettering("Goshawk", 125, 79, 67), 460, 112)
save(BRAND / "goshawk-lockup.svg", lockup)
save(BRAND / "goshawk-lockup-inverse.svg", svg(
    f'<g transform="translate(0 -2) scale(.7)">{mark(PAPER)}</g>'
    + lettering("Goshawk", 125, 79, 67, PAPER), 460, 112))

icon = svg(f'<rect x="8" y="8" width="240" height="240" rx="51" fill="{INK}"/>'
           f'<g transform="translate(22 17) scale(1.3)">{mark(PAPER)}</g>', 256, 256)
save(BRAND / "goshawk-app-icon.svg", icon)
for name, size in [("goshawk-icon.png", 256), ("goshawk-icon@2x.png", 512)]:
    raster(icon, RES / name, size)
for suffix, size in [("", 128), ("@2x", 256), ("@3x", 384)]:
    raster(svg(mark()), RES / f"goshawk{suffix}.png", size)
for variant, colour in [("black", INK), ("white", "#FFFFFF")]:
    for suffix, size in [("", 16), ("@2x", 32), ("@3x", 48)]:
        raster(svg(mark(colour, colour)), RES / f"goshawk-{variant}-small{suffix}.png", size)
save(RES / "dialog/goshawk.svg", svg(f'<g transform="scale(.3125)">{mark()}</g>', 50, 50))
save(RES / "dialog/goshawk-invert.svg", svg(f'<g transform="scale(.3125)">{mark(PAPER)}</g>', 50, 50))
raster(icon, DEPLOY / "linux/Goshawk.png", 512)
png = cairosvg.svg2png(bytestring=icon.encode(), output_width=256, output_height=256)
Image.open(io.BytesIO(png)).save(DEPLOY / "windows/goshawk.ico", sizes=[(n, n) for n in (16, 24, 32, 48, 64, 128, 256)])
if platform.system() == "Darwin":
    with tempfile.TemporaryDirectory() as tmp:
        iconset = Path(tmp) / "goshawk.iconset"
        for n in (16, 32, 128, 256, 512):
            raster(icon, iconset / f"icon_{n}x{n}.png", n)
            raster(icon, iconset / f"icon_{n}x{n}@2x.png", n * 2)
        subprocess.run(["iconutil", "-c", "icns", str(iconset), "-o", str(DEPLOY / "macos/goshawk.icns")], check=True)

# A compact, shareable identity sheet made from the actual shipped vectors.
board = f'<rect width="1440" height="900" fill="{PAPER}"/>'
board += f'<g transform="translate(70 55) scale(1.55)">{mark()}</g>'
board += lettering("Goshawk", 345, 240, 135)
board += '<text x="352" y="291" font-family="sans-serif" font-size="26" fill="#526773">A clear view of your money.</text>'
board += f'<path d="M90 365 H1350" stroke="{INK}" stroke-opacity=".2"/>'
board += f'<rect x="90" y="412" width="570" height="320" rx="16" fill="{INK}"/>'
board += f'<g transform="translate(113 442) scale(1.45)">{mark(PAPER)}</g>'
board += lettering("Goshawk", 350, 565, 53, PAPER)
board += '<text x="353" y="604" font-family="sans-serif" font-size="17" fill="#B1C1CA">BSV wallet · Development preview</text>'
board += f'<g transform="translate(770 420) scale(.83)">{icon[icon.index("<rect"):icon.rindex("</svg>")]}</g>'
board += '<text x="1060" y="460" font-family="sans-serif" font-size="20" fill="#526773">Goshawk</text>'
for i, (name, colour) in enumerate([("Ink", INK), ("Slate", SLATE), ("Amber", AMBER)]):
    x = 1060 + i * 95
    board += f'<rect x="{x}" y="490" width="72" height="72" rx="8" fill="{colour}"/>'
    board += f'<text x="{x}" y="590" font-family="sans-serif" font-size="15" fill="{INK}">{name}</text>'
    board += f'<text x="{x}" y="612" font-family="monospace" font-size="12" fill="#526773">{colour}</text>'
board += '<text x="90" y="806" font-family="sans-serif" font-size="20" fill="#526773">Independent identity. Built on Sparrow. With thanks to its creators.</text>'
save(BRAND / "identity.svg", svg(board, 1440, 900))
cairosvg.svg2png(bytestring=svg(board, 1440, 900).encode(), write_to=str(BRAND / "identity.png"))
print("Wrote brand SVGs, PNGs, tray icons, ICO and platform icon resources.")
