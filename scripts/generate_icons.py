import os
import math
from PIL import Image, ImageDraw, ImageFilter

def create_app_icon(size=512):
    # 512x512 base master icon
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Rounded squircle background
    radius = int(size * 0.22)
    # Background gradient: from #0D0E15 to #1A1C2E
    base_bg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    base_draw = ImageDraw.Draw(base_bg)
    base_draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=(18, 20, 32, 255))
    
    # Gradient overlay
    grad = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        r = int(14 + (28 - 14) * (y / size))
        g = int(16 + (32 - 16) * (y / size))
        b = int(26 + (54 - 26) * (y / size))
        grad_draw = ImageDraw.Draw(grad)
        grad_draw.line([(0, y), (size, y)], fill=(r, g, b, 255))
    
    # Mask gradient with squircle
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=255)
    base_bg.paste(grad, (0, 0), mask)

    # Ambient radial glow inside squircle
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    center = (size // 2, size // 2)
    for rad in range(int(size * 0.45), 0, -4):
        alpha = int(45 * (1 - rad / (size * 0.45)))
        glow_draw.ellipse([center[0] - rad, center[1] - rad, center[0] + rad, center[1] + rad], fill=(99, 102, 241, alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(12))
    base_bg.paste(glow, (0, 0), mask)

    # Subtle border rim with cyan-purple gradient
    rim = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    rim_draw = ImageDraw.Draw(rim)
    rim_draw.rounded_rectangle([1, 1, size - 2, size - 2], radius=radius, outline=(0, 242, 254, 70), width=3)
    base_bg.paste(rim, (0, 0), mask)

    # Center Vinyl Record / Audio Disc
    disc_radius = int(size * 0.30)
    disc = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    disc_draw = ImageDraw.Draw(disc)
    
    # Outer disc base (Dark metallic obsidian)
    disc_draw.ellipse([center[0] - disc_radius, center[1] - disc_radius,
                       center[0] + disc_radius, center[1] + disc_radius],
                      fill=(15, 17, 26, 255), outline=(0, 242, 254, 180), width=4)

    # Concentric vinyl ridges
    for r_offset in [16, 28, 40, 52, 64]:
        cr = disc_radius - r_offset
        if cr > 30:
            disc_draw.ellipse([center[0] - cr, center[1] - cr,
                               center[0] + cr, center[1] + cr],
                              outline=(35, 42, 65, 160), width=1)

    # Center vibrant gradient hub
    hub_radius = int(size * 0.12)
    hub_grad = Image.new("RGBA", (hub_radius * 2, hub_radius * 2), (0, 0, 0, 0))
    hub_draw = ImageDraw.Draw(hub_grad)
    for y in range(hub_radius * 2):
        t = y / (hub_radius * 2)
        r = int(0 * (1 - t) + 124 * t)
        g = int(242 * (1 - t) + 58 * t)
        b = int(254 * (1 - t) + 237 * t)
        hub_draw.line([(0, y), (hub_radius * 2, y)], fill=(r, g, b, 255))
    
    hub_mask = Image.new("L", (hub_radius * 2, hub_radius * 2), 0)
    ImageDraw.Draw(hub_mask).ellipse([0, 0, hub_radius * 2 - 1, hub_radius * 2 - 1], fill=255)
    
    disc.paste(hub_grad, (center[0] - hub_radius, center[1] - hub_radius), hub_mask)

    # Play Icon triangle in center (Pure crisp white with shadow)
    pw = int(size * 0.075)
    ph = int(size * 0.085)
    p_x1 = center[0] - int(pw * 0.4)
    p_y1 = center[1] - ph // 2
    p_x2 = p_x1 + pw
    p_y2 = center[1]
    p_x3 = p_x1
    p_y3 = center[1] + ph // 2

    # Shadow
    disc_draw.polygon([(p_x1 + 1, p_y1 + 2), (p_x2 + 1, p_y2 + 2), (p_x3 + 1, p_y3 + 2)], fill=(0, 0, 0, 100))
    # Shape
    disc_draw.polygon([(p_x1, p_y1), (p_x2, p_y2), (p_x3, p_y3)], fill=(255, 255, 255, 255))

    # Equalizer wave arcs on the left and right of the disc
    # Left waves (Cyan)
    for i, (offset_x, bar_h) in enumerate([(-185, 34), (-165, 58), (-145, 82), (-125, 42)]):
        x = center[0] + offset_x
        disc_draw.line([(x, center[1] - bar_h // 2), (x, center[1] + bar_h // 2)], fill=(0, 242, 254, 210), width=6)
    
    # Right waves (Purple/Pink)
    for i, (offset_x, bar_h) in enumerate([(125, 42), (145, 82), (165, 58), (185, 34)]):
        x = center[0] + offset_x
        color = (139, 92, 246, 210) if i < 2 else (255, 0, 122, 210)
        disc_draw.line([(x, center[1] - bar_h // 2), (x, center[1] + bar_h // 2)], fill=color, width=6)

    # Glass highlight reflection arc on top-left of vinyl
    highlight = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    hl_draw = ImageDraw.Draw(highlight)
    hl_draw.arc([center[0] - disc_radius + 4, center[1] - disc_radius + 4,
                 center[0] + disc_radius - 4, center[1] + disc_radius - 4],
                start=200, end=320, fill=(255, 255, 255, 90), width=3)
    
    base_bg.paste(disc, (0, 0), disc)
    base_bg.paste(highlight, (0, 0), highlight)

    return base_bg

def create_round_icon(base_icon):
    size = base_icon.size[0]
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([2, 2, size - 3, size - 3], fill=255)
    round_img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_img.paste(base_icon, (0, 0), mask)
    # Add subtle border ring
    ImageDraw.Draw(round_img).ellipse([2, 2, size - 3, size - 3], outline=(0, 242, 254, 90), width=3)
    return round_img

# Output directories
res_dir = os.path.join(os.getcwd(), "app", "src", "main", "res")
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

master_icon = create_app_icon(512)
master_round = create_round_icon(master_icon)

# Save play store high-res icon
docs_dir = os.path.join(os.getcwd(), "docs")
os.makedirs(docs_dir, exist_ok=True)
master_icon.save(os.path.join(docs_dir, "app_icon.png"), "PNG")

for folder, dim in densities.items():
    target_dir = os.path.join(res_dir, folder)
    os.makedirs(target_dir, exist_ok=True)
    
    resized_icon = master_icon.resize((dim, dim), Image.Resampling.LANCZOS)
    resized_icon.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")
    
    resized_round = master_round.resize((dim, dim), Image.Resampling.LANCZOS)
    resized_round.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

print("Successfully generated all launcher icons in all densities!")
