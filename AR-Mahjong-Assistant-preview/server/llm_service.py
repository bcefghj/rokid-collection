import json
import os
import logging
from typing import List, Dict, Any
from openai import OpenAI
import re

logger = logging.getLogger(__name__)

class LLMService:
    def __init__(self, api_key: str = None, base_url: str = None, model: str = "gpt-4o"):
        self.api_key = api_key
        self.base_url = base_url
        self.model = model
        
        if self.api_key:
            self.client = OpenAI(api_key=self.api_key, base_url=self.base_url)
        else:
            logger.warning("LLMService initialized without API_KEY. LLM features will be disabled.")
            self.client = None

    def analyze_game_events(self, text: str) -> List[Dict[str, Any]]:
        """
        Analyze transcribed text to extract Mahjong game events.
        Returns a list of events like: [{"type": "DISCARD", "tile": "5s"}]
        """
        if not self.client:
            logger.warning("LLM Client not initialized. Skipping analysis.")
            return []

        prompt = f"""
你是一名专业的麻将裁判。请分析以下语音转录文本，提取其中的牌局事件。

注意：仅提取明确的牌局操作指令。忽略闲聊、疑问句（如"你吃了吗"）以及对他人动作的询问。

文本内容: "{text}"

请提取以下类型的事件（严格遵循动作定义）：
1. 切牌 (DISCARD): 提及牌名但没有跟随动作词。例如 "五万", "打发财", "3索"。
2. 吃 (CHI): 动作词 "吃" 必须紧随在牌名前后。tile 必须是三张顺子牌的组合 (如 "1m2m3m")。
3. 碰 (PON): 动作词 "碰" 必须紧随在牌名前后。
4. 杠 (KAN): 动作词 "杠" 必须紧随在牌名前后。

常见别名知识库：
- 幺鸡/小鸡 -> 1s (一索)
- 大饼 -> 1p (一筒)
- 白板 -> 5z (白)
- 红中 -> 7z (中)
- 发财 -> 6z (发)

规则：
- 如果出现自我更正（如“打五万...不对，打八条”），只输出最后确认的动作。
- 忽略闲聊、疑问句（如“你吃了吗”）以及对他人的询问。
- 严格输出 JSON 数组，无 Markdown。
- 牌名必须严格使用 mpsz 格式，完整对照表如下：
  - 万子: 1m(一万), 2m(二万), 3m(三万), 4m(四万), 5m(五万), 6m(六万), 7m(七万), 8m(八万), 9m(九万)
  - 筒子: 1p(一筒), 2p(二筒), 3p(三筒), 4p(四筒), 5p(五筒), 6p(六筒), 7p(七筒), 8p(八筒), 9p(九筒)
  - 索子: 1s(一索/一条), 2s(二索/二条), 3s(三索/三条), 4s(四索/四条), 5s(五索/五条), 6s(六索/六条), 7s(七索/七条), 8s(八索/八条), 9s(九索/九条)
  - 字牌: 1z(东风), 2z(南风), 3z(西风), 4z(北风), 5z(白板/白), 6z(发财/发), 7z(红中/中)
  - 严禁使用 "w" (如 "5w")，必须转换为 "m" (如 "5m")。

示例参考：

输入: "打五索"
输出: [{{"type": "DISCARD", "tile": "5s"}}]

输入: "碰发财"
输出: [{{"type": "PON", "tile": "6z"}}]

输入: "杠八万"
输出: [{{"type": "KAN", "tile": "8m"}}]

输入: "吃一二三筒"
输出: [{{"type": "CHI", "tile": "1p2p3p"}}]

输入: "碰白板，打三万"
输出: [{{"type": "PON", "tile": "5z"}}, {{"type": "DISCARD", "tile": "3m"}}]

输入: "打五万...不对，打八条"
输出: [{{"type": "DISCARD", "tile": "8s"}}]

输入: "你刚才是不是吃了"
输出: []

输入: "今天运气真好"
输出: []

要求：
- 只输出纯 JSON 数组，不要包含 Markdown 标记 (如 ```json)。
- 如果无法识别任何事件或文本无关，返回空数组 []。
- 重要：确保输出的是合法的 JSON 格式。
"""
        
        try:
            response = self.client.chat.completions.create(
                model=self.model,
                messages=[
                    {"role": "system", "content": "You are a helpful assistant that outputs raw JSON."},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.1
            )
            
            content = response.choices[0].message.content.strip()
            
            # Robust JSON extraction
            match = re.search(r"\[.*\]", content, re.DOTALL)
            if match:
                content = match.group(0)
            
            # Simple cleanup for markdown just in case (fallback)
            if content.startswith("```json"):
                content = content[7:]
            if content.startswith("```"):
                content = content[3:]
            if content.endswith("```"):
                content = content[:-3]
                
            return json.loads(content.strip())
            
        except Exception as e:
            logger.error(f"LLM Analysis Error: {e}")
            # Try to return partial result or empty
            return []
