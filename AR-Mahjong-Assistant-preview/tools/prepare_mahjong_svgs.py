import os
import shutil
import subprocess
import sys
import base64
from PIL import Image

REPO_URL = "https://github.com/FluffyStuff/riichi-mahjong-tiles.git"
CLONE_DIR = "temp_mahjong_tiles"
OUTPUT_DIR = "build_source"

MAPPING = {}

# Winds
MAPPING['Ton'] = 'u1F000'
MAPPING['Nan'] = 'u1F001'
MAPPING['Shaa'] = 'u1F002'
MAPPING['Pei'] = 'u1F003'

# Dragons
MAPPING['Chun'] = 'u1F004' # Red
MAPPING['Hatsu'] = 'u1F005' # Green
MAPPING['Haku'] = 'u1F006' # White

# Man
for i in range(1, 10):
    MAPPING[f'Man{i}'] = f'u{0x1F007 + i - 1:X}'

# Sou
for i in range(1, 10):
    MAPPING[f'Sou{i}'] = f'u{0x1F010 + i - 1:X}'

# Pin
for i in range(1, 10):
    MAPPING[f'Pin{i}'] = f'u{0x1F019 + i - 1:X}'

def create_svg_wrapper(png_path, svg_path):
    with open(png_path, "rb") as img_file:
        encoded_string = base64.b64encode(img_file.read()).decode('utf-8')
    
    # Get dimensions from PIL to be sure
    with Image.open(png_path) as img:
        width, height = img.size
    
    svg_content = f'''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <image width="{width}" height="{height}" xlink:href="data:image/png;base64,{encoded_string}" />
</svg>'''
    
    with open(svg_path, "w", encoding='utf-8') as svg_file:
        svg_file.write(svg_content)

def main():
    if not os.path.exists(CLONE_DIR):
        print(f"Cloning {REPO_URL}...")
        subprocess.check_call(['git', 'clone', '--depth', '1', REPO_URL, CLONE_DIR])

    if os.path.exists(OUTPUT_DIR):
        shutil.rmtree(OUTPUT_DIR)
    os.makedirs(OUTPUT_DIR)

    src_dir = os.path.join(CLONE_DIR, 'Export', 'Regular')
    if not os.path.exists(src_dir):
        src_dir = CLONE_DIR
            
    print(f"Searching for PNGs in {src_dir}...")
    
    # Load Background Image (Front.png)
    front_path = os.path.join(src_dir, 'Front.png')
    if not os.path.exists(front_path):
        print(f"Warning: Front.png not found at {front_path}, skipping background composition.")
        bg_img = None
    else:
        bg_img = Image.open(front_path).convert("RGBA")
        print(f"Loaded background: {front_path}")

    count = 0
    for filename in os.listdir(src_dir):
        if not filename.endswith('.png'):
            continue
        
        name_part = filename.replace('.png', '')
        if name_part in MAPPING:
            target_name = MAPPING[name_part] + '.svg' # Output as SVG
            source_path = os.path.join(src_dir, filename)
            target_path = os.path.join(OUTPUT_DIR, target_name)
            
            # Composite if background exists
            if bg_img:
                fg_img = Image.open(source_path).convert("RGBA")
                # Create a new image for the result
                final_img = Image.new("RGBA", bg_img.size)
                # Paste background
                final_img.paste(bg_img, (0, 0))
                # Alpha composite foreground over background
                # Note: paste with mask handles simple transparency, 
                # but alpha_composite is better for blending.
                # However, Image.alpha_composite requires both to be same size.
                if fg_img.size != bg_img.size:
                    fg_img = fg_img.resize(bg_img.size, Image.LANCZOS)
                
                final_img = Image.alpha_composite(final_img, fg_img)
                
                # Save to temp PNG
                temp_png_path = os.path.join(OUTPUT_DIR, f"temp_{filename}")
                final_img.save(temp_png_path)
                
                # Wrap
                create_svg_wrapper(temp_png_path, target_path)
                
                # Cleanup
                os.remove(temp_png_path)
            else:
                # Just wrap directly
                create_svg_wrapper(source_path, target_path)
                
            count += 1
            print(f"Mapped {filename} -> {target_name}")
    
    print(f"Prepared {count} SVG-wrapped PNG files in {OUTPUT_DIR}")

if __name__ == '__main__':
    main()
