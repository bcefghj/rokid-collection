import unittest
from mahjong_state_tracker import MahjongStateTracker
from mahjong.meld import Meld

class TestMahjongTrackerAdvanced(unittest.TestCase):
    def setUp(self):
        self.tracker = MahjongStateTracker()

    def test_chi_ambiguity_resolution(self):
        # Initial hand: 2m, 3m, 5m, 6m ...
        # IDs: 
        # 2m: 4, 5, 6, 7 -> use 4
        # 3m: 8, 9, 10, 11 -> use 8
        # 5m: 16
        # 6m: 20
        # Total 13 tiles
        hand_init = [4, 8, 16, 20, 40, 44, 48, 52, 56, 60, 64, 68, 72]
        self.tracker.update_state(hand_init) # INIT_WAIT

        # Scenario 1: Chi with 1m (ID 0)
        # Lost: 2m(4), 3m(8). New Hand: 5m... (len 11) + 3 in meld
        # Actually hand size becomes 13 (init) -> 14 (draw/call) -> 11 (after discard)
        # But update_state assumes we see the hand AFTER the action (and discard?).
        # Wait, if we Chi, we have 13 -> call -> 14 (with exposed meld) -> discard -> 13.
        # But the tracker logic says:
        # CHI: Lost=2, Gained=0.
        # This implies:
        # Start: 13 hidden + 1 (incoming call) -> 14.
        # Action: Reveal 2 from hand + 1 incoming.
        # Hidden hand loses 2.
        # Incoming tile never enters hidden hand.
        # So Hidden: 13 -> 11.
        # Then discard: 11 -> 10?
        # No, standard turn:
        # 1. Have 13 hidden.
        # 2. Opponent discards X.
        # 3. Call Chi on X. Reveal A, B from hand.
        # 4. Hand becomes: 13 - 2 = 11 hidden. (Meld has A, B, X).
        # 5. Must discard Y.
        # 6. Hand becomes: 11 - 1 = 10 hidden.
        
        # The tracker logic for CHI is Lost=2, Gained=0.
        # This corresponds to step 4 (Just after calling, before discard? Or including discard?)
        # If including discard: Lost 2 (meld) + Lost 1 (discard) = Lost 3.
        # Our logic `num_lost == 2` implies we are capturing the state *before* the discard?
        # Or maybe the user logic assumes:
        # "CHI/PON: Disappear 2 cards, No new cards."
        # This matches the state immediately after declaring the Meld but BEFORE discarding.
        # (13 -> 11).
        
        # Let's test this transition.
        # Hand 2: remove 4(2m) and 8(3m).
        hand_chi = [16, 20, 40, 44, 48, 52, 56, 60, 64, 68, 72]
        
        # Call with incoming_tile = 0 (1m)
        self.tracker.update_state(hand_chi, incoming_tile=0)
        
        last_action = self.tracker.action_history[-1]
        self.assertEqual(last_action["action"], "CHI")
        
        last_meld = self.tracker.meld_history[-1]
        self.assertEqual(last_meld.type, Meld.CHI)
        self.assertEqual(last_meld.called_tile, 0) # Should be 1m
        # Tiles should be 0, 4, 8 (sorted)
        self.assertEqual(last_meld.tiles, [0, 4, 8])

    def test_chi_ambiguity_resolution_other_side(self):
        self.setUp()
        # Initial hand: 2m, 3m ...
        hand_init = [4, 8, 16, 20, 40, 44, 48, 52, 56, 60, 64, 68, 72]
        self.tracker.update_state(hand_init)

        # Hand 2: remove 4(2m) and 8(3m).
        hand_chi = [16, 20, 40, 44, 48, 52, 56, 60, 64, 68, 72]
        
        # Call with incoming_tile = 12 (4m)
        # 2,3 eating 4 -> 2,3,4
        self.tracker.update_state(hand_chi, incoming_tile=12)
        
        last_meld = self.tracker.meld_history[-1]
        self.assertEqual(last_meld.type, Meld.CHI)
        self.assertEqual(last_meld.called_tile, 12) # Should be 4m
        # Tiles should be 4, 8, 12
        self.assertEqual(last_meld.tiles, [4, 8, 12])

    def test_pon_with_incoming(self):
        self.setUp()
        # Hand: 5p(16), 5p(17) ...
        # Actually 5p is index 13? No.
        # 0-8: m, 9-17: p, 18-26: s.
        # 0-35: 1-9m
        # 36-71: 1-9p
        # 5p is index 4 + 9 = 13.
        # 13 * 4 = 52.
        # So 5p IDs: 52, 53, 54, 55.
        
        hand_init = [52, 53, 60, 64, 68, 72, 76, 80, 84, 88, 92, 96, 100]
        self.tracker.update_state(hand_init)
        
        # Pon: Lose 52, 53.
        hand_pon = [60, 64, 68, 72, 76, 80, 84, 88, 92, 96, 100]
        
        # Incoming: 54 (another 5p)
        self.tracker.update_state(hand_pon, incoming_tile=54)
        
        last_meld = self.tracker.meld_history[-1]
        self.assertEqual(last_meld.type, Meld.PON)
        self.assertEqual(last_meld.called_tile, 54)
        self.assertEqual(last_meld.tiles, [52, 53, 54])

if __name__ == '__main__':
    unittest.main()
