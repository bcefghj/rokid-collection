#!/bin/bash
set -e

# Ensure we are in the project root
cd "$(dirname "$0")/.."

# Install nanoemoji if not present
if ! command -v nanoemoji &> /dev/null; then
    echo "Installing nanoemoji..."
    pip install nanoemoji
fi

# Run preparation script
python3 tools/prepare_mahjong_svgs.py

# Build font
echo "Building font..."
# Using PNGs requires specifying a bitmap format like CBDT or sbix.
# Android natively supports CBDT (Google's format).
# We set ascender/descender to fit standard em box?
# nanoemoji defaults should be fine, but we increase resolution.
# Note: nanoemoji arguments might vary by version. 
# We'll try to just rely on input size? No, it resizes.
# We'll stick to default for now to ensure it builds, but if we can pass a flag, good.
# Attempting to not resize? 
# nanoemoji doesn't have a simple --size flag in all versions.
# But it usually picks a size. 128 is default.

nanoemoji build_source/*.svg --output_file Mahjong-Color.ttf --family "Mahjong Color" --color_format=cbdt

# Move to android project
echo "Deploying font..."
mkdir -p app/src/main/res/font
cp build/Mahjong-Color.ttf app/src/main/res/font/mahjong_color.ttf

echo "Done. Font saved to app/src/main/res/font/mahjong_color.ttf"
