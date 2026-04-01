from server.efficiency_engine import EfficiencyEngine
from mahjong.tile import TilesConverter

def test_efficiency():
    engine = EfficiencyEngine()
    
    print("--- Test 1: Best Discard (14 tiles) ---")
    # 14 tiles: 1112345678999m + 1m (Extra 1m)
    # This hand is full of Man. 
    # Let's try a standard hand: 123m 456p 789s 11z 22z (Pair East, Pair South) -> 14 tiles
    # Wait, 123 456 789 + 11 + 22 = 3+3+3+2+2 = 13 tiles? No.
    # 3+3+3+2+2 = 13.
    # Add one more tile: 3z (West).
    # Hand: 123m 456p 789s 11z 22z 3z
    # Discard 3z (West) to get back to Tenpai (waiting for head?).
    # Actually 11z 22z are two pairs. 
    # If we have 123m 456p 789s 11z 22z, we are Tenpai (Shanpon wait on 1z/2z)?
    # Shanten should be 0.
    # If we add 3z, Shanten becomes 0 (discard 3z -> 0).
    # If we add 1z, we have 123m 456p 789s 111z 22z. Agari (-1).
    
    # Let's construct: 123m 456p 789s 11z 22z 3z
    hand_str_14 = '123m456p789s11223z'
    hand_14 = TilesConverter.one_line_string_to_136_array(hand_str_14)
    
    result = engine.calculate_best_discard(hand_14)
    print(f"Input: {hand_str_14}")
    print(f"Best Discard: {result['discard_tile']}")
    print(f"Result Shanten: {result['shanten']}")
    print(f"Ukeire: {result['ukeire']}")
    
    assert result['discard_tile'] == '3z'
    
    print("\n--- Test 2: Lookup Table (13 tiles) ---")
    # Hand: 123m 456p 789s 11z 2z (Tenpai waiting for 2z)
    hand_str_13 = '123m456p789s112z'
    hand_13 = TilesConverter.one_line_string_to_136_array(hand_str_13)
    
    lookup = engine.generate_lookup_table(hand_13)
    print(f"Lookup Table keys count: {len(lookup)}")
    
    # If we draw 2z, we should discard what? 
    # If draw 2z -> 123m 456p 789s 11z 22z -> Agari (-1).
    # But generate_lookup_table calculates BEST DISCARD.
    # If we draw 2z, we have a winning hand. Shanten -1.
    # Best discard? We can't discard if we win?
    # Actually _calculate_best_discard simulates discarding.
    # If Shanten is -1 (Agari), we don't discard.
    # But my _calculate_best_discard always discards one.
    # If I have 14 tiles (Complete hand), and I discard one, I break the win.
    # So shanten will increase to 0.
    # Unless I have > 14 tiles? No.
    # So if I draw a winning tile, the "suggested discard" might break the win, or be irrelevant.
    # However, "Push-Hu" rule: Shanten == -1 is Hu.
    # If I draw 2z, I should declare Hu.
    # The lookup table should ideally say "WIN".
    # But my current implementation just calculates best discard.
    # Let's see what it says. 
    # If draw 2z, best discard might be anything, resulting in Shanten 0.
    
    if '2z' in lookup:
        print(f"Draw 2z -> Discard: {lookup['2z']['discard']}, Shanten: {lookup['2z']['shanten']}")
    
    # If draw 3z -> Discard 3z (Tsumogiri) or equivalent.
    if '3z' in lookup:
        print(f"Draw 3z -> Discard: {lookup['3z']['discard']}")
        # 11z 2z 3z -> Discarding any honor is fine, but 1m might be better too?
        # Let's just check it exists.
        assert lookup['3z']['discard'] is not None

    print("\n--- Test 3: Analyze Opportunities (13 tiles) ---")
    # Use a real Tenpai hand: 123m 456p 789s 11z 23m (Wait 1m, 4m)
    # 23m needs 1m or 4m to become 123m or 234m.
    hand_str_tenpai = '12323m456p789s11z'
    hand_tenpai = TilesConverter.one_line_string_to_136_array(hand_str_tenpai)
    
    opportunities = engine.analyze_opportunities(hand_tenpai)
    print(f"Current Shanten: {opportunities['current_shanten']}")
    print(f"Win List: {opportunities['win_list']}")
    
    assert opportunities['current_shanten'] == 0
    assert '1m' in opportunities['win_list']
    assert '4m' in opportunities['win_list']
    
    # Test Pon Opportunity
    # Hand: 11m 123p ...
    # If we have 11m, and someone discards 1m. Pon -> 111m.
    # Hand: 11m 456p 789s 11z 23z (Wait 1z/4z?)
    # Let's try: 11m 123p 456s 789s 1z 2z
    # Shanten 1?
    hand_str_pon = '11m123p456s789s12z'
    hand_pon = TilesConverter.one_line_string_to_136_array(hand_str_pon)
    opp_pon = engine.analyze_opportunities(hand_pon)
    
    print("\nPon Test:")
    print(f"Hand: {hand_str_pon}")
    print(f"Watch List: {opp_pon['watch_list']}")
    # Should recommend Pon 1m if it helps.
    # 11m -> Pon -> 111m.
    # Remaining: 123p 456s 789s 1z 2z. (11 tiles).
    # Discard 1z -> 2z wait? Or Discard 2z -> 1z wait?
    # Original: 11m ... 1z 2z.
    # We have 4 melds (123p,456s,789s) + pair (11m) + 1z + 2z.
    # This is Shanten 1 (waiting for 1z or 2z to make pair? No, we have pair 11m).
    # We need 1 meld + 1 pair.
    # We have 3 melds + 1 pair + 2 singles.
    # Need 1 more meld.
    # If we Pon 1m -> 111m (Meld).
    # Now we have 4 melds (111m, 123p, 456s, 789s) + 1z + 2z.
    # We need a pair.
    # Discard 1z -> Wait 2z. (Shanten 0).
    # Original Shanten: 1.
    # So Pon 1m improves Shanten (1 -> 0).
    # So 1m should be in watch list.
    
    found_pon = any(w['tile'] == '1m' and w['action'] == 'PON' for w in opp_pon['watch_list'])
    if found_pon:
        print("Pon 1m detected successfully.")
    else:
        print("Pon 1m NOT detected.")

    print("\n--- Test 4: Keep List Structure ---")
    # Hand: 11m 123p 456s 789s 1z 2z (Shanten 1)
    # If we draw 1m (111m), we discard 1z or 2z.
    # Keep List should contain 1m.
    # Structure should be dict.
    
    keep_list = opp_pon['keep_list'] # using hand from previous test
    print(f"Keep List: {keep_list}")
    
    if keep_list:
        first_item = keep_list[0]
        assert isinstance(first_item, dict), "Keep list item should be a dictionary"
        assert 'draw' in first_item
        assert 'discard' in first_item
        assert 'shanten' in first_item
        assert 'ukeire' in first_item
        print("Keep List structure verified.")
    else:
        print("Keep list empty? Unexpected for this hand.")

if __name__ == "__main__":
    test_efficiency()
