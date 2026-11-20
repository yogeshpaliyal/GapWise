"""Gapwise Play feature graphic and store icon, in the same style as Halo's: dark backdrop with a
soft glow, big name and tagline on the left, a phone bleeding off the bottom on the right.
Screenshots come from docs/play_screenshots.py.

usage: python3 docs/play_assets.py docs/screenshots fastlane/metadata/android/en-US/images
then: rsvg-convert featureGraphic.svg -o featureGraphic.png; rsvg-convert icon.svg -o icon.png
"""
import base64, math, os, sys

SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)
SHOT_W, SHOT_H = 1080, 2424
PLUM_DARK, BLUSH = "#3E1546", "#FFB4D2"


def embed(name):
    with open(os.path.join(SRC, name), "rb") as f:
        return "data:image/jpeg;base64," + base64.b64encode(f.read()).decode()


DEFS = '''<defs>
  <radialGradient id="glow" cx="0.78" cy="0.5" r="0.75">
    <stop offset="0" stop-color="#B0479E" stop-opacity="0.55"/>
    <stop offset="1" stop-color="#1C0E1F" stop-opacity="0"/>
  </radialGradient>
  <linearGradient id="title" x1="0" x2="1">
    <stop offset="0" stop-color="#FFFFFF"/><stop offset="1" stop-color="#FFD3E6"/>
  </linearGradient>
</defs>'''


def phone(x, y, w, img, cid):
    h = w * SHOT_H / SHOT_W
    pad = 12
    return (f'<rect x="{x-pad}" y="{y-pad}" width="{w+2*pad}" height="{h+2*pad}" rx="64" fill="#0E080F" stroke="#5A2E5E" stroke-width="4"/>'
            f'<clipPath id="{cid}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="54"/></clipPath>'
            f'<image href="{img}" x="{x}" y="{y}" width="{w}" height="{h}" clip-path="url(#{cid})" preserveAspectRatio="xMidYMid slice"/>')


def logo(cx, cy, r):
    """Blush sun holding a ring with a gap, and a dot for the next time."""
    pts = []
    for i in range(180):
        t = 2 * math.pi * i / 180 - math.pi / 2
        rr = r * (1 + 0.075 * math.cos(9 * (t + math.pi / 2)))
        pts.append(f"{cx + rr*math.cos(t):.1f},{cy + rr*math.sin(t):.1f}")
    ring = r * 0.52
    a0, a1 = math.radians(-5), math.radians(265)
    x0, y0 = cx + ring * math.cos(a0), cy + ring * math.sin(a0)
    x1, y1 = cx + ring * math.cos(a1), cy + ring * math.sin(a1)
    dx, dy = cx + r * 0.54 * math.cos(math.radians(-50)), cy + r * 0.54 * math.sin(math.radians(-50))
    return (f'<polygon points="{" ".join(pts)}" fill="{BLUSH}"/>'
            f'<path d="M{x0:.1f},{y0:.1f} A{ring:.1f},{ring:.1f} 0 1,1 {x1:.1f},{y1:.1f}" fill="none" stroke="{PLUM_DARK}" stroke-width="{r*0.25:.1f}" stroke-linecap="round"/>'
            f'<circle cx="{dx:.1f}" cy="{dy:.1f}" r="{r*0.15:.1f}" fill="{PLUM_DARK}"/>')


def feature_graphic():
    W, H = 1024, 500
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">', DEFS,
           f'<rect width="{W}" height="{H}" fill="#1C0E1F"/><rect width="{W}" height="{H}" fill="url(#glow)"/>',
           logo(104, 132, 40),
           '<text x="62" y="282" font-family="Bitter" font-weight="700" font-size="112" letter-spacing="-3" fill="url(#title)">Gapwise</text>',
           '<text font-family="DM Sans" font-size="32" fill="#E8C9DC"><tspan x="66" y="340">Remember the last time.</tspan><tspan x="66" y="382">Know the next.</tspan></text>',
           # phone bleeding off the bottom edge
           phone(660, 64, 290, embed("home.jpg"), "fg"),
           '</svg>']
    open(os.path.join(OUT, "featureGraphic.svg"), "w").write("".join(svg))


def icon():
    # Full-bleed square; Play applies its own mask. Matches the adaptive launcher icon (80% mark).
    svg = ('<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">'
           f'<rect width="512" height="512" fill="{PLUM_DARK}"/>' + logo(256, 256, 116) + '</svg>')
    open(os.path.join(OUT, "icon.svg"), "w").write(svg)


feature_graphic()
icon()
