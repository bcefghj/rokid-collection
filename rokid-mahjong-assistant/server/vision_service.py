import cv2
import logging
import numpy as np
from typing import List, Dict, Any
from PIL import Image, ImageDraw
from yolo_inference import YOLOv8Inference

logger = logging.getLogger(__name__)

class VisionService:
    def __init__(self, model_path: str, class_names_path: str, confidence_threshold: float = 0.7, iou_threshold: float = 0.8):
        """
        Initialize the Vision Service with a local YOLO model.
        """
        self.model_path = model_path
        self.class_names_path = class_names_path
        self.confidence_threshold = confidence_threshold
        self.iou_threshold = iou_threshold
        self.model = None
        
        self._initialize_model()

    def _initialize_model(self):
        try:
            logger.info(f"Initializing VisionService with model: {self.model_path}")
            self.model = YOLOv8Inference(
                model_path=self.model_path,
                class_names_path=self.class_names_path,
                confidence_threshold=self.confidence_threshold,
                iou_threshold=self.iou_threshold
            )
            logger.info("VisionService initialized successfully.")
        except Exception as e:
            logger.error(f"Failed to initialize VisionService: {e}")
            raise e

    def detect_objects(self, image_path: str, conf_threshold: float = None, iou_threshold: float = None) -> List[Dict[str, Any]]:
        """
        Detect objects in an image file.
        Returns a list of dictionaries in the format:
        [
            {'x': center_x, 'y': center_y, 'width': w, 'height': h, 'class': class_name, 'confidence': conf},
            ...
        ]
        """
        if not self.model:
            logger.error("Model not initialized.")
            return []

        try:
            # Read image using OpenCV
            frame = cv2.imread(image_path)
            if frame is None:
                logger.error(f"Failed to read image at {image_path}")
                return []

            # Run inference
            detections = self.model.infer(frame, conf_threshold=conf_threshold, iou_threshold=iou_threshold)

            # Convert to standard format
            results = []
            
            # detections.xyxy is a numpy array of [x1, y1, x2, y2]
            # detections.confidence is a numpy array
            # detections['class_name'] is a numpy array of strings
            
            if len(detections.xyxy) > 0:
                for i in range(len(detections.xyxy)):
                    x1, y1, x2, y2 = detections.xyxy[i]
                    conf = float(detections.confidence[i])
                    cls_name = detections['class_name'][i]
                    
                    # Convert to center x, y, width, height
                    width = x2 - x1
                    height = y2 - y1
                    center_x = x1 + width / 2
                    center_y = y1 + height / 2
                    
                    results.append({
                        'x': float(center_x),
                        'y': float(center_y),
                        'width': float(width),
                        'height': float(height),
                        'class': cls_name,
                        'confidence': conf
                    })
            
            return results

        except Exception as e:
            logger.error(f"Error during object detection: {e}")
            return []

def draw_bounding_boxes(image_path: str, predictions: List[dict], output_path: str):
    """
    Draw bounding boxes on the image and save to output_path.
    Assumes predictions have x, y (center), width, height.
    """
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
                # Estimate text size (approximate if font not loaded)
                text_w = len(label) * 6 + 4
                text_h = 14
                draw.rectangle([x0, y0 - text_h, x0 + text_w, y0], fill="red")
                draw.text((x0 + 2, y0 - text_h), label, fill="white")
                
            im.save(output_path)
        return True
    except Exception as e:
        logger.error(f"Error drawing bounding boxes: {e}")
        return False
