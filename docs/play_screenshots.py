"""GapWise Play screenshots, in the same light 'callout' style as Halo's: eyebrow + big headline
with a highlighter swoosh, phone bleeding off the bottom, zoomed callout cards with soft shadows,
handwritten notes. Colours and type are GapWise's (plum, blush, Bitter, DM Sans).

usage: python3 docs/play_screenshots.py docs/screenshots fastlane/metadata/android/en-US/images/phoneScreenshots
then render each SVG: for f in out/*.svg; do rsvg-convert "$f" -o "${f%.svg}.png"; done
Screenshots: home, widgets, edit (1080x2424) and settings (1080 wide, any height), as .jpg.
"""
import base64, os, sys
from PIL import Image, ImageFont

SRC = OUT = None  # set by main, or by docs/play_foldable.py
W, H = 1080, 1920
INK, QUIET, PLUM, BLUSH, HANDC, BG, GRID = "#1F1A1E", "#6E5F6C", "#6B2A72", "#FFB4D2", "#2F64A0", "#FBF6FA", "#F0E4EC"
HEAD_FONT = "Bitter"
BODY_FONT = "DM Sans"
HAND = "TeX Gyre Chorus"
_HEAD_TTF = "/usr/share/fonts/truetype/google-fonts/ofl/bitter/Bitter[wght].ttf"
_BODY_TTF = "/usr/share/fonts/truetype/google-fonts/ofl/dmsans/DMSans[opsz,wght].ttf"

_cache = {}
def img(name):
    if name not in _cache:
        path = os.path.join(SRC, name)
        with open(path, "rb") as f:
            _cache[name] = ("data:image/jpeg;base64," + base64.b64encode(f.read()).decode(), Image.open(path).size)
    return _cache[name]

def head():
    grid = "".join(f'<line x1="{x}" y1="0" x2="{x}" y2="{H}"/>' for x in range(60, W, 60)) + \
           "".join(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}"/>' for y in range(60, H, 60))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">
<defs>
  <filter id="shadow" x="-30%" y="-30%" width="160%" height="160%">
    <feGaussianBlur in="SourceAlpha" stdDeviation="22"/><feOffset dy="18" result="b"/>
    <feComponentTransfer><feFuncA type="linear" slope="0.20"/></feComponentTransfer>
    <feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
  <filter id="soft" x="-30%" y="-30%" width="160%" height="160%">
    <feGaussianBlur in="SourceAlpha" stdDeviation="10"/><feOffset dy="8" result="b"/>
    <feComponentTransfer><feFuncA type="linear" slope="0.18"/></feComponentTransfer>
    <feMerge><feMergeNode/><feMergeNode in="SourceGraphic"/></feMerge>
  </filter>
