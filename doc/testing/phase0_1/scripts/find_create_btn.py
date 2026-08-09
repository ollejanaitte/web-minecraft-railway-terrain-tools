from PIL import Image
im = Image.open("doc/testing/phase0_1/screenshots/_select_sp.png").convert("RGB")
w, h = im.size
print("size", w, h)
for yy in range(470, 520, 2):
    regs = []
    inb = False
    start = 0
    for x in range(w):
        r, g, b = im.getpixel((x, yy))
        isb = 90 <= r <= 150 and abs(r - g) <= 25 and abs(g - b) <= 25 and r > 80
        if isb and not inb:
            inb = True
            start = x
        if (not isb) and inb:
            inb = False
            regs.append((start, x - 1, (start + x) // 2))
    if inb:
        regs.append((start, w - 1, (start + w) // 2))
    # keep wide regions only
    regs = [r for r in regs if r[1] - r[0] > 40]
    if regs:
        print("y", yy, "regs", regs)
