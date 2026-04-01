from PIL import Image
import sys

def check_alpha(path):
    img = Image.open(path)
    extrema = img.getextrema()
    print(f"{path}: Mode={img.mode}, Extrema={extrema}")
    # Extrema for RGBA is ((Rmin, Rmax), (Gmin, Gmax), (Bmin, Bmax), (Amin, Amax))
    if img.mode == 'RGBA':
        alpha_min, alpha_max = extrema[3]
        print(f"Alpha range: {alpha_min}-{alpha_max}")
        if alpha_min < 255:
            print("Has transparency")
        else:
            print("Fully opaque")

check_alpha('temp_mahjong_tiles/Export/Regular/Man1.png')
check_alpha('temp_mahjong_tiles/Export/Regular/Front.png')
