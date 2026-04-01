import sys
import os

# Add server directory to path
sys.path.append(os.path.join(os.getcwd(), 'server'))

from efficiency_engine import EfficiencyEngine
from main import format_suggestions

def test_chi_details():
    engine = EfficiencyEngine()
    
    # Construct a hand in 136 format
    # Hand: 2m, 3m, 5m, 6m, 1p, 1p, 1p, 1s, 1s, 1s, 1z, 1z, 1z
    # 2m: index 1 -> 4
    # 3m: index 2 -> 8
    # 5m: index 4 -> 16
    # 6m: index 5 -> 20
    # 1p: index 9 -> 36, 37, 38
    # 1s: index 18 -> 72, 73, 74
    # 1z: index 27 -> 108, 109, 110
    
    hand_136 = [4, 8, 16, 20, 36, 37, 38, 72, 73, 74, 108, 109, 110]
    
    # Analyze
    # analyze_opportunities(hand_13, melds)
    result = engine.analyze_opportunities(hand_136, [])
    
    # Check raw result
    print("Raw Watch List:")
    found_chi = False
    for item in result.get("watch_list", []):
        if item['tile'] == '4m':
            found_chi = True
            print(f"Tile: {item['tile']}, Used: {item.get('used_tiles')}, Discard: {item.get('discard_suggestion')}")
            
    if not found_chi:
        print("No Chi opportunities found for 4m!")

    # Check formatted string
    formatted = format_suggestions(result)
    print("\nFormatted Output:")
    print(formatted)
    
    # Validation
    # Updated to expect merged format
    expected_snippets = ["4m(23m/35m/56m)"]
    found_count = 0
    for s in expected_snippets:
        if s in formatted:
            found_count += 1
        else:
            print(f"Missing: {s}")
            
    if found_count == 1:
        print("\nSUCCESS: All expected Chi details found (Merged Format).")
    else:
        print(f"\nFAILURE: Found {found_count}/1 expected snippets.")

if __name__ == "__main__":
    test_chi_details()
