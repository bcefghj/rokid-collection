import cv2
import supervision as sv
import time
from yolo_inference import YOLOv8Inference
from config import config

def main():
    # 1. Initialize Inference Client
    print(f"Loading local YOLOv8 model from {config.YOLO_MODEL_PATH}...")
    try:
        model = YOLOv8Inference(
            model_path=config.YOLO_MODEL_PATH,
            class_names_path=config.YOLO_CLASS_NAMES_PATH,
            confidence_threshold=config.YOLO_CONF_THRESHOLD,
            iou_threshold=config.YOLO_IOU_THRESHOLD
        )
    except Exception as e:
        print(f"Error initializing model: {e}")
        return

    # 2. Initialize Camera
    print("Opening camera...")
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("Error: Could not open camera.")
        return

    # Set resolution (optional, can improve performance if lowered)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    # 3. Initialize Annotators
    # BoxAnnotator draws bounding boxes
    box_annotator = sv.BoxAnnotator()
    # LabelAnnotator draws text labels
    label_annotator = sv.LabelAnnotator()

    print("Starting inference loop...")
    print("Press 'q' to quit.")

    frame_count = 0
    start_time = time.time()
    fps = 0

    try:
        while True:
            ret, frame = cap.read()
            if not ret:
                print("Failed to read frame")
                break

            # Run inference
            try:
                # Run local inference
                detections = model.infer(frame)

                # Draw annotations
                annotated_frame = box_annotator.annotate(
                    scene=frame.copy(),
                    detections=detections
                )
                
                # Prepare labels with confidence
                labels = [
                    f"{class_name} {confidence:.2f}"
                    for class_name, confidence
                    in zip(detections['class_name'], detections.confidence)
                ]
                
                annotated_frame = label_annotator.annotate(
                    scene=annotated_frame,
                    detections=detections,
                    labels=labels
                )

            except Exception as e:
                # If inference fails, just show the raw frame
                # and print error occasionally to avoid spamming
                if frame_count % 30 == 0:
                    print(f"Inference error: {e}")
                annotated_frame = frame

            # Calculate and display FPS
            frame_count += 1
            if frame_count % 10 == 0:
                end_time = time.time()
                fps = 10 / (end_time - start_time)
                start_time = end_time
            
            cv2.putText(annotated_frame, f"FPS: {fps:.1f}", (10, 30), 
                        cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

            # Show frame
            cv2.imshow("Mahjong YOLO Live", annotated_frame)

            # Exit on 'q'
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

    except KeyboardInterrupt:
        print("Interrupted by user")
    finally:
        cap.release()
        cv2.destroyAllWindows()
        print("Camera released and windows closed.")

if __name__ == "__main__":
    main()
