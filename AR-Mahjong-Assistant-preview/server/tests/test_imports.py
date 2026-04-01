
print("--- NEW RUN ---", flush=True)
import os
import sys
from fastapi import FastAPI
import database
from stt_service import STTService
from llm_service import LLMService

print("Step 1: Init DB", flush=True)
database.init_db()

print("Step 2: Init Services", flush=True)
print("Step 3: Init STT Service", flush=True)
STT_SERVICE = STTService()

print("Step 4: Init LLM Service", flush=True)
LLM_SERVICE = LLMService()

print("Done", flush=True)
