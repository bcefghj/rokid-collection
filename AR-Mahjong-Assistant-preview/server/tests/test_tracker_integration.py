import unittest
from mahjong_state_tracker import MahjongStateTracker
from mahjong.tile import TilesConverter
from mahjong.meld import Meld

class TestTrackerIntegration(unittest.TestCase):
    def setUp(self):
        self.tracker = MahjongStateTracker()
        
    def test_full_game_flow(self):
        # 1. Init (13 tiles)
        hand_init = "1m2m3m4m5m6m1p2p3p4p5p6p1z" # 13 tiles
        res = self.tracker.update_state(hand_init, [])
        self.assertEqual(res["action"], "INIT_WAIT")
        
        # 2. Draw (14 tiles) - Draw 2z
        hand_draw = hand_init + "2z"
        res = self.tracker.update_state(hand_draw, [], incoming_tile=TilesConverter.one_line_string_to_136_array("2z")[0])
        self.assertEqual(res["action"], "DRAW")
        
        # 3. Discard 1z (13 tiles)
        hand_discard = "1m2m3m4m5m6m1p2p3p4p5p6p2z" # Lost 1z
        res = self.tracker.update_state(hand_discard, [])
        self.assertEqual(res["action"], "DISCARD")
        
        # 4. CHI
        # Current Hand: 1m-6m, 1p-6p, 2z
        # Opponent discards 4z? No, let's say I have 2m,3m and call 1m (Chi).
        # Wait, I have 1m,2m,3m in hand.
        # Let's say I discard 2z.
        # Next turn, opponent discards 7m. I have 5m,6m. I call Chi (567m).
        # Hand loses 5m, 6m. Meld gains 5m, 6m, 7m.
        # New Hand: 1m2m3m4m 1p2p3p4p5p6p 2z (Wait, 2z was discarded).
        # Hand after discard 2z: 1m..6m, 1p..6p.
        # Call 7m with 5m,6m.
        # Hand becomes: 1m2m3m4m 1p2p3p4p5p6p. (10 tiles)
        hand_after_chi = "1m2m3m4m1p2p3p4p5p6p"
        melds_chi = "5m6m7m"
        
        # Incoming tile 7m (id ~ 24)
        incoming_7m = TilesConverter.one_line_string_to_136_array("7m")[0]
        
        res = self.tracker.update_state(hand_after_chi, [melds_chi], incoming_tile=incoming_7m)
        self.assertEqual(res["action"], "CHI")
        # Verify meld history
        self.assertEqual(len(self.tracker.meld_history), 1)
        # Type in meld history is string or enum? 
        # mahjong library uses string 'chi', 'pon', 'kan', 'shouminkan' usually?
        # Let's check logic. tracker uses Meld.CHI. 
        # Meld object type is... let's check.
        self.assertEqual(self.tracker.meld_history[0].type, Meld.CHI)
        
        # 5. ANKAN (Dark Kan)
        # Assume I draw 9p x4 over time.
        # Let's jump state.
        # Hand has 9p, 9p, 9p, 9p and others.
        hand_pre_ankan = "1m2m3m9p9p9p9p1s1s1s2s2s2s" # 14 tiles
        # Reset state to this (simulating many turns)
        self.tracker.current_hidden_hand = self.tracker._normalize_hand(hand_pre_ankan)
        # Keep melds same (Chi 567m)
        self.tracker.current_melded_tiles = self.tracker._normalize_hand(melds_chi)
        
        # Action Ankan: Lose 4x9p, Gain 1x replacement (say 3s).
        hand_post_ankan = "1m2m3m1s1s1s2s2s2s3s" # 11 tiles (14 - 4 + 1 = 11)
        # Meld list UNCHANGED (Ankan not visible)
        res = self.tracker.update_state(hand_post_ankan, [melds_chi])
        
        self.assertEqual(res["action"], "ANKAN")
        self.assertEqual(len(res["tiles"]), 4)
        
        # 6. KAKAN (Added Kan)
        # Suppose I have a Pon of 1s (1s, 1s, 1s).
        # And I draw another 1s.
        # I add it to the Pon.
        
        # Setup state
        hand_pre_kakan = "1m2m3m2s2s2s3s1s" # Has 1s
        melds_pon = "1s1s1s" # Pon 1s
        # We need to register this Pon in history first
        # Manually inject history for test
        pon_tiles = TilesConverter.one_line_string_to_136_array("1s1s1s")
        m = Meld(Meld.PON, pon_tiles, True, pon_tiles[0], 0, 0)
        self.tracker.meld_history = [m]
        self.tracker.current_hidden_hand = self.tracker._normalize_hand(hand_pre_kakan)
        self.tracker.current_melded_tiles = self.tracker._normalize_hand(melds_pon)
        
        # Action Kakan: Lose 1s from hand. Melds gain 1s.
        hand_post_kakan = "1m2m3m2s2s2s3s"
        melds_kakan = "1s1s1s1s"
        
        res = self.tracker.update_state(hand_post_kakan, [melds_kakan])
        self.assertEqual(res["action"], "KAKAN")
        self.assertEqual(self.tracker.meld_history[0].type, Meld.SHOUMINKAN) # Upgraded to Kan (Shouminkan)

if __name__ == '__main__':
    unittest.main()
