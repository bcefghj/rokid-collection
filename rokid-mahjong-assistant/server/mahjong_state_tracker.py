from typing import List, Optional, Dict, Any, Union
import logging
from mahjong.meld import Meld
from mahjong.tile import TilesConverter
from collections import Counter

logger = logging.getLogger(__name__)

class MahjongLogicError(Exception):
    """Custom exception for Mahjong logic errors."""
    pass

class MahjongStateTracker:
    def __init__(self):
        # self.current_hidden_hand: List[int] (136 format)
        self.current_hidden_hand: Optional[List[int]] = None
        # self.current_melded_tiles: List[int] (136 format) - representing visible melds
        self.current_melded_tiles: List[int] = []
        self.meld_history: List[Meld] = []
        self.action_history: List[Dict[str, Any]] = []
        # Global visible tiles (seen on river, other players' melds, dora indicators, etc.)
        # Index 0-33.
        self.visible_tiles: List[int] = [0] * 34

    def update_visible_tiles(self, events: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        Update visible tiles based on external events (e.g. from voice analysis).
        Events format: [{"type": "DISCARD", "tile": "5s"}, ...]
        """
        updated_count = 0
        details = []
        
        for event in events:
            tile_str = event.get("tile")
            action_type = event.get("type")
            
            if not tile_str:
                continue
                
            # Convert tile string (e.g. '5s') to 34-index
            try:
                # TilesConverter returns list of 136-indices.
                ids = TilesConverter.one_line_string_to_136_array(tile_str)
                if not ids:
                    continue
                
                if action_type == "PON":
                    # Hardcoded rule: PON means 3 visible tiles
                    # We take the first tile as representative
                    tile_idx = ids[0] // 4
                    if 0 <= tile_idx < 34:
                        self.visible_tiles[tile_idx] += 3
                        updated_count += 3
                        details.append(f"{action_type}: {tile_str} (+3)")

                elif action_type == "KAN":
                    # Hardcoded rule: KAN means 4 visible tiles
                    tile_idx = ids[0] // 4
                    if 0 <= tile_idx < 34:
                        self.visible_tiles[tile_idx] += 4
                        updated_count += 4
                        details.append(f"{action_type}: {tile_str} (+4)")

                elif action_type in ["CHI", "DISCARD"]:
                    # Standard rule: Add 1 for each tile parsed
                    # e.g. "1m2m3m" for CHI -> +1 for 1m, +1 for 2m, +1 for 3m
                    # e.g. "5s" for DISCARD -> +1 for 5s
                    for t_id in ids:
                        tile_idx = t_id // 4
                        if 0 <= tile_idx < 34:
                            self.visible_tiles[tile_idx] += 1
                            updated_count += 1
                    details.append(f"{action_type}: {tile_str}")

                else:
                    # Fallback
                    tile_idx = ids[0] // 4
                    if 0 <= tile_idx < 34:
                        self.visible_tiles[tile_idx] += 1
                        updated_count += 1
                        details.append(f"{action_type}: {tile_str}")
                    
            except Exception as e:
                logger.error(f"Error updating visible tile for {tile_str}: {e}")
                
        return {"updated_count": updated_count, "details": details}

    def _normalize_hand(self, hand_input: Union[List[int], List[int], str]) -> List[int]:
        """
        Normalize input (hand or melds) to a list of 136-tile indices.
        """
        if isinstance(hand_input, str):
            return TilesConverter.one_line_string_to_136_array(hand_input)
        
        if isinstance(hand_input, list):
            if not hand_input:
                return []
            
            # Check if it's a list of strings (e.g., ['1m', '2m'])
            if hand_input and isinstance(hand_input[0], str):
                # Join and convert
                # Note: TilesConverter expects one line string like '1m2m3m'
                full_str = "".join(hand_input)
                return TilesConverter.one_line_string_to_136_array(full_str)

            # Check if it's a 34-count array (length 34)
            if len(hand_input) == 34:
                hand_136 = []
                for tile_34_index, count in enumerate(hand_input):
                    for i in range(count):
                        hand_136.append(tile_34_index * 4 + i)
                return sorted(hand_136)
            
            # Assume it's already a list of IDs (136 format)
            return sorted(hand_input)
            
        raise MahjongLogicError("Invalid hand input format")

    def _get_diff_tiles(self, old_list: List[int], new_list: List[int]) -> List[int]:
        """
        Calculate newly added tiles (new_list - old_list) based on 34-tile types.
        Returns a list of 136-IDs representing the added tiles.
        """
        old_c = Counter([t // 4 for t in old_list])
        new_c = Counter([t // 4 for t in new_list])
        
        diff_ids = []
        # Find which 34-indices increased in count
        for t34, count in new_c.items():
            old_count = old_c.get(t34, 0)
            if count > old_count:
                diff = count - old_count
                # We need to find 'diff' number of tiles of type t34 in new_list
                # that are potentially "new".
                # To be precise with 136-IDs, we try to pick ones not in old_list if possible,
                # or just pick any matching t34 from new_list.
                # Since we normalize inputs freshly each time, 136-IDs might shift (0 vs 1).
                # So we just construct representative IDs or pick from new_list.
                
                # Pick specific IDs from new_list that match t34
                candidates = [t for t in new_list if t // 4 == t34]
                # Take the last 'diff' ones (arbitrary, but works for representation)
                diff_ids.extend(candidates[:diff])
                
        return sorted(diff_ids)

    def update_state(self, new_hand_input: Union[List[int], str], new_melds_input: Union[List[int], str] = [], incoming_tile: Optional[int] = None) -> Dict[str, Any]:
        """
        Update state based on hidden hand and explicit meld inputs.
        
        Args:
            new_hand_input: Current hidden hand (Top half of screen).
            new_melds_input: Current visible melds (Bottom half of screen).
            incoming_tile: Optional 136-ID of the tile involved in the action (drawn or called).
        """
        try:
            new_hand = self._normalize_hand(new_hand_input)
            new_melds = self._normalize_hand(new_melds_input)
        except Exception as e:
            raise MahjongLogicError(f"Failed to normalize input: {e}")

        # Step A: Initialization
        if self.current_hidden_hand is None:
            # Validate integrity of the first frame
            # We expect 13 or 14 tiles (adjusted for Kans)
            # Since it's init, we assume no Ankans in history yet.
            
            new_melds_counter = Counter([t // 4 for t in new_melds])
            num_open_kans = sum(1 for count in new_melds_counter.values() if count == 4)
            visible_count = len(new_hand) + len(new_melds)
            adjusted_count = visible_count - num_open_kans
            
            if adjusted_count not in [13, 14]:
                 return {
                     "action": "WARNING", 
                     "warning": f"Initial state invalid: Found {adjusted_count} tiles (Expected 13/14). Please ensure all tiles are visible."
                 }

            self.current_hidden_hand = new_hand
            self.current_melded_tiles = new_melds
            
            hand_len = len(new_hand)
            # Basic init logic
            action = "INIT_TURN" if hand_len == 14 else "INIT_WAIT"
            
            result = {"action": action, "hand": new_hand, "melds": new_melds}
            self.action_history.append(result)
            return result

        # Step B: Check for Meld Changes (Priority 1)
        # We trust the explicit meld list. If it grew, a meld happened.
        # Note: We assume melds never disappear (except maybe correction, which we ignore for now).
        
        old_melds = self.current_melded_tiles
        added_meld_tiles = self._get_diff_tiles(old_melds, new_melds)
        
        if len(added_meld_tiles) > 0:
            # Validation: Check if added tiles are valid meld shapes
            if len(added_meld_tiles) not in [1, 3, 4]:
                 return {
                     "action": "WARNING",
                     "warning": f"Unstable meld detected: Added {len(added_meld_tiles)} tiles. Please retry."
                 }
            
            # Validation: Check total tile count (Upper Bound)
            # If we accept this meld, will we have too many tiles?
            # Note: We haven't updated hand yet, so we use new_hand (if valid) or old_hand?
            # User provides new_hand and new_melds. We should check consistency of the input frame.
            
            new_melds_counter = Counter([t // 4 for t in new_melds])
            num_open_kans = sum(1 for count in new_melds_counter.values() if count == 4)
            num_ankans = sum(1 for m in self.meld_history if m.type == Meld.KAN and not m.opened)
            
            visible_count = len(new_hand) + len(new_melds)
            adjusted_count = visible_count - num_open_kans + (3 * num_ankans)
            
            if adjusted_count > 14:
                 return {
                     "action": "WARNING",
                     "warning": f"Too many tiles detected: {adjusted_count} (Max 14). Potential ghost tiles."
                 }

            # Action detected in Meld area
            action_type = "UNKNOWN_MELD"
            meld_obj = None
            
            # Analyze added tiles
            if len(added_meld_tiles) == 1:
                # KAKAN (Added Kan) - 1 tile added to existing PON
                t34 = added_meld_tiles[0] // 4
                for idx, m in enumerate(self.meld_history):
                    if m.type == Meld.PON and (m.tiles[0] // 4) == t34:
                        action_type = "KAKAN"
                        # Upgrade PON to KAN
                        new_tiles = sorted(m.tiles + added_meld_tiles)
                        meld_obj = Meld(Meld.SHOUMINKAN, new_tiles, True, added_meld_tiles[0], m.who, m.from_who)
                        self.meld_history[idx] = meld_obj
                        break
            
            elif len(added_meld_tiles) == 3:
                # CHI or PON
                t1, t2, t3 = sorted(added_meld_tiles)
                if (t1 // 4) == (t2 // 4) == (t3 // 4):
                    action_type = "PON"
                    # Determine called tile
                    called = incoming_tile if incoming_tile is not None else t1
                    meld_obj = Meld(Meld.PON, added_meld_tiles, True, called, 0, 0)
                    self.meld_history.append(meld_obj)
                else:
                    # Assume CHI (check consecutiveness?)
                    # 3 tiles, different. likely Chi.
                    action_type = "CHI"
                    called = incoming_tile if incoming_tile is not None else t1 # Fallback
                    meld_obj = Meld(Meld.CHI, added_meld_tiles, True, called, 0, 0)
                    self.meld_history.append(meld_obj)
            
            elif len(added_meld_tiles) == 4:
                # DAIMINKAN (Open Kan)
                action_type = "DAIMINKAN"
                called = incoming_tile if incoming_tile is not None else added_meld_tiles[0]
                meld_obj = Meld(Meld.KAN, added_meld_tiles, True, called, 0, 0)
                self.meld_history.append(meld_obj)
            
            # Update State
            self.current_hidden_hand = new_hand
            self.current_melded_tiles = new_melds
            
            result = {"action": action_type, "added_melds": added_meld_tiles}
            self.action_history.append(result)
            return result

        # Step C: Check Hand Changes (Priority 2 - if no meld change)
        old_hand = self.current_hidden_hand
        
        # Calculate raw difference
        len_diff = len(new_hand) - len(old_hand)
        
        # Use existing _diff_hands logic (implemented inline or reuse helper if strictly needed, 
        # but here simple length/content check is enough for Draw/Discard)
        
        # We need to detect ANKAN specifically:
        # Condition: Hand len -3 (Lose 4, Gain 1). Meld Area UNCHANGED.
        # Also supports "Draw + Ankan" combined transition (13 -> 11), which is -2.
        if len_diff == -3 or len_diff == -2:
            # Check if we lost 4 identical tiles
            # We need detailed diff
            lost_ids = []
            old_c = Counter([t//4 for t in old_hand])
            new_c = Counter([t//4 for t in new_hand])
            
            possible_ankan_tile = None
            for t34, count in old_c.items():
                if count >= 4 and new_c.get(t34, 0) <= count - 4:
                    possible_ankan_tile = t34
                    break
            
            if possible_ankan_tile is not None:
                # Confirmed ANKAN
                action_type = "ANKAN"
                # Create Meld object (Closed Kan)
                # We need 4 tiles of that type
                ankan_tiles = [t for t in old_hand if t // 4 == possible_ankan_tile][:4]
                meld_obj = Meld(Meld.KAN, sorted(ankan_tiles), False, None, 0, 0)
                self.meld_history.append(meld_obj)
                
                self.current_hidden_hand = new_hand
                # Meld tiles do NOT update because Ankan is not in visible melds (per requirement)
                
                result = {"action": "ANKAN", "tiles": ankan_tiles}
                self.action_history.append(result)
                return result

        # Standard Draw/Discard
        if len_diff == 1:
            action_type = "DRAW"
            result_extras = {"gained_tiles": self._get_diff_tiles(old_hand, new_hand)}
        elif len_diff == -1:
            action_type = "DISCARD"
            result_extras = {"lost_tiles": self._get_diff_tiles(new_hand, old_hand)}
        elif len_diff == 0:
            action_type = "NO_OP"
            result_extras = {}
        else:
            # Fallback for undefined states (e.g. multiple moves missed)
            action_type = "UNKNOWN_STATE_CHANGE"
            result_extras = {}

        # Final Validation: Check for Missing Tiles
        # Note: ANKAN returns early, so we are only handling standard moves or errors here.
        new_melds_counter = Counter([t // 4 for t in new_melds])
        num_open_kans = sum(1 for count in new_melds_counter.values() if count == 4)
        num_ankans = sum(1 for m in self.meld_history if m.type == Meld.KAN and not m.opened)
        
        visible_count = len(new_hand) + len(new_melds)
        adjusted_count = visible_count - num_open_kans + (3 * num_ankans)
        
        if adjusted_count < 13:
             return {
                 "action": "WARNING",
                 "warning": f"Missing tiles detected: {adjusted_count} (Expected 13/14). Please ensure all tiles are visible."
             }

        self.current_hidden_hand = new_hand
        # Melds unchanged
        
        result = {"action": action_type, **result_extras}
        if action_type != "NO_OP":
             self.action_history.append(result)
             
        return result
