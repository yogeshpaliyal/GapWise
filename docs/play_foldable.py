"""GapWise Play screenshots for foldables, in the same style as docs/play_screenshots.py.

Play wants, whenever any foldable screenshot is uploaded: at least one 1:1 and one 4:3 unfolded
screenshot, and one 10:16 folded portrait one. This makes exactly those three:
  1-unfolded-1x1.png  2106 x 2106
  2-unfolded-4x3.png  2688 x 2016
  3-folded-10x16.png  1260 x 2016

usage: python3 docs/play_foldable.py docs/screenshots docs/foldable
then:  for f in docs/foldable/*.svg; do rsvg-convert "$f" -o "${f%.svg}.png" && rm "$f"; done
"""
import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import play_screenshots as ps

ps.SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)


def save(name, w, h, body):
    ps.W, ps.H = w, h
    open(os.path.join(OUT, f"{name}.svg"), "w").write(ps.head() + body() + "</svg>")


def phones(names, w, y, gap, total_w, prefix):
    """A row of phones, centred; the outer ones sit a little lower for depth."""
    row = len(names) * w + (len(names) - 1) * gap
    x = (total_w - row) / 2
    out = []
    n = len(names)
    middle = {(n - 1) // 2, n // 2}
    for i, name in enumerate(names):
        drop = 0 if i in middle else 70
        out.append(ps.phone(x, y + drop, w, name, f"{prefix}{i}"))
        x += w + gap
    return "".join(out)


# 1:1 unfolded: the promise, three screens side by side.
def square():
    t, _ = ps.headline(["Remember the last time", "you did anything"], 132, 260, mark=(0, "last time"))
    s = t + phones(["widgets.jpg", "home.jpg", "edit.jpg"], 560, 560, 90, 2106, "sq")
    s += ps.hand("one tap logs it!", 300, 1990, 96, -6)
    return s


# 4:3 unfolded (landscape): four screens, from the home screen to settings.
def wide():
    t, _ = ps.headline(["One tap now.", "A nudge when it's due."], 120, 230, eyebrow="GAPWISE", mark=(1, "nudge"))
    s = t + phones(["widgets.jpg", "home.jpg", "edit.jpg", "sort.jpg"], 540, 560, 90, 2688, "wd")
    return s


# 10:16 folded portrait: the outer screen, one phone.
def folded():
    t, _ = ps.headline(["Remember the", "last time"], 120, 230, mark=(1, "last time"))
    s = t + ps.phone(190, 520, 880, "home.jpg", "fd")
    s += ps.pill(630, 1830, "No streaks. No guilt.")
    return s


save("1-unfolded-1x1", 2106, 2106, square)
save("2-unfolded-4x3", 2688, 2016, wide)
save("3-folded-10x16", 1260, 2016, folded)
