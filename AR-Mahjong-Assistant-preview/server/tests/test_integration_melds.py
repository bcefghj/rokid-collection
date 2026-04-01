import unittest
from mahjong_state_tracker import MahjongStateTracker
from efficiency_engine import EfficiencyEngine
from mahjong.tile import TilesConverter

class TestIntegrationMelds(unittest.TestCase):
    def test_pon_discard_integration(self):
        tracker = MahjongStateTracker()
        
        # 1. Initial Hand (13 tiles)
        # 11z (East) pair, 22z (South) pair, 1-9m
        # 1z=0,1,2,3. 2z=4,5,6,7.
        # Hand: 0,1 (East), 4,5 (South), 0-8 Man? No.
        # Let's use string conversion for simplicity.
        hand_str_1 = "11z22z123456789m" # 13 tiles
        result_1 = tracker.update_state(hand_str_1)
        self.assertEqual(result_1['action'], 'INIT_WAIT')
        
        # 2. Simulate Pon on 1z (East)
        # We lose 11z (pair) from hand.
        # Hand becomes 22z + 1-9m (11 tiles)
        hand_str_2 = "22z123456789m"
        result_2 = tracker.update_state(hand_str_2)
        
        self.assertEqual(result_2['action'], 'PON')
        self.assertEqual(len(tracker.meld_history), 1)
        self.assertEqual(tracker.meld_history[0].type, 'pon')
        
        # 3. Now we are in "Turn State" (after Pon, we must discard)
        # Hand length: 11. 11 % 3 = 2. Correct.
        hidden_hand = tracker.current_hidden_hand
        melds = tracker.meld_history
        
        self.assertEqual(len(hidden_hand), 11)
        
        # 4. Run Efficiency Engine
        engine = EfficiencyEngine()
        best_discard = engine._calculate_best_discard(hidden_hand, melds)
        
        print(f"Best Discard after Pon: {best_discard}")
        
        # We have 22z (pair) and 1-9m (3 sequences: 123, 456, 789)
        # Full hand (incl Pon 1z) is: 111z (Pon), 22z (Pair), 123m, 456m, 789m.
        # This is a winning hand (Tenpai/Win).
        # Shanten should be -1 (Win) or 0 (Tenpai)?
        # 4 melds (1z, 1m, 4m, 7m) + 1 pair (2z).
        # Wait, if we Pon'd, we have 11 tiles + 1 Pon.
        # To win, we need 14 tiles.
        # Current state: 11 hidden + 3 exposed = 14 tiles.
        # If it is a winning hand, Shanten is -1.
        # If Shanten is -1, _calculate_best_discard usually returns the tile that maintains it?
        # Or returns None?
        # My implementation sorts by shanten.
        # If we are already won, discarding any tile might increase shanten (break hand).
        # But we MUST discard after Pon.
        # So we look for a discard that keeps Shanten low.
        # If we discard 2z, we are left with 2z(single) + 4 sets. Tenpai.
        # If we discard 1m, we break sequence.
        # Let's see what it recommends.
        
        self.assertIsNotNone(best_discard)
        # Expectation: It should calculate something.
        # Since 11z Pon + 22z Pair + 1-9m is complete, Shanten is -1.
        # Discarding one tile (e.g. 2z) leaves us with 13 tiles (Tenpai, Shanten 0).
        # Discarding 1m leaves us with 13 tiles (Shanten 0? No, 23m needs 1/4m).
        
    def test_generate_lookup_with_melds(self):
        tracker = MahjongStateTracker()
        # 13 tiles, 1 meld (Pon 1z)
        # Hand: 22z 123456789m
        # But we need to set up tracker state.
        # Shortcut: manually set tracker state
        tracker.meld_history = [] # Need valid Meld object?
        # Let's use update_state sequence again.
        
        tracker.update_state("11z22z123456789m") # Init
        tracker.update_state("22z123456789m") # Pon 1z
        
        # Now discard 2z to be in "Wait State" (10 tiles)
        tracker.update_state("2z123456789m") # Discard 2z
        
        hidden_hand = tracker.current_hidden_hand
        melds = tracker.meld_history
        self.assertEqual(len(hidden_hand), 10)
        
        engine = EfficiencyEngine()
        lookup = engine.generate_lookup_table(hidden_hand, melds)
        
        self.assertTrue(len(lookup) > 0)
        print(f"Lookup Table Sample: {list(lookup.items())[:1]}")

if __name__ == '__main__':
    unittest.main()
