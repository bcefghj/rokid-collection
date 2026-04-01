import sqlite3
import json
import os
from datetime import datetime
from typing import List, Dict, Optional

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(BASE_DIR, "history.db")

def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    
    # Create sessions table
    c.execute('''
        CREATE TABLE IF NOT EXISTS sessions (
            session_id TEXT PRIMARY KEY,
            start_time TEXT,
            end_time TEXT,
            status TEXT,
            last_activity_time TEXT
        )
    ''')
    
    # Create interactions table
    c.execute('''
        CREATE TABLE IF NOT EXISTS interactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id TEXT,
            timestamp TEXT,
            image_path TEXT,
            steps_log TEXT,
            response_json TEXT,
            FOREIGN KEY(session_id) REFERENCES sessions(session_id)
        )
    ''')
    
    conn.commit()
    conn.close()

def get_db_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def create_or_update_session(session_id: str):
    conn = get_db_connection()
    c = conn.cursor()
    now = datetime.now().isoformat()
    
    # Check if session exists
    c.execute('SELECT * FROM sessions WHERE session_id = ?', (session_id,))
    session = c.fetchone()
    
    if session is None:
        c.execute('''
            INSERT INTO sessions (session_id, start_time, status, last_activity_time)
            VALUES (?, ?, ?, ?)
        ''', (session_id, now, "active", now))
    else:
        c.execute('''
            UPDATE sessions 
            SET last_activity_time = ?, status = 'active'
            WHERE session_id = ?
        ''', (now, session_id))
        
    conn.commit()
    conn.close()

def end_session(session_id: str):
    conn = get_db_connection()
    c = conn.cursor()
    now = datetime.now().isoformat()
    
    c.execute('''
        UPDATE sessions 
        SET end_time = ?, status = 'ended', last_activity_time = ?
        WHERE session_id = ?
    ''', (now, now, session_id))
    
    conn.commit()
    conn.close()

def log_interaction(session_id: str, image_path: str, steps: List[str], response: Dict):
    conn = get_db_connection()
    c = conn.cursor()
    now = datetime.now().isoformat()
    
    steps_json = json.dumps(steps)
    response_json = json.dumps(response)
    
    c.execute('''
        INSERT INTO interactions (session_id, timestamp, image_path, steps_log, response_json)
        VALUES (?, ?, ?, ?, ?)
    ''', (session_id, now, image_path, steps_json, response_json))
    
    # Also update session activity
    c.execute('''
        UPDATE sessions 
        SET last_activity_time = ?
        WHERE session_id = ?
    ''', (now, session_id))
    
    conn.commit()
    conn.close()

def get_all_sessions():
    conn = get_db_connection()
    c = conn.cursor()
    c.execute('SELECT * FROM sessions ORDER BY last_activity_time DESC')
    sessions = [dict(row) for row in c.fetchall()]
    conn.close()
    return sessions

def get_session_details(session_id: str):
    conn = get_db_connection()
    c = conn.cursor()
    
    # Get session info
    c.execute('SELECT * FROM sessions WHERE session_id = ?', (session_id,))
    session_row = c.fetchone()
    if not session_row:
        conn.close()
        return None
        
    session = dict(session_row)
    
    # Get interactions
    c.execute('SELECT * FROM interactions WHERE session_id = ? ORDER BY timestamp ASC', (session_id,))
    interactions = []
    for row in c.fetchall():
        item = dict(row)
        # Parse JSON fields
        try:
            item['steps_log'] = json.loads(item['steps_log'])
        except:
            item['steps_log'] = []
            
        try:
            item['response_json'] = json.loads(item['response_json'])
        except:
            item['response_json'] = {}
            
        interactions.append(item)
        
    session['interactions'] = interactions
    conn.close()
    return session

def close_inactive_sessions(timeout_seconds: int = 300) -> List[str]:
    """Close sessions that have been inactive for more than timeout_seconds. Returns list of closed session IDs."""
    conn = get_db_connection()
    c = conn.cursor()
    now = datetime.now()
    now_str = now.isoformat()
    
    # Get all active sessions
    c.execute("SELECT session_id, last_activity_time FROM sessions WHERE status = 'active'")
    active_sessions = c.fetchall()
    
    closed_sessions = []
    for row in active_sessions:
        sid = row['session_id']
        last_activity_str = row['last_activity_time']
        try:
            # Handle potential format differences or errors
            if '.' in last_activity_str:
                last_activity = datetime.fromisoformat(last_activity_str)
            else:
                # Fallback if seconds are missing fractions or whatever
                last_activity = datetime.fromisoformat(last_activity_str)
            
            delta = (now - last_activity).total_seconds()
            
            if delta > timeout_seconds:
                print(f"[Auto-Close] Closing session {sid}. Inactive for {delta:.1f}s")
                c.execute('''
                    UPDATE sessions 
                    SET status = 'ended', end_time = ? 
                    WHERE session_id = ?
                ''', (now_str, sid))
                closed_sessions.append(sid)
        except Exception as e:
            print(f"[Auto-Close] Error checking session {sid}: {e}")
            
    if closed_sessions:
        conn.commit()
    conn.close()
    return closed_sessions
