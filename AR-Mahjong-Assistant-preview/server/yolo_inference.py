import cv2
import numpy as np
import onnxruntime as ort
import supervision as sv
import logging

logger = logging.getLogger(__name__)

class YOLOv8Inference:
    def __init__(self, model_path, class_names_path, confidence_threshold=0.7, iou_threshold=0.8, input_size=None):
        """
        Initialize YOLOv8 ONNX Inference
        
        Args:
            input_size: Optional tuple (width, height) to force specific inference size. 
                        Useful for dynamic models or overriding model metadata.
        """
        logger.info(f"Loading model from {model_path}...")
        try:
            self.session = ort.InferenceSession(model_path)
            self.input_name = self.session.get_inputs()[0].name
            self.output_name = self.session.get_outputs()[0].name
            
            # Get input shape from model
            model_inputs = self.session.get_inputs()[0]
            self.input_shape = model_inputs.shape 
            
            # Determine input dimensions
            # Priority: 1. Manual override 2. Model metadata 3. Default 640x640
            if input_size:
                self.input_width, self.input_height = input_size
                logger.info(f"Model input size forced to: {self.input_width}x{self.input_height}")
            else:
                # Handle dynamic axes (which might be strings or None in ONNX)
                h = self.input_shape[2]
                w = self.input_shape[3]
                
                if isinstance(h, int) and isinstance(w, int):
                    self.input_height = h
                    self.input_width = w
                    logger.info(f"Model input size detected: {self.input_width}x{self.input_height}")
                else:
                    logger.warning(f"Model has dynamic input shape {self.input_shape}. Defaulting to 640x640. Please specify input_size if needed.")
                    self.input_height = 640
                    self.input_width = 640
        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            raise e
        
        self.confidence_threshold = confidence_threshold
        self.iou_threshold = iou_threshold
        
        print(f"Loading class names from {class_names_path}...")
        with open(class_names_path, 'r') as f:
            self.class_names = [line.strip() for line in f.readlines()]
            
    def preprocess(self, image):
        """
        Preprocess image: Letterbox resize, normalize, CHW
        """
        img_h, img_w = image.shape[:2]
        
        # Calculate scaling ratio
        scale = min(self.input_width / img_w, self.input_height / img_h)
        new_w = int(round(img_w * scale))
        new_h = int(round(img_h * scale))
        
        # Resize
        if (img_w, img_h) != (new_w, new_h):
            image_resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_LINEAR)
        else:
            image_resized = image

        # Calculate padding
        dw = (self.input_width - new_w) / 2
        dh = (self.input_height - new_h) / 2
        
        top, bottom = int(round(dh - 0.1)), int(round(dh + 0.1))
        left, right = int(round(dw - 0.1)), int(round(dw + 0.1))
        
        # Add border
        image_padded = cv2.copyMakeBorder(image_resized, top, bottom, left, right, cv2.BORDER_CONSTANT, value=(114, 114, 114))
        
        # BGR to RGB, HWC to CHW, Normalize
        image_input = cv2.cvtColor(image_padded, cv2.COLOR_BGR2RGB)
        image_input = image_input.transpose((2, 0, 1))
        image_input = np.expand_dims(image_input, axis=0)
        image_input = np.ascontiguousarray(image_input, dtype=np.float32)
        image_input /= 255.0
        
        return image_input, scale, (dw, dh)

    def infer(self, frame, conf_threshold=None, iou_threshold=None):
        """
        Run inference on a frame
        """
        # Use provided thresholds or fall back to instance defaults
        conf_thres = conf_threshold if conf_threshold is not None else self.confidence_threshold
        iou_thres = iou_threshold if iou_threshold is not None else self.iou_threshold

        input_tensor, scale, (dw, dh) = self.preprocess(frame)
        
        outputs = self.session.run([self.output_name], {self.input_name: input_tensor})[0]
        
        # Postprocess
        # outputs shape: (1, 4 + num_classes, 8400)
        # Transpose to (1, 8400, 4 + num_classes)
        predictions = np.transpose(outputs, (0, 2, 1))
        predictions = predictions[0]
        
        # Split boxes and scores
        boxes = predictions[:, :4] # cx, cy, w, h
        scores = predictions[:, 4:]
        
        # Get class with max score
        class_ids = np.argmax(scores, axis=1)
        max_scores = np.max(scores, axis=1)
        
        # Filter by confidence
        mask = max_scores > conf_thres
        boxes = boxes[mask]
        class_ids = class_ids[mask]
        scores = max_scores[mask]
        
        if len(boxes) == 0:
            empty_det = sv.Detections.empty()
            empty_det['class_name'] = np.array([])
            return empty_det
            
        # Convert cx, cy, w, h to x1, y1, x2, y2
        xyxy = np.zeros_like(boxes)
        xyxy[:, 0] = boxes[:, 0] - boxes[:, 2] / 2
        xyxy[:, 1] = boxes[:, 1] - boxes[:, 3] / 2
        xyxy[:, 2] = boxes[:, 0] + boxes[:, 2] / 2
        xyxy[:, 3] = boxes[:, 1] + boxes[:, 3] / 2
        
        # NMS
        indices = cv2.dnn.NMSBoxes(xyxy.tolist(), scores.tolist(), conf_thres, iou_thres)
        
        if len(indices) == 0:
            empty_det = sv.Detections.empty()
            empty_det['class_name'] = np.array([])
            return empty_det
            
        indices = np.array(indices).flatten()
        
        final_boxes = xyxy[indices]
        final_scores = scores[indices]
        final_class_ids = class_ids[indices]
        
        # Rescale boxes to original image
        final_boxes[:, [0, 2]] -= dw
        final_boxes[:, [1, 3]] -= dh
        final_boxes[:, :] /= scale
        
        # Create supervision Detections object
        detections = sv.Detections(
            xyxy=final_boxes,
            confidence=final_scores,
            class_id=final_class_ids
        )
        
        # Add class names to data dict for convenience
        detections['class_name'] = np.array([self.class_names[class_id] for class_id in final_class_ids])
        
        return detections
