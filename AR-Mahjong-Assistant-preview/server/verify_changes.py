import requests
import uuid
import time
import os

BASE_URL = "http://localhost:8000"

def test_workflow():
    session_id = str(uuid.uuid4())
    print(f"Testing with Session ID: {session_id}")
    
    # 1. Send Image (Analyze)
    print("Sending analyze request...")
    with open("server/test_image.jpg", "rb") as f:
        files = {"image": ("test.jpg", f, "image/jpeg")}
        data = {"session_id": session_id}
        resp = requests.post(f"{BASE_URL}/api/analyze-hand", files=files, data=data)
        
    if resp.status_code == 200:
        print("Analyze Success:", resp.json())
    else:
        print("Analyze Failed:", resp.text)
        return

    # 2. Check History
    print("Checking history...")
    resp = requests.get(f"{BASE_URL}/api/history/details/{session_id}")
    if resp.status_code == 200:
        history = resp.json()
        print(f"History retrieved. Status: {history['status']}")
        print(f"Interactions count: {len(history['interactions'])}")
        if len(history['interactions']) > 0:
            print("First interaction steps:", history['interactions'][0]['steps_log'])
    else:
        print("History Failed:", resp.text)

    # 3. End Session
    print("Ending session...")
    resp = requests.post(f"{BASE_URL}/api/end-session", json={"session_id": session_id})
    print("End Session:", resp.json())

if __name__ == "__main__":
    # Ensure server is running before running this
    test_workflow()
