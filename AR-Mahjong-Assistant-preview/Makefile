.PHONY: run test format lint install clean

# Variables
PYTHON = python
PIP = pip
SERVER_DIR = server

install:
	$(PIP) install -r $(SERVER_DIR)/requirements.txt

run:
	cd $(SERVER_DIR) && $(PYTHON) main.py

test:
	pytest

format:
	ruff format .
	ruff check --fix .

lint:
	ruff check .

clean:
	find . -type d -name "__pycache__" -exec rm -rf {} +
	find . -type d -name ".pytest_cache" -exec rm -rf {} +
	find . -type d -name ".ruff_cache" -exec rm -rf {} +
