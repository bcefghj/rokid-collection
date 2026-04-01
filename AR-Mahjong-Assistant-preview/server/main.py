from fastapi import FastAPI, File, UploadFile, Form
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse
from typing import List, Optional, Dict, Any
from PIL import Image
import uvicorn
import shutil
import os
import datetime
import database
import asyncio
import logging
from config import config
from mahjong_state_tracker import MahjongStateTracker
from mahjong.tile import TilesConverter
from efficiency_engine import EfficiencyEngine, format_suggestions
from stt_service import STTService
from llm_service import LLMService
from vision_service import VisionService, draw_bounding_boxes
from schemas import (
    StartSessionRequest, 
    AnalyzeResponse, 
    EndSessionRequest, 
    ProcessAudioResponse
)

# Configure Logging
logging.basicConfig(
    level=getattr(logging, config.LOG_LEVEL.upper(), logging.INFO),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)

# Global Session Trackers
SESSION_TRACKERS: Dict[str, MahjongStateTracker] = {}
EFFICIENCY_ENGINE = EfficiencyEngine()

# Initialize Services
# Note: Ensure OPENAI_API_KEY is set in environment or pass it here
STT_SERVICE = STTService()
# STT_SERVICE = None
LLM_SERVICE = LLMService(
    base_url=config.LLM_BASE_URL,
    api_key=config.LLM_API_KEY,
    model=config.LLM_MODEL
)
VISION_SERVICE = VisionService(
    model_path=config.YOLO_MODEL_PATH,
    class_names_path=config.YOLO_CLASS_NAMES_PATH,
    confidence_threshold=config.YOLO_CONF_THRESHOLD,
    iou_threshold=config.YOLO_IOU_THRESHOLD
)

# Initialize Database
database.init_db()

# YOLO Class to MPSZ Notation Mapping
YOLO_TO_MPSZ_MAPPING = {
    # --- Bamboo (s) ---
    '1B': '1s', '2B': '2s', '3B': '3s',
    '4B': '4s', '5B': '5s', '6B': '6s',
    '7B': '7s', '8B': '8s', '9B': '9s',

    # --- Characters (m) ---
    '1C': '1m', '2C': '2m', '3C': '3m',
    '4C': '4m', '5C': '5m', '6C': '6m',
    '7C': '7m', '8C': '8m', '9C': '9m',

    # --- Dots (p) ---
    '1D': '1p', '2D': '2p', '3D': '3p',
    '4D': '4p', '5D': '5p', '6D': '6p',
    '7D': '7p', '8D': '8p', '9D': '9p',

    # --- Winds (z 1-4) ---
    'EW': '1z', # East
    'SW': '2z', # South
    'WW': '3z', # West
    'NW': '4z', # North

    # --- Dragons (z 5-7) ---
    'WD': '5z', # White
    'GD': '6z', # Green
    'RD': '7z', # Red

    # --- Flowers/Seasons (Bonus) ---
    '1F': 'f1', '2F': 'f2', '3F': 'f3', '4F': 'f4',
    '1S': 's1', '2S': 's2', '3S': 's3', '4S': 's4',
}

def convert_to_mpsz(yolo_classes: List[str]):
    """
    Convert YOLO classes to MPSZ notation.
    Returns a tuple (hand_tiles, bonus_tiles).
    """
    hand_tiles = []
    bonus_tiles = []
    
    for cls in yolo_classes:
        mpsz = YOLO_TO_MPSZ_MAPPING.get(cls)
        if mpsz:
            # Check if it is a bonus tile (starts with 'f' or 's' followed by digit)
            if mpsz.startswith('f') or mpsz.startswith('s'):
                bonus_tiles.append(mpsz)
            else:
                hand_tiles.append(mpsz)
        else:
            # Keep original if unknown, or maybe ignore? 
            # For now keeping it to be safe, but logging might be good.
            hand_tiles.append(cls)
            
    return hand_tiles, bonus_tiles

app = FastAPI()

# Add CORS to allow requests from anywhere (helpful for dev)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Define Base Directory and Static Directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")

