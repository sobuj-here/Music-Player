import os
import math
from PIL import Image, ImageDraw, ImageFont

screenshots_dir = os.path.join(os.getcwd(), "docs", "screenshots")
os.makedirs(screenshots_dir, exist_ok=True)

WIDTH, HEIGHT = 720, 1280
FONT_TITLE_SIZE = 34
FONT_SUB_SIZE = 22
FONT_BODY_SIZE = 18
FONT_SMALL_SIZE = 14

def draw_header_bar(draw, title, has_back=False):
    # Top app bar
    draw.rectangle([0, 0, WIDTH, 100], fill=(18, 20, 32))
    draw.line([(0, 100), (WIDTH, 100)], fill=(35, 40, 60), width=1)
    if has_back:
        # Back arrow
        draw.line([(40, 55), (60, 40)], fill=(255, 255, 255), width=3)
        draw.line([(40, 55), (60, 70)], fill=(255, 255, 255), width=3)
        draw.line([(40, 55), (75, 55)], fill=(255, 255, 255), width=3)
        x_title = 95
    else:
        x_title = 32
    draw.text((x_title, 42), title, fill=(255, 255, 255))

def draw_mini_player(draw):
    y = HEIGHT - 110
    draw.rounded_rectangle([16, y, WIDTH - 16, y + 84], radius=20, fill=(28, 32, 50), outline=(0, 242, 254, 80), width=1)
    # Album art thumbnail
    draw.rounded_rectangle([28, y + 12, 28 + 60, y + 72], radius=12, fill=(99, 102, 241))
    draw.polygon([(52, y + 34), (68, y + 42), (52, y + 50)], fill=(255, 255, 255))
    # Title & Artist
    draw.text((104, y + 20), "Cyberpunk Odyssey", fill=(255, 255, 255))
    draw.text((104, y + 46), "Synthwave Collective • 03:42", fill=(160, 170, 200))
    # Play/Pause Circle
    draw.ellipse([WIDTH - 90, y + 17, WIDTH - 40, y + 67], fill=(0, 242, 254))
    draw.polygon([(WIDTH - 70, y + 32), (WIDTH - 56, y + 42), (WIDTH - 70, y + 52)], fill=(15, 17, 26))

