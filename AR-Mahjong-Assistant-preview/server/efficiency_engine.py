from typing import List, Dict, Any, Tuple, Optional
from mahjong.shanten import Shanten
from mahjong.tile import TilesConverter
from mahjong.meld import Meld

class EfficiencyEngine:
    def __init__(self):
        self.shanten_calculator = Shanten()
        
        # Global visible tiles (seen on river, other players' melds, etc.)
        # This is maintained by external calls to update_tile_count
        self.visible_tiles = [0] * 34
        
        # MPSZ Mapping for string representation
        self.index_to_mpsz = []
        for s in ['m', 'p', 's', 'z']:
            count = 7 if s == 'z' else 9
            for i in range(1, count + 1):
                self.index_to_mpsz.append(f"{i}{s}")

    def reset_visible_tiles(self):
        """Reset the global visible tile counters to zero (for new round)."""
        self.visible_tiles = [0] * 34

    def update_tile_count(self, tile_idx: int, delta: int):
        """
        Update the visible count for a specific tile index.
        Args:
            tile_idx: 0-33 index of the tile
            delta: Amount to change (+1 for discard/meld, -1 for undo)
        """
        if 0 <= tile_idx < 34:
            self.visible_tiles[tile_idx] += delta
            # Ensure non-negative
            if self.visible_tiles[tile_idx] < 0:
                self.visible_tiles[tile_idx] = 0

    def _to_34_array(self, hand_136: List[int]) -> List[int]:
        """Convert 136-tile format list to 34-tile count array."""
        hand_34 = [0] * 34
        for tile in hand_136:
            # 136 format: 0-135. 0-3 is 1m, 4-7 is 2m, etc.
            hand_34[tile // 4] += 1
        return hand_34

    def _get_full_hand_34(self, hidden_hand_136: List[int], melds: Optional[List[Meld]] = None) -> List[int]:
        """Combine hidden hand and melds into a 34-tile count array."""
        hand_34 = self._to_34_array(hidden_hand_136)
        if melds:
            for meld in melds:
                # Meld.tiles is a list of 136 IDs
                for tile in meld.tiles:
                    hand_34[tile // 4] += 1
        return hand_34

    def _get_ukeire(self, hand_34: List[int], current_shanten: int) -> Tuple[int, List[str]]:
        """
        Calculate Ukeire (effective tiles) considering global visible tiles.
        Returns: (total_count, list_of_tile_strings)
        """
        ukeire_count = 0
        ukeire_tiles = []
        
        # Iterate all 34 tiles
        for i in range(34):
            if hand_34[i] >= 4:
                continue
                
            # Try adding this tile
            hand_34[i] += 1
            new_shanten = self.shanten_calculator.calculate_shanten(hand_34)
            hand_34[i] -= 1 # Restore
            
            # If shanten improved
            if new_shanten < current_shanten:
                # Calculate remaining tiles: Total(4) - (InHand + VisibleOnTable)
                # VisibleOnTable (self.visible_tiles) includes river, other melds, etc.
                visible_count = self.visible_tiles[i]
                in_hand_count = hand_34[i]
                
                remaining = 4 - (in_hand_count + visible_count)
                if remaining < 0:
                    remaining = 0
                    
                ukeire_count += remaining
                ukeire_tiles.append(self.index_to_mpsz[i])
                
        return ukeire_count, ukeire_tiles

    def calculate_best_discard(self, hand_14: List[int], melds: Optional[List[Meld]] = None) -> Dict[str, Any]:
        """
        Calculate the best discard for a turn state hand.
        Args:
            hand_14: The current hidden hand (136-ID list).
            melds: Optional list of Melds (open sets).
        """
        hidden_hand_34 = self._to_34_array(hand_14)
        full_hand_34 = self._get_full_hand_34(hand_14, melds)
        
        candidates = []
        
        # Iterate unique tiles in HIDDEN hand to discard
        unique_tiles = [i for i, c in enumerate(hidden_hand_34) if c > 0]
        
        for tile_idx in unique_tiles:
            # Simulate discard from FULL hand
            full_hand_34[tile_idx] -= 1
            # The discarded tile becomes visible (in river), so it is not in wall.
            # We must update visible_tiles temporarily to ensure _get_ukeire is correct.
            self.visible_tiles[tile_idx] += 1
            
            # Calculate properties of the remaining tiles
            shanten = self.shanten_calculator.calculate_shanten(full_hand_34)
            ukeire, ukeire_tiles = self._get_ukeire(full_hand_34, shanten)
            
            candidates.append({
                "discard_tile": self.index_to_mpsz[tile_idx],
                "discard_id": tile_idx, # using 34-index as ID representative
                "shanten": shanten,
                "ukeire": ukeire,
                "ukeire_tiles": ukeire_tiles
            })
            
            # Restore
            self.visible_tiles[tile_idx] -= 1
            full_hand_34[tile_idx] += 1
            
        # Sort candidates:
        # Priority 1: Shanten (min)
        # Priority 2: Ukeire (max)
        candidates.sort(key=lambda x: (x['shanten'], -x['ukeire']))
        
        if not candidates:
            return None
            
        best_candidate = candidates[0]
        
        # --- NEW: Calculate opportunities for the BEST discard ---
        # We need to reconstruct the hand_13 that results from this discard
        # best_candidate['discard_id'] is the 34-index of the tile to discard
        discard_34_idx = best_candidate['discard_id']
        
        # Find a matching 136-index in the original hand_14 to remove
        # We just need ONE instance of that tile type
        tile_to_remove = -1
        for t_136 in hand_14:
            if t_136 // 4 == discard_34_idx:
                tile_to_remove = t_136
                break
                
        if tile_to_remove != -1:
            # Create the 13-tile hand
            hand_13 = list(hand_14)
            hand_13.remove(tile_to_remove)
            
            # Run analysis
            opportunities = self.analyze_opportunities(hand_13, melds)
            
            # Attach to result
            best_candidate['opportunities'] = opportunities
            
        return best_candidate

    def generate_lookup_table(self, hand_13: List[int], melds: Optional[List[Meld]] = None) -> Dict[str, Any]:
        """
        Generate lookup table for all possible draws for a waiting state hand.
        """
        lookup_table = {}
        # We need to simulate drawing to the full hand
        full_hand_34 = self._get_full_hand_34(hand_13, melds)
        
        # Iterate all possible draws (0-33)
        for draw_idx in range(34):
            # Skip if we already have 4 of this tile (impossible to draw)
            if full_hand_34[draw_idx] >= 4:
                continue
                
            draw_str = self.index_to_mpsz[draw_idx]
            
            # Simulate drawing
            full_hand_34[draw_idx] += 1
            
            # Internal logic of best discard using 34-array
            # We can only discard from HIDDEN hand.
            # But wait, we don't have hidden_hand_34 updated here easily.
            # Actually, we can check if full_hand_34[idx] > melds[idx].
            # Simpler: We know the draw is in the hand (we just added it).
            # If the drawn tile is added, it is part of the hidden hand.
            # The discardable tiles are: Original Hidden Hand + Drawn Tile.
            # So effectively, all tiles in full_hand_34 minus the locked meld tiles.
            
            # Calculate locked counts
            locked_34 = [0] * 34
            if melds:
                for m in melds:
                    for t in m.tiles:
                        locked_34[t // 4] += 1
            
            candidate_list = []
            
            # Iterate unique tiles in FULL hand
            unique_tiles = [i for i, c in enumerate(full_hand_34) if c > 0]
            
            for discard_idx in unique_tiles:
                # Check if this tile is discardable (count in full > count in locked)
                if full_hand_34[discard_idx] <= locked_34[discard_idx]:
                    continue # This tile type is fully locked in melds
                
                full_hand_34[discard_idx] -= 1
                self.visible_tiles[discard_idx] += 1
                shanten = self.shanten_calculator.calculate_shanten(full_hand_34)
                ukeire, _ = self._get_ukeire(full_hand_34, shanten)
                self.visible_tiles[discard_idx] -= 1
                full_hand_34[discard_idx] += 1
                
                candidate_list.append((shanten, -ukeire, discard_idx))
            
            if candidate_list:
                candidate_list.sort() # Sorts by shanten asc, then -ukeire asc (ukeire desc)
                best_shanten, best_neg_ukeire, best_discard_idx = candidate_list[0]
                
                lookup_table[draw_str] = {
                    "discard": self.index_to_mpsz[best_discard_idx],
                    "ukeire": -best_neg_ukeire,
                    "shanten": best_shanten
                }
            
            # Restore
            full_hand_34[draw_idx] -= 1
            
        return lookup_table

    def _simulate_meld_and_discard(self, full_hand_34: List[int], locked_34: List[int], incoming_tile_idx: int, meld_indices: List[int]) -> Tuple[int, int, int]:
        """
        Simulate declaring a meld (Pon/Chi) and finding the best discard.
        Args:
            full_hand_34: Current full hand counts.
            locked_34: Current locked counts.
            incoming_tile_idx: The tile being called.
            meld_indices: The 3 indices forming the meld (including incoming).
        Returns:
            (best_shanten, best_ukeire, best_discard_idx)
        """
        # 1. Add called tile
        full_hand_34[incoming_tile_idx] += 1
        
        # 2. Lock the meld tiles
        for idx in meld_indices:
            locked_34[idx] += 1
            
        # 3. Find best discard
        best_shanten = 99
        best_ukeire = -1
        best_discard_idx = -1
        
        unique_tiles = [t for t, c in enumerate(full_hand_34) if c > 0]
        
        for discard_idx in unique_tiles:
            # Check if discardable
            if full_hand_34[discard_idx] <= locked_34[discard_idx]:
                continue
                
            full_hand_34[discard_idx] -= 1
            self.visible_tiles[discard_idx] += 1
            
            s = self.shanten_calculator.calculate_shanten(full_hand_34)
            u, _ = self._get_ukeire(full_hand_34, s)
            
            if s < best_shanten:
                best_shanten = s
                best_ukeire = u
                best_discard_idx = discard_idx
            elif s == best_shanten:
                if u > best_ukeire:
                    best_ukeire = u
                    best_discard_idx = discard_idx
            
            self.visible_tiles[discard_idx] -= 1
            full_hand_34[discard_idx] += 1
            
        # Restore state
        for idx in meld_indices:
            locked_34[idx] -= 1
        full_hand_34[incoming_tile_idx] -= 1
        
        return best_shanten, best_ukeire, best_discard_idx

    def analyze_opportunities(self, hand_13: List[int], melds: Optional[List[Meld]] = None) -> Dict[str, Any]:
        """
        Analyze opportunities for a waiting state hand:
        - Win list (if tenpai)
        - Watch list (Pon/Kan/Chi)
        - Keep list (based on lookup table)
        """
        full_hand_34 = self._get_full_hand_34(hand_13, melds)
        current_shanten = self.shanten_calculator.calculate_shanten(full_hand_34)
        current_ukeire, _ = self._get_ukeire(full_hand_34, current_shanten)
        
        result = {
            "current_shanten": current_shanten,
            "win_list": [],
            "watch_list": [],
            "keep_list": []
        }
        
        # 1. Check Win (if Shanten is 0)
        if current_shanten == 0:
            _, winning_tiles = self._get_ukeire(full_hand_34, current_shanten)
            result["win_list"] = winning_tiles
            
        # 2. Check Watch List (Pon/Kan/Chi)
        hidden_hand_34 = self._to_34_array(hand_13)
        
        # Calculate initial locked counts
        locked_34 = [0] * 34
        if melds:
            for m in melds:
                for t in m.tiles:
                    locked_34[t // 4] += 1
        
        for i in range(34):
            tile_str = self.index_to_mpsz[i]
            
            # --- Check Pon ---
            if hidden_hand_34[i] >= 2:
                best_pon_shanten, best_pon_ukeire, best_pon_discard = self._simulate_meld_and_discard(
                    full_hand_34, locked_34, i, [i, i, i]
                )
                
                should_pon = False
                if best_pon_shanten < current_shanten:
                    should_pon = True
                elif best_pon_shanten == current_shanten and best_pon_ukeire > current_ukeire:
                    should_pon = True
                    
                if should_pon:
                    discard_str = self.index_to_mpsz[best_pon_discard] if best_pon_discard != -1 else ""
                    result["watch_list"].append({
                        "tile": tile_str,
                        "action": "PON",
                        "shanten_after": best_pon_shanten,
                        "ukeire_after": best_pon_ukeire,
                        "discard_suggestion": discard_str
                    })
            
            # --- Check Kan (Daiminkan) ---
            if hidden_hand_34[i] == 3:
                # Simulate Kan: Add tile, total 14. 
                # Don't check Ukeire to avoid "15 tiles" crash.
                full_hand_34[i] += 1
                kan_shanten = self.shanten_calculator.calculate_shanten(full_hand_34)
                full_hand_34[i] -= 1
                
                # Kan is always worth considering if it doesn't break Tenpai
                should_kan = False
                if kan_shanten < current_shanten:
                    should_kan = True
                elif kan_shanten == current_shanten:
                    # Same shanten, usually Kan is OK for score
                    should_kan = True
                    
                if should_kan:
                     result["watch_list"].append({
                        "tile": tile_str,
                        "action": "KAN",
                        "shanten_after": kan_shanten,
                        "ukeire_after": -1 # Unknown
                    })

            # --- Check Chi (Chow) ---
            # Only for m, p, s (indices 0-26)
            if i < 27:
                suit = i // 9
                
                # Chi Combinations: 
                # 1. Left: [i-2, i-1, i] -> requires i-2, i-1 in hand
                # 2. Middle: [i-1, i, i+1] -> requires i-1, i+1 in hand
                # 3. Right: [i, i+1, i+2] -> requires i+1, i+2 in hand
                
                # Helper to check existence in hidden hand
                def has_hidden(idx):
                    return 0 <= idx < 34 and hidden_hand_34[idx] > 0
                
                combinations = []
                
                # Left: (i-2, i-1) + i
                if i % 9 >= 2 and has_hidden(i-2) and has_hidden(i-1):
                    combinations.append([i-2, i-1, i])
                    
                # Middle: (i-1, i+1) + i
                if i % 9 >= 1 and i % 9 <= 7 and has_hidden(i-1) and has_hidden(i+1):
                    combinations.append([i-1, i, i+1])
                    
                # Right: (i+1, i+2) + i
                if i % 9 <= 6 and has_hidden(i+1) and has_hidden(i+2):
                    combinations.append([i, i+1, i+2])
                    
                for combo in combinations:
                    best_chi_shanten, best_chi_ukeire, best_chi_discard = self._simulate_meld_and_discard(
                        full_hand_34, locked_34, i, combo
                    )
                    
                    should_chi = False
                    if best_chi_shanten < current_shanten:
                        should_chi = True
                    elif best_chi_shanten == current_shanten and best_chi_ukeire > current_ukeire:
                        should_chi = True
                        
                    if should_chi:
                        discard_str = self.index_to_mpsz[best_chi_discard] if best_chi_discard != -1 else ""
                        
                        # Identify used tiles (tiles in combo that are not the incoming tile i)
                        used_indices = [x for x in combo if x != i]
                        used_tiles = [self.index_to_mpsz[x] for x in used_indices]
                        
                        result["watch_list"].append({
                            "tile": tile_str,
                            "action": "CHI",
                            "shanten_after": best_chi_shanten,
                            "ukeire_after": best_chi_ukeire,
                            "discard_suggestion": discard_str,
                            "used_tiles": used_tiles
                        })

        # 3. Check Keep List
        lookup = self.generate_lookup_table(hand_13, melds)
        for draw_tile, data in lookup.items():
            if data["discard"] != draw_tile:
                result["keep_list"].append({
                    "draw": draw_tile,
                    "discard": data["discard"],
                    "shanten": data["shanten"],
                    "ukeire": data["ukeire"]
                })
                
        # Sort keep list for display
        # Priority: Shanten (asc) -> Ukeire (desc) -> Tile Order
        def sort_key(item):
            s = item["draw"]
            # m, p, s, z order
            suit_order = {'m': 0, 'p': 1, 's': 2, 'z': 3}
            tile_val = 999
            if s:
                tile_val = suit_order.get(s[-1], 9) * 10 + int(s[:-1])
            
            # Tuple comparison: (shanten, -ukeire, tile_val)
            # Python sorts tuples element by element.
            return (item['shanten'], -item['ukeire'], tile_val)
            
        result["keep_list"].sort(key=sort_key)
        
        return result

def format_suggestions(engine_result: Dict[str, Any], result_type: str = "opportunity") -> str:
    """
    Format the efficiency engine result into a user-friendly string.
    Args:
        engine_result: The dictionary returned by calculate_best_discard or analyze_opportunities
        result_type: "discard" (14 tiles) or "opportunity" (13 tiles)
    """
    if not engine_result:
        return "完成 / 未找到切牌建议"

    lines = []

    # === Case 1: Discard Decision (14 tiles) ===
    if result_type == "discard":
        tile = engine_result.get('discard_tile', '?')
        shanten = engine_result.get('shanten', -1)
        ukeire = engine_result.get('ukeire', -1)
        
        lines.append(f"切 {tile} (向听数: {shanten}, 进张数: {ukeire})")
        
        # Check for opportunities after this discard
        if 'opportunities' in engine_result:
            opp_lines = format_suggestions(engine_result['opportunities'], result_type="opportunity")
            if opp_lines:
                lines.append(opp_lines)
                
        return "  ".join(lines)

    # === Case 2: Opportunity Analysis (13 tiles) ===
    # result keys: win_list, watch_list, keep_list
    
    # 1. Win List
    if engine_result.get('win_list'):
        wins = ", ".join(engine_result['win_list'])
        lines.append(f"胡: {wins}")

    # 2. Keep List (Improvement)
    keep_list = engine_result.get('keep_list', [])
    if keep_list:
        # Take top 5 for display
        top_keeps = keep_list[:5] 
        
        keep_grouped = {}
        for item in top_keeps:
            draw = item['draw']
            discard = item['discard']
            
            if discard not in keep_grouped:
                keep_grouped[discard] = []
            keep_grouped[discard].append(draw)
            
        for discard_tile, draws in keep_grouped.items():
            draws_str = ", ".join(draws)
            lines.append(f"摸: {draws_str} (切 {discard_tile})")

    # 3. Watch List (Chi/Pon/Kan)
    watch_list = engine_result.get('watch_list', [])
    if watch_list:
        # Group by Action -> Discard -> List of (tile, used_tiles)
        grouped = {} 
        action_order = ["CHI", "PON", "KAN"] # Priority order
        action_map = {"CHI": "吃", "PON": "碰", "KAN": "杠"}
        
        for item in watch_list:
            action = item['action']
            tile = item['tile']
            discard = item.get('discard_suggestion', "")
            used_tiles = item.get('used_tiles', [])
            ukeire = item.get('ukeire_after', -1)
            
            if action not in grouped:
                grouped[action] = {}
            if discard not in grouped[action]:
                grouped[action][discard] = []
            
            # Store tuple (tile, used_tiles, ukeire)
            grouped[action][discard].append((tile, used_tiles, ukeire))
            
        # Generate lines
        for action in action_order:
            if action in grouped:
                action_name = action_map.get(action, action)
                discards = grouped[action]
                
                for discard_tile, items in discards.items():
                    # Group by tile (t) to merge strategies
                    tile_groups = {}
                    for t, u, uk in items:
                        if t not in tile_groups:
                            tile_groups[t] = []
                        if u:
                            tile_groups[t].append((u, uk))
                    
                    # Sort tiles naturally
                    sorted_tiles = sorted(tile_groups.keys(), key=lambda x: (x[-1], x[:-1]))
                    
                    item_strs = []
                    for t in sorted_tiles:
                        options = tile_groups[t]
                        
                        if not options:
                            item_strs.append(t)
                            continue

                        # Filter options: Keep only those with MAX ukeire
                        if options:
                            max_ukeire = max(opt[1] for opt in options)
                            # Only keep used_tiles where ukeire is max
                            used_lists = [opt[0] for opt in options if opt[1] == max_ukeire]
                        else:
                            used_lists = []

                        # Format each used_tiles list
                        formatted_used_options = []
                        
                        # Sort used options for consistency
                        used_lists.sort(key=lambda u: "".join(u))

                        for u in used_lists:
                            # Format used tiles: ['2m', '3m'] -> '23m'
                            if len(u) == 2 and u[0][-1] == u[1][-1]:
                                suffix = u[0][-1]
                                digits = u[0][:-1] + u[1][:-1]
                                used_str = digits + suffix
                                formatted_used_options.append(used_str)
                            else:
                                used_str = "".join(u)
                                formatted_used_options.append(used_str)
                        
                        # Join multiple options with '/'
                        combined_used = "/".join(formatted_used_options)
                        item_strs.append(f"{t}({combined_used})")
                            
                    tiles_str = ", ".join(item_strs)
                    
                    line = f"{action_name}: {tiles_str}"
                    if discard_tile:
                        line += f" (切 {discard_tile})"
                    lines.append(line)

    if not lines:
        return "摸牌 / 默听"
        
    return "  ".join(lines)
