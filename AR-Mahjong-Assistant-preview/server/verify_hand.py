import sys
import os

# Ensure we can import from the current directory
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from efficiency_engine import EfficiencyEngine
from mahjong.tile import TilesConverter

def main():
    # Initialize engine
    engine = EfficiencyEngine()

    # Hand: 3467m 2356p 5578s 11z
    # Note: 11z usually means East East.
    # The TilesConverter expects a string like "3467m2356p5578s11z"
    hand_str = "3467m2356p5578s11z"
    print(f"正在分析手牌: {hand_str}")

    # Convert to 136-tile array (internal representation)
    try:
        hand_136 = TilesConverter.one_line_string_to_136_array(hand_str)
    except Exception as e:
        print(f"Error parsing hand: {e}")
        return

    # 1. Calculate Best Discard (which implies calculating current shanten)
    # The method calculate_best_discard takes hand_14
    print("正在计算最佳切牌...")
    # We want to see all candidates to compare 1z vs 3m
    # engine.calculate_best_discard only returns the best. 
    # Let's inspect the internal logic or use a loop here similar to the engine.
    
    hidden_hand_34 = engine._to_34_array(hand_136)
    full_hand_34 = engine._get_full_hand_34(hand_136)
    
    candidates = []
    unique_tiles = [i for i, c in enumerate(hidden_hand_34) if c > 0]
    
    print(f"Candidates indices: {unique_tiles}")
    
    for tile_idx in unique_tiles:
        # Simulate discard
        full_hand_34[tile_idx] -= 1
        
        shanten = engine.shanten_calculator.calculate_shanten(full_hand_34)
        ukeire, ukeire_tiles = engine._get_blind_ukeire(full_hand_34, shanten)
        
        tile_str = engine.index_to_mpsz[tile_idx]
        candidates.append({
            "discard_tile": tile_str,
            "discard_id": tile_idx,
            "shanten": shanten,
            "ukeire": ukeire,
            "ukeire_tiles": ukeire_tiles
        })
        
        # Restore
        full_hand_34[tile_idx] += 1
        
    # Sort
    candidates.sort(key=lambda x: (x['shanten'], -x['ukeire']))
    
    print("-" * 30)
    print("所有切牌选项 (Top 5):")
    for c in candidates[:5]:
        print(f"切: {c['discard_tile']} -> 向听: {c['shanten']}, 进张: {c['ukeire']}") #, tiles: {c['ukeire_tiles']}")
        
    result = candidates[0] if candidates else None

    if result:
        print("-" * 30)

    # 2. Analyze Opportunities (Wait/Melds) - usually for 13 tiles
    # We simulate the state after discarding the best tile
    if result:
        # Find the discard ID in 34 format
        discard_34 = result['discard_id']
        
        # Remove one instance of this tile from hand_136 to get a 13-tile hand
        hand_13 = []
        removed = False
        for t in hand_136:
            if not removed and (t // 4) == discard_34:
                removed = True
                continue
            hand_13.append(t)
            
        print("-" * 30)
        print("切牌后状态分析 (13张状态):")
        opps = engine.analyze_opportunities(hand_13)
        print(f"当前向听数 (13张): {opps['current_shanten']}")
        
        watch_list = opps.get('watch_list', [])
        if watch_list:
            print("吃/碰/杠 观察列表 (Watch List):")
            for item in watch_list:
                print(f"  - 牌: {item['tile']}, 动作: {item['action']}, 切牌建议: {item.get('discard_suggestion')}")
        else:
            print("没有推荐的吃/碰/杠机会。")
            
        keep_list = opps.get('keep_list', [])
        if keep_list:
            print("摸排进张分析 (Top 5):")
            for i, item in enumerate(keep_list[:5]):
                print(f"  {i+1}. 摸: {item['draw']} -> 切: {item['discard']} (向听: {item['shanten']}, 进张: {item['ukeire']})")

if __name__ == "__main__":
    main()
