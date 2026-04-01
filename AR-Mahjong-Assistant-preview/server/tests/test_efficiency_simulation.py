import unittest
import random
import sys
import os
from collections import Counter
from typing import List

# Add server directory to path
sys.path.append(os.path.join(os.path.dirname(__file__), '.'))

from efficiency_engine import EfficiencyEngine
from mahjong.tile import TilesConverter

class TestEfficiencySimulation(unittest.TestCase):
    def setUp(self):
        self.engine = EfficiencyEngine()

    def _generate_random_hand_from_34(self, tile_count=14) -> List[int]:
        """Generate a random hand ensuring max 4 of each tile type."""
        # 34 types * 4 tiles
        deck = []
        for i in range(34):
            deck.extend([i*4, i*4+1, i*4+2, i*4+3])
        random.shuffle(deck)
        return sorted(deck[:tile_count])

    def test_static_benchmarks_chinitsu(self):
        """Test a known Chinitsu (Full Flush) shape."""
        print("\n[Static] Testing Chinitsu Shape (Wait for 4m, 7m)...")
        # Hand: 11123456m + 88p + 999s + 1z (13 tiles)
        # This hand + 1z(draw) -> Discard 1z -> Tenpai waiting 4m, 7m
        # 111m (set), 234m (run), 56m (wait 4,7), 88p (pair), 999s (set)
        
        hand_str = "11123456m88p999s1z"
        hand_136 = TilesConverter.one_line_string_to_136_array(hand_str)
        
        best_discard = self.engine.calculate_best_discard(hand_136)
        
        print(f"Hand: {hand_str}")
        if best_discard:
            print(f"Recommended Discard: {best_discard['discard_tile']}")
            print(f"Shanten after discard: {best_discard['shanten']}")
            print(f"Ukeire tiles: {best_discard['ukeire_tiles']}")
            
            self.assertEqual(best_discard['discard_tile'], '1z', "Should discard the isolated honor tile")
            self.assertEqual(best_discard['shanten'], 0, "Should be Tenpai (0 shanten)")
            # Expect 1m, 4m, 7m, 8p
            # Analysis:
            # 1m -> 111(K) + 123(S) + 456(S) + 88(P) + 999(K) -> Agari
            # 4m -> 111(K) + 234(S) + 56(S)+4(Wait) + 88(P) + 999(K) -> Agari
            # 7m -> 111(K) + 234(S) + 56(S)+7(Wait) + 88(P) + 999(K) -> Agari
            # 8p -> 11(P) + 123(S) + 456(S) + 888(K) + 999(K) -> Agari
            expected_waits = {'1m', '4m', '7m', '8p'}
            self.assertEqual(set(best_discard['ukeire_tiles']), expected_waits, f"Should wait for exactly {expected_waits}")
        else:
            self.fail("Engine returned None for best_discard")

    def test_static_benchmarks_seven_pairs(self):
        """Test Chiitoitsu (Seven Pairs)."""
        print("\n[Static] Testing Seven Pairs...")
        # 1133557799m11p2z3z
        # 6 pairs (11,33,55,77,99,11) + 2z + 3z
        # Discard 2z or 3z -> 1-shanten for Chiitoitsu
        
        hand_str_14 = "1133557799m11p2z3z"
        hand_136_14 = TilesConverter.one_line_string_to_136_array(hand_str_14)
        
        best_discard = self.engine.calculate_best_discard(hand_136_14)
        
        print(f"Hand: {hand_str_14}")
        if best_discard:
            print(f"Recommended Discard: {best_discard['discard_tile']}")
            print(f"Shanten: {best_discard['shanten']}")
            
            # Standard Mahjong library shanten usually supports Chiitoitsu.
            # If shanten is <= 1, it likely supports Chiitoitsu.
            if best_discard['shanten'] <= 1:
                print("PASS: Engine supports Seven Pairs logic (or standard shape coincidence).")
            else:
                print(f"WARN: Engine calculated shanten {best_discard['shanten']}, might NOT support Seven Pairs efficiently.")
                # We don't fail here because the user requirement was '凑一般型' (standard form),
                # but '推倒胡' usually allows Chiitoitsu. This is an informational test.
        else:
            self.fail("Engine returned None for best_discard")

    def test_fuzz_consistency(self):
        """
        Fuzz Testing: Generate random hands and verify consistency 
        between calculate_best_discard and _get_blind_ukeire.
        """
        print("\n[Fuzz] Starting Fuzz Consistency Test (50 iterations)...")
        iterations = 50
        pass_count = 0
        
        for i in range(iterations):
            hand_136 = self._generate_random_hand_from_34(14)
            
            try:
                result = self.engine.calculate_best_discard(hand_136)
                if result is None:
                    # Should not happen for 14 tiles unless full hand is weird
                    print(f"Iter {i}: No discard returned.")
                    continue
                
                discard_tile_idx = result['discard_id'] # This is 0-33 index
                predicted_ukeire = result['ukeire']
                predicted_shanten = result['shanten']
                
                # Verification Step:
                # Convert original 14 to 34-array
                hand_34 = self.engine._to_34_array(hand_136)
                
                # Check if we actually have that tile
                if hand_34[discard_tile_idx] <= 0:
                    print(f"Iter {i}: Suggested discard {discard_tile_idx} not in hand!")
                    self.fail("Suggested discard not in hand")
                
                # Manually discard
                hand_34[discard_tile_idx] -= 1
                
                # Recalculate properties
                real_shanten = self.engine.shanten_calculator.calculate_shanten(hand_34)
                real_ukeire, _ = self.engine._get_blind_ukeire(hand_34, real_shanten)
                
                # Assertions
                if real_shanten != predicted_shanten:
                    print(f"Iter {i}: Shanten mismatch! Pred: {predicted_shanten}, Real: {real_shanten}")
                    self.fail("Shanten mismatch")
                
                if real_ukeire != predicted_ukeire:
                    print(f"Iter {i}: Ukeire mismatch! Pred: {predicted_ukeire}, Real: {real_ukeire}")
                    self.fail("Ukeire mismatch")
                    
                pass_count += 1
                
            except Exception as e:
                print(f"Iter {i}: Exception occurred: {e}")
                print(f"Hand: {hand_136}")
                raise e
                
        print(f"[Fuzz] Passed {pass_count}/{iterations} iterations.")

if __name__ == '__main__':
    unittest.main()
