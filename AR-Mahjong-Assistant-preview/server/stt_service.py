import os
import sys
import logging

logger = logging.getLogger(__name__)

try:
    from faster_whisper import WhisperModel, BatchedInferencePipeline
except ImportError:
    logger.warning("WARNING: faster-whisper not found. STT will not work.")
    WhisperModel = None
    BatchedInferencePipeline = None

class STTService:
    def __init__(self, model="base", language="zh"):
        logger.info(f"Initializing STT Service (faster-whisper) with model='{model}', language='{language}'...")
        if WhisperModel and BatchedInferencePipeline:
            try:
                # Initialize WhisperModel with CPU and int8 quantization
                # User requested: CPU version, int8
                self._model = WhisperModel(model, device="cpu", compute_type="int8")
                
                # Wrap with BatchedInferencePipeline for batch_size=8
                self.batched_model = BatchedInferencePipeline(model=self._model)
                self.language = language
                
                logger.info("STT Service initialized successfully with BatchedInferencePipeline.")
            except Exception as e:
                logger.error(f"Failed to initialize STT Service: {e}")
                self.batched_model = None
        else:
            self.batched_model = None

    def transcribe(self, file_path: str) -> str:
        """
        Transcribe audio file to text using faster-whisper.
        Raises Exception if STT fails or not available.
        """
        if not self.batched_model:
            raise RuntimeError("STT Service not available (model not loaded).")

        if not os.path.exists(file_path):
            raise FileNotFoundError(f"Audio file not found: {file_path}")

        try:
            logger.info(f"Transcribing file: {file_path}")
            # Use batched transcription
            # User requested: batch_size=8
            segments, info = self.batched_model.transcribe(
                file_path, 
                batch_size=8,
                language=self.language
            )
            
            # Gather segments
            text_segments = []
            for segment in segments:
                text_segments.append(segment.text)
                
            full_text = "".join(text_segments)
            logger.info(f"Transcription result: {full_text}")
            return full_text
        except Exception as e:
            logger.error(f"Transcription error: {e}")
            raise e
