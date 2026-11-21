"""README banner and GitHub social preview (1280x640), in the feature graphic's style.

usage: python3 docs/readme_banner.py docs/screenshots docs/banner.svg
then:  rsvg-convert docs/banner.svg -o docs/banner.png && rm docs/banner.svg
"""
import importlib.util, os, sys

SRC, OUT = sys.argv[1], sys.argv[2]

# Reuse the feature graphic's backdrop, logo and phone frame without running it.
spec = importlib.util.spec_from_file_location("assets", os.path.join(os.path.dirname(__file__), "play_assets.py"))
src = open(spec.origin).read().split("\nfeature_graphic()")[0]
ns = {"__file__": spec.origin}
sys.argv = [sys.argv[0], SRC, "/tmp"]
exec(compile(src, spec.origin, "exec"), ns)
logo, phone, embed, DEFS = ns["logo"], ns["phone"], ns["embed"], ns["DEFS"]

W, H = 1280, 640
svg = [
    f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}">', DEFS,
    f'<rect width="{W}" height="{H}" fill="#1C0E1F"/><rect width="{W}" height="{H}" fill="url(#glow)"/>',
    logo(126, 170, 50),
    '<text x="72" y="350" font-family="Bitter" font-weight="700" font-size="132" letter-spacing="-3" fill="url(#title)">GapWise</text>',
    '<text font-family="DM Sans" font-size="38" fill="#E8C9DC"><tspan x="78" y="420">Remember the last time.</tspan><tspan x="78" y="468">Know the next.</tspan></text>',
    '<text x="78" y="548" font-family="DM Sans" font-size="24" fill="#B99AB0">Free and open source · No account · No ads</text>',
    # Two phones behind, one in front, bleeding off the bottom.
    phone(1010, 150, 220, embed("widgets.jpg"), "p3"),
    phone(700, 150, 220, embed("edit.jpg"), "p1"),
    phone(830, 70, 250, embed("home.jpg"), "p2"),
    '</svg>',
]
open(OUT, "w").write("".join(svg))
