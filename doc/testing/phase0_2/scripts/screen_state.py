#!/usr/bin/env python3
"""Phase 0.2 screen-state analyzer. Prints feature summary for a screenshot.
Usage: python3 screen_state.py <png>
Output format (parseable):
  sky=0.000 dirt=0.500 hot=5 buttons=330,390 worldentries=1 loading=0 title=1
"""
import sys
from PIL import Image

path = sys.argv[1]
im = Image.open(path).convert("RGB")
w, h = im.size
px = im.load()

def frac(y0, y1, pred, stepx=4, stepy=2):
    n = t = 0
    for y in range(int(h * y0), int(h * y1), stepy):
        for x in range(0, w, stepx):
            p = px[x, y]
            t += 1
            if pred(p):
                n += 1
    return n / max(1, t)

sky = frac(0, 0.33, lambda p: p[2] > p[0] + 10 and p[2] > p[1] + 10 and p[2] > 80)
dirt = frac(0, 0.5, lambda p: 15 <= p[0] <= 95 and 10 <= p[1] <= 75)
dark = frac(0.88, 1.0, lambda p: sum(p) < 200)
bright = frac(0, 1, lambda p: sum(p) > 550)
blue = frac(0.4, 0.9, lambda p: p[2] > 150 and p[2] > p[0] + 40 and p[2] > p[1] + 30)

# button centers: rows in x 200..1100 with 'face' color (95..145 grayish)
rows = []
for y in range(120, h - 10):
    face = sum(
        1
        for x in range(200, 1100, 3)
        if (lambda p: 95 <= p[0] <= 150 and abs(p[0] - p[1]) <= 20)(px[x, y])
    )
    if face > 50:
        rows.append(y)
clusters = []
for y in rows:
    if not clusters or y - clusters[-1][-1] > 8:
        clusters.append([y])
    else:
        clusters[-1].append(y)
buttons = ",".join(str(sum(c) // len(c)) for c in clusters if len(c) >= 5)

# world thumbnail boxes: large blue rectangles (world icons) in center area
blue_boxes = 0
row_blue = {}
for y in range(100, h - 40, 2):
    cnt = sum(1 for x in range(200, 1100, 3) if px[x, y][2] > 130 and px[x, y][2] > px[x, y][0] + 40)
    if cnt > 20:
        row_blue[y] = cnt
boxes = 0
prev = None
for y in sorted(row_blue):
    if prev is not None and y - prev > 30:
        boxes += 1
    prev = y
if row_blue:
    boxes += 1

# 'loading' signature: mostly uniform dark/black screen
loading = 1 if dark > 0.9 and sky < 0.05 and bright < 0.03 else 0
# title/menu signature: dirt dominant, some buttons
title = 1 if dirt > 0.7 and (buttons or bright < 0.2) and sky < 0.1 else 0

print(
    "sky=%.3f dirt=%.3f hot=%.3f dark=%.3f bright=%.3f blue=%.3f buttons=%s worldthumb=%d loading=%d title=%d"
    % (sky, dirt, dark, dark, bright, blue, buttons, boxes, loading, title)
)
