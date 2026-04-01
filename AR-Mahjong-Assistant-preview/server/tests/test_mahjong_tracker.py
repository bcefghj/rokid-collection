import unittest
from mahjong_state_tracker import MahjongStateTracker, MahjongLogicError
from mahjong.meld import Meld

class TestMahjongStateTracker(unittest.TestCase):
    def setUp(self):
        self.tracker = MahjongStateTracker()

    def test_init_13(self):
        # 13 tiles: 1-9m, 1-4p
        hand = [0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48] 
        res = self.tracker.update_state(hand)
        self.assertEqual(res["action"], "INIT_WAIT")
        self.assertEqual(self.tracker.current_hidden_hand, hand)

    def test_init_14(self):
        hand = [0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52]
        res = self.tracker.update_state(hand)
        self.assertEqual(res["action"], "INIT_TURN")

    def test_draw(self):
        # Init with 13
        hand1 = [0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        self.tracker.update_state(hand1)
        
        # Draw one tile (say 52)
        hand2 = hand1 + [52]
        res = self.tracker.update_state(hand2)
        self.assertEqual(res["action"], "DRAW")
        self.assertEqual(res["gained_tiles"], [52])
        self.assertEqual(len(self.tracker.current_hidden_hand), 14)

    def test_discard(self):
        # Init with 14
        hand1 = [0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52]
        self.tracker.update_state(hand1)
        
        # Discard tile 0
        hand2 = hand1[1:]
        res = self.tracker.update_state(hand2)
        self.assertEqual(res["action"], "DISCARD")
        self.assertEqual(res["lost_tiles"], [0])
        self.assertEqual(len(self.tracker.current_hidden_hand), 13)

    def test_pon(self):
        # Init with 13: 1m, 1m (0, 1), ...
        hand1 = [0, 1, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        self.tracker.update_state(hand1, [])
        
        # Pon: lose 0, 1. Gain [0, 1, 2] in melds.
        # New hand: 11 tiles (lost 2)
        hand2 = [8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        melds2 = [0, 1, 2]
        
        res = self.tracker.update_state(hand2, melds2)
        self.assertEqual(res["action"], "PON")
        self.assertEqual(len(self.tracker.meld_history), 1)
        self.assertEqual(self.tracker.meld_history[0].type, Meld.PON)
        # Check tiles in meld: should be 0, 1 and inferred 1m
        self.assertEqual([t//4 for t in self.tracker.meld_history[0].tiles], [0, 0, 0])

    def test_chi(self):
        # Init with 13: 1m (0), 2m (4)...
        hand1 = [0, 4, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52]
        self.tracker.update_state(hand1, [])
        
        # Chi: lose 1m, 2m. Gain [0, 4, 8] (1m, 2m, 3m) in melds.
        hand2 = [12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52]
        melds2 = [0, 4, 8]
        
        res = self.tracker.update_state(hand2, melds2)
        self.assertEqual(res["action"], "CHI")
        self.assertEqual(len(self.tracker.meld_history), 1)
        self.assertEqual(self.tracker.meld_history[0].type, Meld.CHI)
        
    def test_daiminkan(self):
        # Init 13: three 1m (0, 1, 2)
        hand1 = [0, 1, 2, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        self.tracker.update_state(hand1, [])
        
        # Daiminkan: lose 0,1,2. Gain [0, 1, 2, 3] in melds.
        # Hand length: 13 -> 10.
        hand2 = [12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        # Wait, Daiminkan logic: Discarder cuts, you call Kan. You lose 3 tiles from hand.
        # You display 4 tiles. You draw 1 supplement. Discard 1.
        # State capture usually happens after the call?
        # If captured immediately after Kan: Hand has 10 tiles. Melds has 4.
        melds2 = [0, 1, 2, 3]
        
        res = self.tracker.update_state(hand2, melds2)
        self.assertEqual(res["action"], "DAIMINKAN")
        self.assertEqual(self.tracker.meld_history[0].type, Meld.KAN)
        self.assertTrue(self.tracker.meld_history[0].opened)

    def test_ankan(self):
        # Init 14: four 1m
        hand1 = [0, 1, 2, 3, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        self.tracker.update_state(hand1, [])
        
        # Ankan: lose 0,1,2,3. Gain replacement (52).
        # Net -3. Melds UNCHANGED (Ankan is not visible in open melds list usually, or handled separately)
        # Based on logic: Ankan not in new_melds.
        hand2 = [12, 16, 20, 24, 28, 32, 36, 40, 44, 48, 52]
        res = self.tracker.update_state(hand2, [])
        self.assertEqual(res["action"], "ANKAN")
        self.assertEqual(self.tracker.meld_history[0].type, Meld.KAN)
        self.assertFalse(self.tracker.meld_history[0].opened)

    def test_ankan_from_13_tiles(self):
        """
        Test Ankan detection when transition is 13 tiles -> 11 tiles (Draw + Ankan combined).
        Delta H = -2.
        """
        # Initial Hand: 13 tiles, including 4x 1m (IDs 0,1,2,3)
        hand_13 = [0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 12, 13, 14]
        
        # Initialize
        self.tracker.update_state(hand_13, [])
        
        # Next State: 11 tiles.
        # Action: Draw 5m (16), Ankan 1m (lose 0,1,2,3), Draw Supplement 6m (20).
        # Result: 2m*3, 3m*3, 4m*3, 5m*1, 6m*1
        hand_11 = [4, 5, 6, 8, 9, 10, 12, 13, 14, 16, 20]
        
        # Check diff
        # Len: 13 -> 11 (-2)
        # 1m count: 4 -> 0 (Lost 4)
        
        result = self.tracker.update_state(hand_11, [])
        
        self.assertEqual(result['action'], 'ANKAN')
        self.assertEqual(len(result['tiles']), 4)
        self.assertEqual(result['tiles'][0] // 4, 0) # 1m

    def test_kakan(self):
        # Setup: PON existing
        # Hand has 11 tiles (after Pon)
        hand1 = [8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        melds1 = [0, 1, 2] # Pon 1m
        
        # Initialize with history
        self.tracker.update_state(hand1, melds1)
        # Manually inject history because update_state detects change, but here we init.
        # To make Kakan work, we need an existing PON in meld_history.
        # update_state(hand1, melds1) would see melds1 as NEW if init.
        # Step A init: sets current_melded_tiles = melds1. Action INIT_WAIT.
        # It does NOT populate meld_history.
        
        # We need to manually populate meld_history for the test to work,
        # OR simulate the Pon event first.
        # Let's simulate Pon first.
        self.tracker = MahjongStateTracker()
        h0 = [0, 1, 8, 12, 16, 20, 24, 28, 32, 36, 40, 44, 48]
        self.tracker.update_state(h0, []) # Init
        self.tracker.update_state(hand1, melds1) # Pon
        
        # Now Draw 4th 1m (3)
        hand2 = hand1 + [3]
        self.tracker.update_state(hand2, melds1) # Draw
        
        # Kakan: lose 3. Add 3 to melds.
        hand3 = hand1 # Back to 11 tiles
        melds2 = [0, 1, 2, 3]
        
        res = self.tracker.update_state(hand3, melds2)
        
        self.assertEqual(res["action"], "KAKAN")
        self.assertEqual(self.tracker.meld_history[-1].type, Meld.SHOUMINKAN)

if __name__ == '__main__':
    unittest.main()
