from PIL import Image, ImageDraw
import os

# Create a dummy image
img = Image.new('RGB', (500, 500), color = 'white')
img.save('test_input.jpg')

predictions = [
    {'x': 100, 'y': 100, 'width': 50, 'height': 50, 'class': '1s', 'confidence': 0.95},
    {'x': 200, 'y': 200, 'width': 80, 'height': 60, 'class': '2z', 'confidence': 0.88}
]

def draw_bounding_boxes(image_path: str, predictions: list, output_path: str):
    try:
        with Image.open(image_path) as im:
            draw = ImageDraw.Draw(im)
            for p in predictions:
                x = p.get('x', 0)
                y = p.get('y', 0)
                w = p.get('width', 0)
                h = p.get('height', 0)
                cls = p.get('class', '?')
                conf = p.get('confidence', 0.0)
                
                # Calculate corners (x,y are center)
                x0 = x - w / 2
                y0 = y - h / 2
                x1 = x + w / 2
                y1 = y + h / 2
                
                # Draw box
                draw.rectangle([x0, y0, x1, y1], outline="red", width=3)
                
                # Draw label background
                label = f"{cls} {conf:.2f}"
                text_w = len(label) * 6 + 4
                text_h = 14
                draw.rectangle([x0, y0 - text_h, x0 + text_w, y0], fill="red")
                draw.text((x0 + 2, y0 - text_h), label, fill="white")
                
            im.save(output_path)
            print(f"Saved to {output_path}")
        return True
    except Exception as e:
        print(f"Error: {e}")
        return False

if __name__ == "__main__":
    success = draw_bounding_boxes('test_input.jpg', predictions, 'test_output.jpg')
    if success and os.path.exists('test_output.jpg'):
        print("Drawing test passed.")
    else:
        print("Drawing test failed.")
