import requests
import os
import json

BASE_URL = "http://localhost:8000"
IMAGE_PATH = "麻将样本2.webp"
SESSION_ID = "verify_sample_2"

def test_analyze():
    if not os.path.exists(IMAGE_PATH):
        print(f"Error: {IMAGE_PATH} not found.")
        return

    print(f"Testing analyze-hand with {IMAGE_PATH}...")
    
    # MIME type explicitly set to image/webp
    files = {
        'image': ('麻将样本2.webp', open(IMAGE_PATH, 'rb'), 'image/webp')
    }
    data = {
        'session_id': SESSION_ID
    }
    
    try:
        response = requests.post(f"{BASE_URL}/api/analyze-hand", files=files, data=data)
        
        if response.status_code == 200:
            result = response.json()
            print("Success!")
            print(json.dumps(result, indent=2, ensure_ascii=False))
            
            print("\n--- Result Summary ---")
            print(f"Hand Tiles (暗牌): {result.get('user_hand', [])}")
            print(f"Melded Tiles (鸣牌): {result.get('melded_tiles', [])}")
                 
        else:
            print(f"Failed with status {response.status_code}")
            print(response.text)
            
    except Exception as e:
        print(f"Exception: {e}")

if __name__ == "__main__":
    test_analyze()
