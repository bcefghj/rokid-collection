import unittest
from mahjong_state_tracker import MahjongStateTracker

class TestTrackerLogic(unittest.TestCase):
    def setUp(self):
        self.tracker = MahjongStateTracker()
        
    def test_discard(self):
        events = [{"type": "DISCARD", "tile": "5s"}]
        result = self.tracker.update_visible_tiles(events)
        
        # 5s is index: 0-8 (m), 9-17 (p), 18-26 (s). 5s is 18 + 4 = 22.
        # Wait, let's use TilesConverter to be sure or just check counts.
        # 1m-9m (0-8), 1p-9p (9-17), 1s-9s (18-26).
        # 5s is index 22.
        
        self.assertEqual(self.tracker.visible_tiles[22], 1)
        self.assertEqual(result["updated_count"], 1)
        
    def test_pon(self):
        # PON 1z (East) -> index 27
        events = [{"type": "PON", "tile": "1z"}]
        result = self.tracker.update_visible_tiles(events)
        
        self.assertEqual(self.tracker.visible_tiles[27], 3)
        self.assertEqual(result["updated_count"], 3)
        
    def test_kan(self):
        # KAN 2m -> index 1
        events = [{"type": "KAN", "tile": "2m"}]
        result = self.tracker.update_visible_tiles(events)
        
        self.assertEqual(self.tracker.visible_tiles[1], 4)
        self.assertEqual(result["updated_count"], 4)
        
    def test_chi_sequence(self):
        # CHI 1m2m3m -> index 0, 1, 2
        events = [{"type": "CHI", "tile": "1m2m3m"}]
        result = self.tracker.update_visible_tiles(events)
        
        self.assertEqual(self.tracker.visible_tiles[0], 1)
        self.assertEqual(self.tracker.visible_tiles[1], 1)
        self.assertEqual(self.tracker.visible_tiles[2], 1)
        self.assertEqual(result["updated_count"], 3)
        
    def test_mixed(self):
        events = [
            {"type": "DISCARD", "tile": "1s"},
            {"type": "PON", "tile": "2s"}
        ]
        self.tracker.update_visible_tiles(events)
        # 1s (idx 18) -> 1
        # 2s (idx 19) -> 3
        self.assertEqual(self.tracker.visible_tiles[18], 1)
        self.assertEqual(self.tracker.visible_tiles[19], 3)

if __name__ == '__main__':
    unittest.main()
