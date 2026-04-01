
from server.mahjong_state_tracker import MahjongStateTracker

def test_gang_plus_discard():
    tracker = MahjongStateTracker()
    
    # Initial State: 14 tiles (Ready to discard or An Gang)
    # Hand: 1m 1m 1m 1m 2m 3m 4m 5m 6m 7m 8m 9m 1p 1p
    # Note: 136 format simulation. 
    # 1m (0,1,2,3), 2m (4), ...
    
    initial_hand = [
        0, 1, 2, 3,   # 1m x4
        4,            # 2m
        8,            # 3m
        12,           # 4m
        16,           # 5m
        20,           # 6m
        24,           # 7m
        28,           # 8m
        32,           # 9m
        36,           # 1p
        40            # 1p
    ]
    # Total 14 tiles
    
    print("--- Step 1: Init ---")
    res1 = tracker.update_state(initial_hand, [])
    print(f"Result: {res1['action']}")
    
    # Scenario: User performs An Gang (1m) AND Discards (9m) before next update.
    # An Gang: Remove 0,1,2,3. Draw replacement (say 9p -> 72). Hand size becomes 11.
    # Discard: Remove 32 (9m). Hand size becomes 10.
    
    next_hand = [
        4,            # 2m
        8,            # 3m
        12,           # 4m
        16,           # 5m
        20,           # 6m
        24,           # 7m
        28,           # 8m
        # 9m discarded
        36,           # 1p
        40,           # 1p
        72            # 9p (replacement)
    ]
    # Total 10 tiles
    
    print("\n--- Step 2: An Gang + Discard (Simulated delay) ---")
    res2 = tracker.update_state(next_hand, [])
    print(f"Result: {res2['action']}")
    
    if res2['action'] == "UNKNOWN_STATE_CHANGE":
        print("\n[FAIL] System failed to identify An Gang + Discard sequence.")
    elif "ANKAN" in res2['action']:
        print("\n[PASS] System identified An Gang.")
    else:
        print(f"\n[?] Unexpected result: {res2['action']}")

if __name__ == "__main__":
    test_gang_plus_discard()
