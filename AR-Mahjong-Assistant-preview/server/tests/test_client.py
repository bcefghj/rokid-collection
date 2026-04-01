import requests
import os

url = "http://127.0.0.1:8000/api/analyze-hand"

# Create a dummy image file for testing if it doesn't exist
if not os.path.exists("test_image.jpg"):
    with open("test_image.jpg", "wb") as f:
        f.write(b"dummy image content")

files = {
    'image': ('test_image.jpg', open('test_image.jpg', 'rb'), 'image/jpeg')
}
data = {
    'session_id': 'test_session_123'
}

try:
    print(f"Sending request to {url}...")
    response = requests.post(url, files=files, data=data)
    print(f"Status Code: {response.status_code}")
    print(f"Response: {response.json()}")
except Exception as e:
    print(f"Error: {e}")
    print("Ensure the server is running (uvicorn main:app --reload)")