# Ensure uploads directory exists
UPLOAD_DIR = os.path.join(STATIC_DIR, "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Mount static files
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

@app.get("/")
async def read_root():
    return FileResponse(os.path.join(STATIC_DIR, "dashboard.html"))

@app.post("/api/start-session")
async def start_session(request: StartSessionRequest):
    logger.info(f"Received Start Session request: session_id={request.session_id}")
    database.create_or_update_session(request.session_id)
    # Initialize Tracker
    SESSION_TRACKERS[request.session_id] = MahjongStateTracker()
    return {"status": "success", "session_id": request.session_id}

@app.post("/api/analyze-hand", response_model=AnalyzeResponse)
async def analyze_hand(
    image: UploadFile = File(...),
    session_id: str = Form(...),
    incoming_tile: Optional[str] = Form(None)
):
    start_time = datetime.datetime.now()
    steps_log = []
    
    # Step 1: Initialize
    logger.info(f"Received Analyze request: session_id={session_id}, filename={image.filename}")
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Received request with image: {image.filename}")
    
    # Step 2: Ensure Session Exists
    database.create_or_update_session(session_id)
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Session verified/active")

    # Step 3: Save Image
    timestamp = int(start_time.timestamp() * 1000)
    file_extension = os.path.splitext(image.filename)[1] or ".jpg"
    safe_filename = f"{session_id}_{timestamp}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, safe_filename)
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(image.file, buffer)
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Image saved to {file_path}")
    except Exception as e:
        error_msg = f"Failed to save image: {str(e)}"
        logger.error(error_msg)
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] ERROR: {error_msg}")
        # Continue with mock logic even if save fails, but log it

    # Step 4: Perform Analysis
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Starting AI analysis...")
    
    user_hand = []
    melded_tiles = []
    annotated_path = None
    
    try:
        # Split image for dual inference
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Splitting image for dual inference (Hand/Melded)...")
        
        base_path = os.path.splitext(file_path)[0]
        top_path = f"{base_path}_top.jpg"
        bottom_path = f"{base_path}_bottom.jpg"
        mid_y = 0
        
        with Image.open(file_path) as img:
            width, height = img.size
            mid_y = height // 2
            
            top_img = img.crop((0, 0, width, mid_y))
            bottom_img = img.crop((0, mid_y, width, height))
            
            top_img.save(top_path)
            bottom_img.save(bottom_path)
            
        # 1. Inference Hand (Top)
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Analyzing Hand (Top Half)...")
        preds_top = VISION_SERVICE.detect_objects(top_path)
        preds_top.sort(key=lambda p: p.get("x", 0))
        user_hand, _ = convert_to_mpsz([p["class"] for p in preds_top])
        
        # 2. Inference Melded (Bottom)
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Analyzing Melded (Bottom Half)...")
        preds_bottom = VISION_SERVICE.detect_objects(bottom_path)
        
        # Adjust coordinates for bottom predictions
        for p in preds_bottom:
            p['y'] = p.get('y', 0) + mid_y
            
        preds_bottom.sort(key=lambda p: p.get("x", 0))
        melded_tiles, _ = convert_to_mpsz([p["class"] for p in preds_bottom])
        
        # 3. Combine and Draw
        all_preds = preds_top + preds_bottom
        
        annotated_filename = f"{session_id}_{timestamp}_annotated.jpg"
        annotated_full_path = os.path.join(UPLOAD_DIR, annotated_filename)
        
        if draw_bounding_boxes(file_path, all_preds, annotated_full_path):
            annotated_path = f"/static/uploads/{annotated_filename}"
            steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Generated annotated image with combined results")
            
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Result: Hand={user_hand}, Melded={melded_tiles}")
        
        # Cleanup temp files
        if os.path.exists(top_path): os.remove(top_path)
        if os.path.exists(bottom_path): os.remove(bottom_path)
        
    except Exception as e:
        error_msg = f"Inference/Processing Error: {str(e)}"
        logger.error(error_msg)
        steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] {error_msg}")

    # State Tracking & Action Inference
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Updating state tracker...")
    
    tracker = SESSION_TRACKERS.get(session_id)
    if not tracker:
        tracker = MahjongStateTracker()
        SESSION_TRACKERS[session_id] = tracker
        steps_log.append("Created new tracker for session")

    action_detected = "UNKNOWN"
    try:
        incoming_id = None
        if incoming_tile:
            ids = TilesConverter.one_line_string_to_136_array(incoming_tile)
            if ids:
                incoming_id = ids[0]
        
        update_result = tracker.update_state(user_hand, melded_tiles, incoming_id)
        action_detected = update_result.get("action", "UNKNOWN")
        warning_msg = update_result.get("warning")
        
        steps_log.append(f"State Update: Action={action_detected}")
        if warning_msg:
            steps_log.append(f"WARNING: {warning_msg}")
        
    except Exception as e:
        error_msg = f"Tracker Error: {e}"
        logger.error(error_msg)
        steps_log.append(error_msg)
        warning_msg = f"Internal Error: {e}"

    # Efficiency / Suggestion Logic
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Analysing optimal move...")
    
    suggested_play = f"Action: {action_detected}"
    
    if warning_msg:
        suggested_play = "请重新拍摄确认"
    else:
        try:
            if tracker.current_hidden_hand:
                hidden_count = len(tracker.current_hidden_hand)
                # Count tiles in melds (each meld object has .tiles list)
                meld_count = sum(len(m.tiles) for m in tracker.meld_history)
                total_tiles = hidden_count + meld_count
                
                # 14, 11, 8, 5, 2 -> My Turn (Discard)
                if total_tiles % 3 == 2: 
                    result = EFFICIENCY_ENGINE.calculate_best_discard(
                        tracker.current_hidden_hand, 
                        tracker.meld_history
                    )
                    suggested_play = format_suggestions(result, "discard")
                
                # 13, 10, 7, 4, 1 -> Waiting (Opponent Turn)
                elif total_tiles % 3 == 1: 
                    result = EFFICIENCY_ENGINE.analyze_opportunities(
                        tracker.current_hidden_hand,
                        tracker.meld_history
                    )
                    suggested_play = format_suggestions(result, "opportunity")
                    
        except Exception as e:
            err_msg = f"Efficiency Engine Error: {e}"
            logger.error(err_msg)
            steps_log.append(err_msg)

    response_data = AnalyzeResponse(
        user_hand=user_hand,
        melded_tiles=melded_tiles,
        suggested_play=suggested_play, 
        annotated_image_path=annotated_path,
        action_detected=action_detected,
        warning=warning_msg,
        is_stable=(warning_msg is None)
    )
    
    steps_log.append(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Analysis complete. Generating response.")

    # Step 5: Log Interaction to DB
    # We store the relative path for frontend access
    relative_image_path = f"/static/uploads/{safe_filename}"
    database.log_interaction(
        session_id=session_id,
        image_path=relative_image_path,
        steps=steps_log,
        response=response_data.dict()
    )
    
    logger.info(f"Processed successfully. Response sent.")
    return response_data

@app.post("/api/process-audio", response_model=ProcessAudioResponse)
async def process_audio(
    audio: UploadFile = File(...),
    session_id: str = Form(...)
):
    logger.info(f"Received Audio Processing request: session_id={session_id}")
    
    # Ensure Session Exists
    database.create_or_update_session(session_id)
    if session_id not in SESSION_TRACKERS:
        SESSION_TRACKERS[session_id] = MahjongStateTracker()
        logger.info("Created new tracker for session (from audio)")
    
    # Save Audio
    timestamp = int(datetime.datetime.now().timestamp() * 1000)
    ext = os.path.splitext(audio.filename)[1] or ".wav"
    filename = f"{session_id}_{timestamp}{ext}"
    file_path = os.path.join(UPLOAD_DIR, filename)
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(audio.file, buffer)
        logger.info(f"Audio saved to {file_path}")
    except Exception as e:
        logger.error(f"Failed to save audio: {e}")
        return ProcessAudioResponse(
            transcript="",
            events=[],
            updated_visible_tiles_count=0,
            details=[f"Error saving file: {str(e)}"]
        )
        
    # STT
    try:
        transcript = STT_SERVICE.transcribe(file_path)
    except Exception as e:
        logger.error(f"STT failed: {e}")
        return ProcessAudioResponse(
            transcript="",
            events=[],
            updated_visible_tiles_count=0,
            details=[f"STT processing error: {str(e)}"]
        )
    
    # LLM
    events = []
    if transcript:
        events = LLM_SERVICE.analyze_game_events(transcript)
        
    # Update State
    tracker = SESSION_TRACKERS[session_id]
    update_result = tracker.update_visible_tiles(events)
    
    # Log Interaction to DB
    relative_audio_path = f"/static/uploads/{filename}"
    
    response_data = {
        "transcript": transcript,
        "events": events,
        "updated_visible_tiles_count": update_result["updated_count"],
        "details": update_result["details"],
        "visible_tiles_snapshot": tracker.visible_tiles, # Snapshot of current state
        "audio_path": relative_audio_path
    }
    
    steps_log = [
        f"[{datetime.datetime.now().strftime('%H:%M:%S')}] Audio processed",
        f"Transcript: {transcript}",
        f"Events found: {len(events)}",
        f"Visible tiles updated: {update_result['updated_count']}"
    ]
    
    database.log_interaction(
        session_id=session_id,
        image_path=None, # No image for audio interaction
        steps=steps_log,
        response=response_data
    )
    
    return ProcessAudioResponse(
        transcript=transcript,
        events=events,
        updated_visible_tiles_count=update_result["updated_count"],
        details=update_result["details"]
    )

@app.post("/api/end-session")
async def end_session(request: EndSessionRequest):
    logger.info(f"Received End Session request: session_id={request.session_id}")
    database.end_session(request.session_id)
    # Cleanup Tracker
    SESSION_TRACKERS.pop(request.session_id, None)
    return {"status": "success", "message": "Session ended"}

# --- History APIs ---

@app.get("/api/history/sessions")
async def get_history_sessions():
    return database.get_all_sessions()

@app.get("/api/history/details/{session_id}")
async def get_history_details(session_id: str):
    details = database.get_session_details(session_id)
    if not details:
        return {"error": "Session not found"}
    return details

@app.post("/api/debug/yolo")
async def debug_yolo(
    image: UploadFile = File(...),
    conf_threshold: float = Form(0.54),
    iou_threshold: float = Form(0.85)
):
    timestamp = int(datetime.datetime.now().timestamp() * 1000)
    file_extension = os.path.splitext(image.filename)[1] or ".jpg"
    safe_filename = f"debug_{timestamp}{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, safe_filename)
    
    try:
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(image.file, buffer)
    except Exception as e:
        return {"error": f"Failed to save image: {str(e)}"}

    # Run detection with custom thresholds
    preds = VISION_SERVICE.detect_objects(
        file_path, 
        conf_threshold=conf_threshold, 
        iou_threshold=iou_threshold
    )
    
    # Sort predictions
    preds.sort(key=lambda p: p.get("x", 0))

    # Generate annotated image
    annotated_filename = f"debug_{timestamp}_annotated.jpg"
    annotated_full_path = os.path.join(UPLOAD_DIR, annotated_filename)
    
    annotated_url = None
    if draw_bounding_boxes(file_path, preds, annotated_full_path):
        annotated_url = f"/static/uploads/{annotated_filename}"
        
    return {
        "predictions": preds,
        "annotated_image_url": annotated_url,
        "original_image_url": f"/static/uploads/{safe_filename}",
        "params": {
            "conf_threshold": conf_threshold,
            "iou_threshold": iou_threshold
        }
    }

# --- Background Tasks ---

async def monitor_inactive_sessions():
    """Background task to close inactive sessions every 60 seconds."""
    logger.info("Starting inactive session monitor...")
    while True:
        try:
            await asyncio.sleep(60)
            # Check for sessions inactive for > 300 seconds
            closed_sessions = database.close_inactive_sessions(300)
            if len(closed_sessions) > 0:
                logger.info(f"Monitor: Closed {len(closed_sessions)} inactive sessions.")
                for sid in closed_sessions:
                    SESSION_TRACKERS.pop(sid, None)
        except Exception as e:
            logger.error(f"Monitor Error: {e}")
            await asyncio.sleep(60) # Wait before retrying

@app.on_event("startup")
async def startup_event():
    asyncio.create_task(monitor_inactive_sessions())

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