def render_now_playing():
    img = Image.new("RGB", (WIDTH, HEIGHT), (14, 16, 26))
    draw = ImageDraw.Draw(img)
    
    # Top bar
    draw_header_bar(draw, "Now Playing", has_back=True)

    # Album artwork center box with glow
    art_size = 480
    art_x = (WIDTH - art_size) // 2
    art_y = 150
    # Outer glow shadow
    for offset in range(16, 0, -2):
        draw.rounded_rectangle([art_x - offset, art_y - offset, art_x + art_size + offset, art_y + art_size + offset],
                               radius=36, fill=(15 + offset * 2, 20 + offset * 3, 45 + offset * 5))
    
    # Main album art card
    draw.rounded_rectangle([art_x, art_y, art_x + art_size, art_y + art_size], radius=32, fill=(22, 26, 42), outline=(0, 242, 254, 120), width=2)
    # Inner graphical artwork representation
    draw.ellipse([art_x + 90, art_y + 90, art_x + art_size - 90, art_y + art_size - 90], fill=(12, 14, 22), outline=(139, 92, 246, 200), width=3)
    draw.ellipse([art_x + 180, art_y + 180, art_x + art_size - 180, art_y + art_size - 180], fill=(0, 242, 254))
    
    # Track title & artist
    info_y = 680
    draw.text((WIDTH // 2 - 140, info_y), "Neon Nights (VIP Mix)", fill=(255, 255, 255))
    draw.text((WIDTH // 2 - 100, info_y + 40), "Retro Horizon • Synth City", fill=(150, 165, 195))

    # Favorite icon
    draw.text((WIDTH - 80, info_y + 10), "★", fill=(0, 242, 254))

    # Seeker Bar
    seek_y = 800
    draw.rounded_rectangle([48, seek_y, WIDTH - 48, seek_y + 8], radius=4, fill=(40, 46, 70))
    progress_w = int((WIDTH - 96) * 0.45)
    draw.rounded_rectangle([48, seek_y, 48 + progress_w, seek_y + 8], radius=4, fill=(0, 242, 254))
    draw.ellipse([48 + progress_w - 8, seek_y - 4, 48 + progress_w + 8, seek_y + 12], fill=(255, 255, 255))
    
    # Timestamps
    draw.text((48, seek_y + 18), "02:15", fill=(140, 155, 185))
    draw.text((WIDTH - 105, seek_y + 18), "-02:45", fill=(140, 155, 185))

    # Main Playback Controls
    ctrl_y = 920
    # Shuffle button
    draw.text((64, ctrl_y + 18), "⇄", fill=(0, 242, 254))
    # Previous button
    draw.polygon([(200, ctrl_y + 28), (225, ctrl_y + 12), (225, ctrl_y + 44)], fill=(255, 255, 255))
    draw.line([(196, ctrl_y + 12), (196, ctrl_y + 44)], fill=(255, 255, 255), width=3)
    # Play / Pause hero button
    draw.ellipse([WIDTH // 2 - 46, ctrl_y - 12, WIDTH // 2 + 46, ctrl_y + 80], fill=(0, 242, 254))
    draw.polygon([(WIDTH // 2 - 14, ctrl_y + 12), (WIDTH // 2 + 18, ctrl_y + 34), (WIDTH // 2 - 14, ctrl_y + 56)], fill=(12, 14, 22))
    # Next button
    draw.polygon([(520, ctrl_y + 28), (495, ctrl_y + 12), (495, ctrl_y + 44)], fill=(255, 255, 255))
    draw.line([(524, ctrl_y + 12), (524, ctrl_y + 44)], fill=(255, 255, 255), width=3)
    # Repeat button
    draw.text((WIDTH - 90, ctrl_y + 18), "↻", fill=(140, 155, 185))

    # Bottom Auxiliary Controls (Speed, Equalizer, Queue, Sleep Timer)
    bottom_y = 1090
    draw.rounded_rectangle([36, bottom_y, WIDTH - 36, bottom_y + 74], radius=24, fill=(22, 26, 42), outline=(35, 42, 65), width=1)
    
    draw.text((70, bottom_y + 24), "1.0x Speed", fill=(0, 242, 254))
    draw.text((250, bottom_y + 24), "Equalizer", fill=(200, 210, 235))
    draw.text((430, bottom_y + 24), "Sleep: 30m", fill=(200, 210, 235))
    draw.text((580, bottom_y + 24), "Queue (48)", fill=(0, 242, 254))

    return img

def render_library():
    img = Image.new("RGB", (WIDTH, HEIGHT), (14, 16, 26))
    draw = ImageDraw.Draw(img)
    
    # Search bar & Title
    draw.rectangle([0, 0, WIDTH, 170], fill=(18, 20, 32))
    draw.text((32, 24), "Music Player", fill=(255, 255, 255))
    # Settings Icon
    draw.text((WIDTH - 60, 26), "⚙", fill=(180, 190, 220))
    
    # Search field
    draw.rounded_rectangle([24, 76, WIDTH - 24, 134], radius=16, fill=(28, 32, 50), outline=(45, 52, 80), width=1)
    draw.text((46, 92), "🔍 Search songs, albums, artists...", fill=(120, 135, 165))

    # 8-Tab row
    tabs = ["Songs", "Albums", "Artists", "Folders", "Playlists", "Favorites", "Recent"]
    tab_x = 24
    for i, tab in enumerate(tabs[:5]):
        is_active = (i == 0)
        tw = 100
        if is_active:
            draw.rounded_rectangle([tab_x, 150, tab_x + tw, 185], radius=12, fill=(0, 242, 254))
            draw.text((tab_x + 18, 158), tab, fill=(12, 14, 22))
        else:
            draw.rounded_rectangle([tab_x, 150, tab_x + tw, 185], radius=12, fill=(24, 28, 44))
            draw.text((tab_x + 18, 158), tab, fill=(160, 175, 205))
        tab_x += tw + 12

    # Songs count row
    draw.text((32, 210), "All Tracks (142 songs)", fill=(140, 155, 185))
    draw.text((WIDTH - 150, 210), "Sort: Date Added ↓", fill=(0, 242, 254))

    # Song list items
    songs = [
        ("Midnight Resonance", "Celestial Echoes • 04:12", True),
        ("Starlight Highway", "Neon Pulse • 03:45", False),
        ("Quantum Horizon", "Aether Wave • 05:20", False),
        ("Cosmic Drifter", "Solaris Dream • 02:58", False),
        ("Hyperdrive Anthem", "Overdrive 88 • 04:36", False),
        ("Subterranean Beats", "Deep Bass Division • 03:19", False),
        ("Solar Flare", "Nova Eclipse • 04:50", False),
        ("Infinite Loop", "Binary Pulse • 03:33", False),
    ]

    list_y = 250
    for title, sub, is_playing in songs:
        if is_playing:
            draw.rounded_rectangle([16, list_y, WIDTH - 16, list_y + 80], radius=16, fill=(24, 30, 48), outline=(0, 242, 254, 100), width=1)
        # Art placeholder
        fill_col = (0, 242, 254) if is_playing else (35, 42, 65)
        draw.rounded_rectangle([28, list_y + 12, 28 + 56, list_y + 68], radius=12, fill=fill_col)
        draw.polygon([(48, list_y + 30), (64, list_y + 40), (48, list_y + 50)], fill=(255, 255, 255) if not is_playing else (12, 14, 22))
        
        # Text
        draw.text((102, list_y + 16), title, fill=(0, 242, 254) if is_playing else (255, 255, 255))
        draw.text((102, list_y + 44), sub, fill=(140, 155, 185))
        
        # Options menu dots
        draw.text((WIDTH - 50, list_y + 26), "⋮", fill=(160, 175, 205))
        list_y += 88

    # Bottom Mini-Player
    draw_mini_player(draw)

    return img

def render_equalizer():
    img = Image.new("RGB", (WIDTH, HEIGHT), (14, 16, 26))
    draw = ImageDraw.Draw(img)
    draw_header_bar(draw, "Equalizer & Effects", has_back=True)

    # Master Toggle Card
    draw.rounded_rectangle([24, 120, WIDTH - 24, 200], radius=20, fill=(22, 26, 42), outline=(35, 42, 65), width=1)
    draw.text((48, 145), "Enable Sound Processing", fill=(255, 255, 255))
    # Active Switch
    draw.rounded_rectangle([WIDTH - 100, 142, WIDTH - 48, 178], radius=18, fill=(0, 242, 254))
    draw.ellipse([WIDTH - 76, 146, WIDTH - 52, 174], fill=(12, 14, 22))

    # Presets Row
    draw.text((32, 225), "AUDIO PRESETS", fill=(140, 155, 185))
    presets = [("Flat", False), ("Rock", True), ("Classical", False), ("Electronic", False), ("Jazz", False)]
    px = 24
    for name, active in presets:
        pw = 110
        if active:
            draw.rounded_rectangle([px, 255, px + pw, 298], radius=14, fill=(139, 92, 246))
            draw.text((px + 24, 267), name, fill=(255, 255, 255))
        else:
            draw.rounded_rectangle([px, 255, px + pw, 298], radius=14, fill=(24, 28, 44), outline=(38, 45, 70), width=1)
            draw.text((px + 24, 267), name, fill=(160, 175, 205))
        px += pw + 16

    # 5-Band Equalizer Graphic Box
    draw.rounded_rectangle([24, 330, WIDTH - 24, 850], radius=24, fill=(20, 24, 38), outline=(35, 42, 65), width=1)
    draw.text((44, 355), "Graphic Frequency Bands (5-Band)", fill=(255, 255, 255))

    bands = [
        ("60 Hz", 0.75, "+4.5 dB"),
        ("230 Hz", 0.60, "+2.0 dB"),
        ("910 Hz", 0.45, "-1.0 dB"),
        ("3.6 kHz", 0.70, "+3.5 dB"),
        ("14 kHz", 0.85, "+6.0 dB")
    ]
    
    bx = 60
    b_width = 80
    slider_top = 420
    slider_bottom = 740

    for freq, val, db in bands:
        # Frequency label
        draw.text((bx + 10, slider_top), freq, fill=(140, 155, 185))
        
        # Track line
        track_x = bx + b_width // 2
        draw.line([(track_x, slider_top + 40), (track_x, slider_bottom)], fill=(38, 45, 70), width=8)
        
        # Active track fill
        thumb_y = int(slider_bottom - (slider_bottom - (slider_top + 40)) * val)
        draw.line([(track_x, thumb_y), (track_x, slider_bottom)], fill=(0, 242, 254), width=8)
        
        # Thumb handle
        draw.ellipse([track_x - 14, thumb_y - 14, track_x + 14, thumb_y + 14], fill=(255, 255, 255), outline=(0, 242, 254), width=3)
        
        # Value tag
        draw.text((bx + 8, slider_bottom + 18), db, fill=(0, 242, 254))
        
        bx += 125

    # Enhancements: Bass Boost & Virtualizer
    draw.rounded_rectangle([24, 880, WIDTH - 24, 1020], radius=20, fill=(22, 26, 42), outline=(35, 42, 65), width=1)
    draw.text((44, 905), "Bass Boost Level (80%)", fill=(255, 255, 255))
    draw.rounded_rectangle([44, 945, WIDTH - 44, 955], radius=5, fill=(38, 45, 70))
    draw.rounded_rectangle([44, 945, int(44 + (WIDTH - 88) * 0.8), 955], radius=5, fill=(139, 92, 246))

    draw.rounded_rectangle([24, 1040, WIDTH - 24, 1180], radius=20, fill=(22, 26, 42), outline=(35, 42, 65), width=1)
    draw.text((44, 1065), "3D Virtualizer Surround (65%)", fill=(255, 255, 255))
    draw.rounded_rectangle([44, 1105, WIDTH - 44, 1115], radius=5, fill=(38, 45, 70))
    draw.rounded_rectangle([44, 1105, int(44 + (WIDTH - 88) * 0.65), 1115], radius=5, fill=(0, 242, 254))

    return img

def render_hero_banner():
    bw, bh = 1200, 520
    b_img = Image.new("RGB", (bw, bh), (11, 13, 20))
    draw = ImageDraw.Draw(b_img)

    # Ambient backdrop waves & glow
    for r in range(400, 0, -10):
        draw.ellipse([850 - r, 260 - r, 850 + r, 260 + r], fill=(16 + int(r * 0.05), 19 + int(r * 0.08), 35 + int(r * 0.15)))

    # Title & Branding
    draw.text((60, 80), "Music Player", fill=(255, 255, 255))
    draw.text((60, 140), "Native Offline Android Audio Player", fill=(0, 242, 254))
    draw.text((60, 195), "Built with Kotlin • Jetpack Compose • Media3 ExoPlayer • Room SQLite", fill=(160, 175, 205))
    
    # Feature Bullet Highlights
    highlights = [
        "✓ Reliable 48h+ continuous background playback with MediaSessionService",
        "✓ 100% Offline-First with Zero Telemetry, Trackers, or Cloud SDKs",
        "✓ Full Library Scanning: Songs, Albums, Artists, Folders, Playlists, Favorites",
        "✓ 5-Band Graphic Equalizer, Bass Boost, 3D Surround, & Sleep Timer"
    ]
    y_pos = 260
    for h in highlights:
        draw.text((60, y_pos), h, fill=(210, 220, 240))
        y_pos += 38

    # App icon badge mockup on the right side
    icon_box_size = 280
    ix = 820
    iy = 120
    draw.rounded_rectangle([ix, iy, ix + icon_box_size, iy + icon_box_size], radius=56, fill=(18, 22, 36), outline=(0, 242, 254, 140), width=3)
    draw.ellipse([ix + 40, iy + 40, ix + icon_box_size - 40, iy + icon_box_size - 40], fill=(12, 14, 22), outline=(139, 92, 246), width=4)
    draw.polygon([(ix + 120, iy + 95), (ix + 190, iy + 140), (ix + 120, iy + 185)], fill=(0, 242, 254))

    return b_img

render_now_playing().save(os.path.join(screenshots_dir, "01_now_playing.png"), "PNG")
render_library().save(os.path.join(screenshots_dir, "02_library.png"), "PNG")
render_equalizer().save(os.path.join(screenshots_dir, "03_equalizer.png"), "PNG")
render_hero_banner().save(os.path.join(screenshots_dir, "00_hero_banner.png"), "PNG")

print("Generated all showcase screenshots successfully!")
