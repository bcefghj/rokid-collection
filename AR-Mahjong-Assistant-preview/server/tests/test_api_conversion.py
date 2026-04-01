import requests
import os

API_URL = "http://localhost:8000/api/analyze-hand"
IMAGE_PATH = "static/uploads/3a6b35ba-213b-40da-8566-13ce7e15a9ac_1768273293616.jpg"
SESSION_ID = "test_conversion_session"

def test_api():
    if not os.path.exists(IMAGE_PATH):
        print(f"Image not found: {IMAGE_PATH}")
        return

    print(f"Sending request to {API_URL}...")
    with open(IMAGE_PATH, "rb") as f:
        files = {"image": f}
        data = {"session_id": SESSION_ID}
        response = requests.post(API_URL, files=files, data=data)

    if response.status_code == 200:
        result = response.json()
        print("Success!")
        print(f"User Hand: {result.get('user_hand')}")
        # Verify format (should look like '1s', '2m', etc.)
        hand = result.get('user_hand', [])
        valid_mpsz = True
        for tile in hand:
            if not (len(tile) >= 2 and tile[-1] in ['m', 'p', 's', 'z']):
                # It might be UNKNOWN if mapping fails, but we expect mostly valid ones
                print(f"Warning: Unexpected tile format: {tile}")
                valid_mpsz = False
        
        if valid_mpsz and hand:
            print("MPSZ Verification: PASS")
        elif not hand:
             print("MPSZ Verification: WARNING (Empty Hand)")
        else:
             print("MPSZ Verification: FAIL (See warnings)")
             
    else:
        print(f"Error: {response.status_code}")
        print(response.text)

if __name__ == "__main__":
    test_api()
