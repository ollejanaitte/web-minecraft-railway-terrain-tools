#!/usr/bin/env python3
"""Detect Minecraft-style menu buttons (gray rounded rectangles) + screen state.
Usage: python3 analyze_buttons.py <png>
Output: state=<state> sky=.. dirt=.. dark=.. loading=.. buttons=<y1,y2,..> worldthumbs=<n>
"""
import sys
from PIL import Image
import numpy as np

path = sys.argv[1]
im = Image.open(path).convert("RGB")
w, h = im.size
arr = np.array(im).astype(int)
R, G, B = arr[:, :, 0], arr[:, :, 1], arr[:, :, 2]

def frac(ymask, pred):
    n = t = 0
    yy = np.arange(h)[ymask]
    if len(yy) == 0:
        return 0.0
    for y in yy[::2]:
        for x in range(0, w, 4):
            r, g, b = R[y, x], G[y, x], B[y, x]
            t += 1
            if pred(r, g, b):
                n += 1
    return n / max(1, t)

top = np.zeros(h, dtype=bool); top[:int(h * 0.33)] = True
bot = np.zeros(h, dtype=bool); bot[int(h * 0.88):] = True
sky = frac(top, lambda r, g, b: b > r + 10 and b > g + 10 and b > 80)
dirt = frac(top, lambda r, g, b: 15 <= r <= 95 and 10 <= g <= 75)
dark = frac(np.full(h, True), lambda r, g, b: sum((r, g, b)) < 200)
bright = frac(np.full(h, True), lambda r, g, b: sum((r, g, b)) > 500)

# Button detection: rows in x 300..980 with 'face' gray (95..150, channels close)
rows = []
for y in range(120, h - 10):
    cnt = 0
    for x in range(300, 980, 3):
        r, g, b = R[y, x], G[y, x], B[y, x]
        if 95 <= r <= 150 and abs(r - g) <= 20 and abs(g - b) <= 25:
            cnt += 1
    if cnt > 40:
        rows.append(y)

clusters = []
for y in rows:
    if not clusters or y - clusters[-1][-1] > 8:
        clusters.append([y])
    else:
        clusters[-1].append(y)
buttons = []
for c in clusters:
    if len(c) >= 6:
        y0, y1 = c[0], c[-1]
        # width check: gray pixels spread horizontally
        cnt = 0
        for x in range(300, 980, 3):
            hit = False
            for y in range(y0, y1 + 1, 2):
                r, g, b = R[y, x], G[y, x], B[y, x]
                if 95 <= r <= 150 and abs(r - g) <= 20 and abs(g - b) <= 25:
                    hit = True
                    break
            if hit:
                cnt += 1
        if cnt > 30:
            buttons.append((y0 + y1) // 2)

# world thumbnail boxes: distinct large blue-ish blocks in middle area
blue_frac = frac(np.full(h, True), lambda r, g, b: b > 120 and b > r + 40 and b > g + 30)
loading = 1 if dark > 0.9 and sky < 0.05 and dirt < 0.15 and bright < 0.03 else 0
state = "unknown"
if loading:
    state = "loading"
elif sky > 0.5 and dirt < 0.1:
    state = "menu-clouds"
elif dirt > 0.5:
    state = "dirt-screen"
elif sky > 0.15 and dark < 0.6:
    state = "in-world"
print(
    "state=%s sky=%.3f dirt=%.3f dark=%.3f bright=%.3f blue=%.3f buttons=%s worldthumb=%d"
    % (state, sky, dirt, dark, bright, blue_frac, ",".join(map(str, buttons)), int(blue_frac > 0.05 and 0.4 * h > len(rows) > 5))
)
