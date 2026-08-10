from PIL import Image
import sys

path = sys.argv[1]
im = Image.open(path).convert("RGB")
w, h = im.size
sky = sum(
    1
    for y in range(8, 110, 5)
    for x in range(0, w, 18)
    if (lambda p: p[2] > p[0] + 12 and p[2] > 100)(im.getpixel((x, y)))
)
dirt = sum(
    1
    for y in range(8, 110, 5)
    for x in range(0, w, 18)
    if (lambda p: 15 <= p[0] <= 90 and 10 <= p[1] <= 70)(im.getpixel((x, y)))
)
N = max(1, ((110 - 8) // 5) * (w // 18))
hot = sum(
    1
    for y in range(h - 60, h - 8, 2)
    if sum(1 for x in range(w // 3, 2 * w // 3, 4) if sum(im.getpixel((x, y))) < 140) > 25
)
rows = []
for y in range(120, h - 10):
    face = sum(
        1
        for x in range(200, 1100, 3)
        if (lambda p: 95 <= p[0] <= 145 and abs(p[0] - p[1]) <= 20)(im.getpixel((x, y)))
    )
    if face > 50:
        rows.append(y)
clusters = []
for y in rows:
    if not clusters or y - clusters[-1][-1] > 8:
        clusters.append([y])
    else:
        clusters[-1].append(y)
centers = [sum(c) // len(c) for c in clusters if len(c) >= 5]
print(
    "sky=%.3f dirt=%.3f hot=%d buttons=%s"
    % (sky / N, dirt / N, hot, ",".join(map(str, centers)))
)
