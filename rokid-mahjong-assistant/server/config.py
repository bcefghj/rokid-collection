import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

class Config:
    # Static Paths
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    STATIC_DIR = os.path.join(BASE_DIR, "static")
    UPLOAD_DIR = os.path.join(STATIC_DIR, "uploads")

    # LLM Configuration
    LLM_API_KEY = os.getenv("LLM_API_KEY", "lm-studio")
    LLM_BASE_URL = os.getenv("LLM_BASE_URL", "http://127.0.0.1:1234/v1")
    LLM_MODEL = os.getenv("LLM_MODEL", "qwen/qwen3-4b-2507")

    # Local YOLO Model Configuration
    YOLO_MODEL_PATH = os.path.join(BASE_DIR, "models/yolo/weights.onnx")
    YOLO_CLASS_NAMES_PATH = os.path.join(BASE_DIR, "models/yolo/class_names.txt")
    YOLO_CONF_THRESHOLD = float(os.getenv("YOLO_CONF_THRESHOLD", 0.54))
    YOLO_IOU_THRESHOLD = float(os.getenv("YOLO_IOU_THRESHOLD", 0.85))

    # Application Settings
    LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")

config = Config()