</defs>
<rect width="{W}" height="{H}" fill="{BG}"/>
<g stroke="{GRID}" stroke-width="2">{grid}</g>'''

def text_w(s, size, spacing=None):
    """Advance width of Bitter Bold, including the headline's tight letter-spacing."""
    spacing = -size * 0.02 if spacing is None else spacing
    font = ImageFont.truetype(_HEAD_TTF, int(size))
    try:
        font.set_variation_by_axes([700])
    except Exception:
        pass
    return font.getlength(s) + spacing * len(s)

def headline(lines, size, y0, eyebrow=None, mark=None):
    """mark = (line index, words) to underline with a blush highlighter swoosh."""
    out = []
    y = y0
    if eyebrow:
        out.append(f'<text x="{W/2}" y="{y}" text-anchor="middle" font-family="{BODY_FONT}" font-weight="800" font-size="36" letter-spacing="5" fill="{PLUM}">{eyebrow}</text>')
        y += size * 1.05
    for i, line in enumerate(lines):
        if mark and mark[0] == i:
            lw = text_w(line, size)
            start = W / 2 - lw / 2 + text_w(line[: line.index(mark[1])], size)
            ww = text_w(mark[1], size)
            out.append(f'<path d="M{start-6:.0f},{y+18} Q{start+ww/2:.0f},{y+4} {start+ww+10:.0f},{y+14}" fill="none" stroke="{BLUSH}" stroke-width="24" stroke-linecap="round" opacity="0.85"/>')
        out.append(f'<text x="{W/2}" y="{y}" text-anchor="middle" font-family="{HEAD_FONT}" font-weight="700" font-size="{size}" letter-spacing="{-size*0.02:.1f}" fill="{INK}">{line}</text>')
        y += size * 1.04
    return "".join(out), y

def phone(x, y, w, name, cid):
    data, (sw, sh) = img(name)
    h = w * sh / sw
    b = 16
    return (f'<g filter="url(#shadow)"><rect x="{x-b}" y="{y-b}" width="{w+2*b}" height="{h+2*b}" rx="{78+b}" fill="#141014"/></g>'
            f'<clipPath id="{cid}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="78"/></clipPath>'
            f'<image href="{data}" x="{x}" y="{y}" width="{w}" height="{h}" clip-path="url(#{cid})" preserveAspectRatio="none"/>'
            f'<circle cx="{x+w/2}" cy="{y+30}" r="13" fill="#0A0A0C"/>')

def callout(name, crop, x, y, w, rot, cid, rx=44, pad=0, fill="#FFFFFF"):
    """A zoomed piece of a screenshot as a floating rounded card."""
    data, (sw, sh) = img(name)
    cx, cy, cw, ch = crop
    h = w * ch / cw
    return (f'<g transform="rotate({rot} {x+w/2} {y+h/2})"><g filter="url(#shadow)">'
            f'<rect x="{x-pad}" y="{y-pad}" width="{w+2*pad}" height="{h+2*pad}" rx="{rx+pad}" fill="{fill}"/></g>'
            f'<clipPath id="{cid}"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}"/></clipPath>'
            f'<svg x="{x}" y="{y}" width="{w}" height="{h}" viewBox="{cx} {cy} {cw} {ch}" preserveAspectRatio="none" clip-path="url(#{cid})">'
            f'<image href="{data}" width="{sw}" height="{sh}"/></svg></g>')

def hand(text, x, y, size, rot, anchor="start", color=HANDC):
    return (f'<text x="{x}" y="{y}" transform="rotate({rot} {x} {y})" text-anchor="{anchor}" font-family="{HAND}" '
            f'font-size="{size}" fill="{color}" stroke="{color}" stroke-width="{size*0.025:.1f}">{text}</text>')

def arrow(d, color=HANDC):
    return f'<path d="{d}" fill="none" stroke="{color}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>'

def mark(cx, cy, r):
    """The GapWise logo: blush sun holding a ring with a gap, and a dot for the next time."""
    import math
    pts = []
    for i in range(180):
        t = 2 * math.pi * i / 180 - math.pi / 2
        rr = r * (1 + 0.075 * math.cos(9 * (t + math.pi / 2)))
        pts.append(f"{cx + rr*math.cos(t):.1f},{cy + rr*math.sin(t):.1f}")
    ring_r = r * 0.52
    a0, a1 = math.radians(-5), math.radians(265)
    x0, y0 = cx + ring_r * math.cos(a0), cy + ring_r * math.sin(a0)
    x1, y1 = cx + ring_r * math.cos(a1), cy + ring_r * math.sin(a1)
    dx, dy = cx + r * 0.54 * math.cos(math.radians(-50)), cy + r * 0.54 * math.sin(math.radians(-50))
    return (f'<g filter="url(#shadow)"><circle cx="{cx}" cy="{cy}" r="{r*1.25:.0f}" fill="#3E1546"/></g>'
            f'<polygon points="{" ".join(pts)}" fill="{BLUSH}"/>'
            f'<path d="M{x0:.1f},{y0:.1f} A{ring_r:.1f},{ring_r:.1f} 0 1,1 {x1:.1f},{y1:.1f}" fill="none" stroke="#3E1546" stroke-width="{r*0.25:.1f}" stroke-linecap="round"/>'
            f'<circle cx="{dx:.1f}" cy="{dy:.1f}" r="{r*0.15:.1f}" fill="#3E1546"/>')

def badge(n, cx, cy):
    return (f'<g filter="url(#soft)"><circle cx="{cx}" cy="{cy}" r="30" fill="{INK}"/></g>'
            f'<text x="{cx}" y="{cy+12}" text-anchor="middle" font-family="{BODY_FONT}" font-weight="700" font-size="32" fill="#FFFFFF">{n}</text>')

def pill(cx, y, label, dark=True):
    font = ImageFont.truetype(_BODY_TTF, 34)
    w = font.getlength(label) * 1.05 + 150
    x = cx - w / 2
    bg, fg = ("#2A1630", "#FFFFFF") if dark else ("#FFFFFF", INK)
    return (f'<g filter="url(#shadow)"><rect x="{x}" y="{y}" width="{w}" height="104" rx="52" fill="{bg}"/></g>'
            f'<circle cx="{x+58}" cy="{y+52}" r="22" fill="none" stroke="{BLUSH}" stroke-width="7"/><circle cx="{x+58}" cy="{y+52}" r="7" fill="#FFFFFF"/>'
            f'<text x="{x+100}" y="{y+64}" font-family="{BODY_FONT}" font-weight="700" font-size="34" fill="{fg}">{label}</text>')

def save(n, body):
    open(os.path.join(OUT, f"{n}.svg"), "w").write(head() + body + "</svg>")

def main():
    global SRC, OUT
    SRC, OUT = sys.argv[1], sys.argv[2]
    os.makedirs(OUT, exist_ok=True)
    # 1. Hero: the promise, and one card as a big floating callout with the three steps.
    t, _ = headline(["Remember the", "last time"], 128, 250, mark=(1, "last time"))
    s = t
    s += callout("home.jpg", (42, 772, 482, 534), 380, 660, 620, 4, "c1a", rx=64)
    s += badge(1, 110, 850) + hand("do it,", 165, 870, 92, -6)
    s += badge(2, 110, 1040) + hand("tap it,", 165, 1060, 92, -6)
    s += badge(3, 110, 1230) + hand("relax!", 165, 1250, 92, -6)
    s += arrow("M330,1330 Q430,1420 520,1360 M494,1350 l27,8 l-9,25")
    s += pill(W / 2, 1640, "No streaks. No guilt.")
    save(1, s)

    # 2. Widgets: the home screen in a phone, the tile widget zoomed out to the right.
    t, _ = headline(["One tap from", "your home screen"], 92, 170, eyebrow="WIDGETS", mark=(1, "home screen"))
    PX, PW = 70, 620
    s = t + phone(PX, 520, PW, "widgets.jpg", "p2")
    s += callout("widgets.jpg", (50, 765, 572, 532), 540, 760, 480, 4, "c2a", rx=48, pad=14)
    s += hand("one tap", 880, 1420, 96, -8, anchor="middle")
    s += hand("logs it!", 890, 1520, 96, -8, anchor="middle")
    s += arrow("M880,1330 Q910,1290 880,1250 M858,1268 l22,-20 l20,22")
    save(2, s)

    # 3. Shapes: the legend from settings, big, with a note.
    t, _ = headline(["Know what's due", "at a glance"], 92, 170, eyebrow="SHAPES", mark=(1, "glance"))
    s = t
    s += callout("settings.jpg", (0, 1160, 1080, 640), 120, 470, 840, -2, "c3a", rx=56, pad=16)
    s += hand("the sun means", 140, 1340, 100, -8)
    s += hand("it's about time!", 230, 1460, 100, -8)
    s += arrow("M800,1300 Q880,1230 840,1150 M818,1172 l22,-24 l22,24")
    s += callout("home.jpg", (42, 772, 996, 534), 160, 1540, 760, 2, "c3b", rx=40, pad=10)
    save(3, s)

    # 4. Track anything: the editor in a phone, the emoji picker zoomed.
    t, _ = headline(["Haircuts to", "oil changes"], 92, 170, eyebrow="TRACK ANYTHING", mark=(1, "oil changes"))
    s = t + phone(PX, 520, PW, "edit.jpg", "p4")
    s += callout("edit.jpg", (30, 570, 960, 350), 470, 900, 560, -4, "c4a", rx=40, pad=14)
    s += hand("any emoji,", 880, 1300, 88, -8, anchor="middle")
    s += hand("any colour!", 890, 1400, 88, -8, anchor="middle")
    s += arrow("M880,1200 Q910,1160 880,1120 M858,1138 l22,-20 l20,22")
    save(4, s)

    # 5. Private: promise cards, and the logo.
    t, _ = headline(["Your data", "stays yours"], 110, 190, eyebrow="PRIVATE", mark=(1, "yours"))
    s = t
    rows = [("No account, no ads", "Nothing to sign up for, nothing tracked", -3),
            ("Backs up to your Drive", "A private folder only GapWise can see", 2),
            ("Open source", "Read every line on GitHub", -2)]
    y = 640
    for title, body, rot in rows:
        s += (f'<g transform="rotate({rot} {W/2} {y+110})"><g filter="url(#shadow)"><rect x="110" y="{y}" width="860" height="220" rx="56" fill="#FFFFFF"/></g>'
              f'<circle cx="230" cy="{y+110}" r="54" fill="{PLUM}"/><path d="M204,{y+110} l18,20 l34,-38" fill="none" stroke="#FFFFFF" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>'
              f'<text x="320" y="{y+100}" font-family="{BODY_FONT}" font-weight="700" font-size="46" fill="{INK}">{title}</text>'
              f'<text x="320" y="{y+156}" font-family="{BODY_FONT}" font-size="34" fill="{QUIET}">{body}</text></g>')
        y += 290
    s += hand("promise!", 640, 1610, 120, -10)
    s += mark(230, 1640, 74)
    save(5, s)


if __name__ == "__main__":
    main()
