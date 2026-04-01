from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class StartSessionRequest(BaseModel):
    session_id: str

class EndSessionRequest(BaseModel):
    session_id: str

class AnalyzeResponse(BaseModel):
    user_hand: List[str]
    melded_tiles: List[str]
    suggested_play: str
    annotated_image_path: Optional[str] = None
    action_detected: Optional[str] = None
    warning: Optional[str] = None
    is_stable: bool = True

class ProcessAudioResponse(BaseModel):
    transcript: str
    events: List[Dict[str, Any]]
    updated_visible_tiles_count: int
    details: List[str]
