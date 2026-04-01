import unittest
import sys
import os

# Add project root to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from server.mahjong_state_tracker import MahjongStateTracker
from mahjong.meld import Meld

class TestUnstableMelds(unittest.TestCase):
    def setUp(self):
        self.tracker = MahjongStateTracker()
        # Initial State: 13 Tiles (Waiting)
        # 1m 2m 3m 4m 5m 6m 7m 8m 9m 1p 2p 3p 4p
        self.hand_13 = "1m2m3m4m5m6m7m8m9m1p2p3p4p"
        # 10 Tiles (After calling Pon/Chi)
        self.hand_10 = "4m5m6m7m8m9m1p2p3p4p" 
        
        # Init with 13 tiles
        self.tracker.update_state(self.hand_13, [])

    def test_partial_meld_rejection(self):
        """
        Test Case 1: Partial Meld Detection
        Scenario: User has 13 tiles. Calls Pon (should have 10 hand + 3 meld).
        Input: 10 hand + 2 meld (Partial recognition).
        Expectation: Warning returned, State NOT updated.
        """
        print("\n=== Test 1: Partial Meld Rejection ===")
        
        # Input: Hand 10, Melds 2 (Partial 1m Pon)
        # 1m: 0, 1
        partial_melds = [0, 1] 
        
        result = self.tracker.update_state(self.hand_10, partial_melds)
        
        print(f"Result: {result}")
        
        self.assertEqual(result.get("action"), "WARNING", "Should return WARNING for partial meld")
        self.assertIn("Unstable meld", result.get("warning", ""), "Warning should mention unstable meld")
        
        # Verify State NOT updated
        self.assertEqual(len(self.tracker.current_melded_tiles), 0, "Melds should remain empty")
        self.assertEqual(len(self.tracker.current_hidden_hand), 13, "Hand should remain 13 tiles (reverted/untouched)")

    def test_ghost_meld_rejection(self):
        """
        Test Case 2: Ghost Meld Rejection (Too Many Tiles)
        Scenario: User has 13 tiles.
        Input: 13 hand + 3 meld (Ghost). Total 16.
        Expectation: Warning returned.
        """
        print("\n=== Test 2: Ghost Meld Rejection ===")
        
        # Ghost Pon (5z)
        ghost_melds = [124, 125, 126]
        
        result = self.tracker.update_state(self.hand_13, ghost_melds)
        
        print(f"Result: {result}")
        
        self.assertEqual(result.get("action"), "WARNING", "Should return WARNING for ghost meld")
        self.assertIn("Too many tiles", result.get("warning", ""), "Warning should mention too many tiles")
        
        # Verify State
        self.assertEqual(len(self.tracker.current_melded_tiles), 0, "Melds should remain empty")

    def test_missing_tiles_warning(self):
        """
        Test Case 3: Missing Tiles (Occlusion)
        Scenario: User has 13 tiles.
        Input: 10 hand + 0 meld. Total 10.
        Expectation: Warning returned.
        """
        print("\n=== Test 3: Missing Tiles Warning ===")
        
        # Input: Hand 10
        result = self.tracker.update_state(self.hand_10, [])
        
        print(f"Result: {result}")
        
        self.assertEqual(result.get("action"), "WARNING", "Should return WARNING for missing tiles")
        self.assertIn("Missing tiles", result.get("warning", ""), "Warning should mention missing tiles")
        
        # Verify State
        self.assertEqual(len(self.tracker.current_hidden_hand), 13, "Hand should remain 13 tiles")

    def test_valid_pon(self):
        """
        Test Case 4: Valid Pon (Control)
        Scenario: Hand 13 -> Hand 10 + Melds 3.
        Expectation: Success (PON).
        """
        print("\n=== Test 4: Valid Pon ===")
        
        # Pon 1m: 0, 1, 2
        melds = [0, 1, 2]
        
        result = self.tracker.update_state(self.hand_10, melds)
        
        print(f"Result: {result}")
        
        self.assertEqual(result.get("action"), "PON", "Should detect PON")
        self.assertEqual(len(self.tracker.current_melded_tiles), 3, "State should update")

if __name__ == '__main__':
    unittest.main()
