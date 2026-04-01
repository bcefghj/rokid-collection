import unittest
import sys
import os

# Add server directory to path
sys.path.append(os.path.join(os.path.dirname(__file__), '.'))

from efficiency_engine import EfficiencyEngine

class TestChiKan(unittest.TestCase):
    def setUp(self):
        self.engine = EfficiencyEngine()

    def test_chi_opportunity(self):
        print("\n--- Testing Chi Opportunity ---")
        # Hand: 2m, 3m (Waiting for 1m/4m). 
        # Plus 555m, 666m, 777m, 11z (Pair)
        # 2m=4, 3m=8
        # 5m=16,17,18
        # 6m=20,21,22
        # 7m=24,25,26
        # 1z=108,109
        hand_13 = [4, 8, 16, 17, 18, 20, 21, 22, 24, 25, 26, 108, 109]
        
        # Check initial shanten
        result = self.engine.analyze_opportunities(hand_13)
        print(f"Initial Shanten: {result['current_shanten']}")
        
        # Incoming tile: 4m (12)
        # We simulate "Watch List" logic manually or check if analyze_opportunities covers it?
        # analyze_opportunities checks ALL 34 tiles.
        # So we look for "4m" in the watch_list with action "CHI".
        
        watch_list = result['watch_list']
        found_chi = False
        for item in watch_list:
            if item['tile'] == '4m' and item['action'] == 'CHI':
                found_chi = True
                print(f"Found Chi: {item}")
                break
        
        if not found_chi:
            print("Chi (4m) NOT found in watch list.")
        
        self.assertTrue(found_chi, "Should suggest Chi for 4m")

    def test_kan_logic(self):
        print("\n--- Testing Kan Logic ---")
        # Hand: 111m, 234m, 567m, 88s, 99p
        # 1m: 0, 1, 2
        # 2m: 4
        # 3m: 8
        # 4m: 12
        # 5m: 16
        # 6m: 20
        # 7m: 24
        # 8s: 100, 101 (8s is 72 + 7*4 = 100)
        # 9p: 68, 69 (9p is 36 + 8*4 = 68)
        
        hand_13 = [0, 1, 2, 4, 8, 12, 16, 20, 24, 100, 101, 68, 69]
        
        result = self.engine.analyze_opportunities(hand_13)
        print(f"Initial Shanten: {result['current_shanten']}")
        
        # Check Kan for 1m
        watch_list = result['watch_list']
        found_kan = False
        for item in watch_list:
            if item['tile'] == '1m' and item['action'] == 'KAN':
                found_kan = True
                print(f"Found Kan: {item}")
                break
                
        if not found_kan:
            print("Kan (1m) NOT found in watch list.")

if __name__ == '__main__':
    unittest.main()
