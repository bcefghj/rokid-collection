import time
import os
import resource
import statistics
import concurrent.futures
from PIL import Image
from inference_sdk import InferenceHTTPClient, InferenceConfiguration
from mahjong.tile import TilesConverter
from mahjong.meld import Meld
from efficiency_engine import EfficiencyEngine

# --- YOLO Helper (Copied from main.py to avoid side effects) ---
YOLO_TO_MPSZ_MAPPING = {
    '1B': '1s', '2B': '2s', '3B': '3s', '4B': '4s', '5B': '5s', '6B': '6s', '7B': '7s', '8B': '8s', '9B': '9s',
    '1C': '1m', '2C': '2m', '3C': '3m', '4C': '4m', '5C': '5m', '6C': '6m', '7C': '7m', '8C': '8m', '9C': '9m',
    '1D': '1p', '2D': '2p', '3D': '3p', '4D': '4p', '5D': '5p', '6D': '6p', '7D': '7p', '8D': '8p', '9D': '9p',
    'EW': '1z', 'SW': '2z', 'WW': '3z', 'NW': '4z',
    'WD': '5z', 'GD': '6z', 'RD': '7z',
    '1F': 'f1', '2F': 'f2', '3F': 'f3', '4F': 'f4',
    '1S': 's1', '2S': 's2', '3S': 's3', '4S': 's4',
}

def convert_to_mpsz(yolo_classes):
    hand_tiles = []
    bonus_tiles = []
    for cls in yolo_classes:
        mpsz = YOLO_TO_MPSZ_MAPPING.get(cls)
        if mpsz:
            if mpsz.startswith('f') or mpsz.startswith('s'):
                bonus_tiles.append(mpsz)
            else:
                hand_tiles.append(mpsz)
        else:
            hand_tiles.append(cls)
    return hand_tiles, bonus_tiles

def get_current_memory_mb():
    """Get current memory usage in MB."""
    usage = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
    # On macOS it is strictly bytes.
    return usage / (1024 * 1024)

def format_opportunities(opportunities):
    if not opportunities:
        return "  无鸣牌机会"
    
    lines = []
    
    # 1. Win List
    if opportunities.get('win_list'):
        lines.append(f"  - 荣和 (Ron): {', '.join(opportunities['win_list'])}")
        
    # 2. Watch List (Chi/Pon/Kan)
    watch_list = opportunities.get('watch_list', [])
    if watch_list:
        lines.append("  - 鸣牌机会 (Melds):")
        for item in watch_list:
            action = item['action']
            tile = item['tile']
            discard = item.get('discard_suggestion', "")
            lines.append(f"    * {action} {tile} -> 切 {discard} (向听数: {item.get('shanten_after')})")
            
    # 3. Keep List
    keep_list = opportunities.get('keep_list', [])
    if keep_list:
        lines.append("  - 改良 (Improvement):")
        # Show top 5
        for item in keep_list[:5]:
            lines.append(f"    * 摸 {item['draw']} -> 切 {item['discard']} (向听: {item['shanten']}, 进张: {item['ukeire']})")
            
    return "\n".join(lines)

def run_complex_analysis(engine):
    # Case: 3467m 2356p 5578s 11z (14 tiles)
    print("\n[测试用例分析]")
    man = '3467'
    pin = '2356'
    sou = '5578' # 5,5,7,8
    honors = '11' # 1,1
    
    print(f"手牌: {man}m {pin}p {sou}s {honors}z")
    ids = TilesConverter.string_to_136_array(man=man, pin=pin, sou=sou, honors=honors)
    
    if len(ids) != 14:
        raise ValueError(f"Hand size incorrect: {len(ids)}. Expected 14.")
    
    print("正在计算最佳切牌...")
    start_time = time.time()
    result = engine.calculate_best_discard(ids, melds=[])
    duration = time.time() - start_time
    
    if result:
        print("-" * 40)
        print(f"推荐切牌: {result['discard_tile']}")
        print(f"当前向听数 (切牌后): {result['shanten']}")
        print(f"有效进张数 (Ukeire): {result['ukeire']}")
        print(f"有效进张列表: {', '.join(result.get('ukeire_tiles', []))}")
        print(f"计算耗时: {duration:.4f} 秒")
        print("-" * 40)
        
        if 'opportunities' in result:
            print("[切牌后的机会分析 (对手打出牌时/下轮摸牌)]:")
            print(format_opportunities(result['opportunities']))
    else:
        print("未找到切牌建议")

def benchmark():
    engine = EfficiencyEngine()
    try:
        run_complex_analysis(engine)
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    benchmark()
