import time
import json
from openai import OpenAI

# Configuration
API_URL = "http://127.0.0.1:1234/v1"
API_KEY = "qwen/qwen3-4b-2507"
MODEL_NAME = "qwen/qwen3-4b-2507"

# Simulated Transcript
# Translation: "Opponent discarded a 5 Wan, I Pon here, then discard a Red Dragon, Riichi!"
TRANSCRIPT = "下家打了一张五万，我这边碰了，然后切一张红中，立直！"

def run_test():
    print(f"Connecting to LLM at {API_URL}...")
    print(f"Model: {MODEL_NAME}")
    print(f"Simulated Transcript: \"{TRANSCRIPT}\"")
    print("-" * 50)

    client = OpenAI(base_url=API_URL, api_key=API_KEY)

    prompt = f"""
你是一名专业的麻将裁判。请分析以下语音转录文本，提取其中的牌局事件。

文本内容: "{TRANSCRIPT}"

请提取以下类型的事件：
1. 切牌 (DISCARD): 例如 "切 5索", "打发财", "5万" (如果是切牌语境)
2. 吃 (CHI)
3. 碰 (PON)
4. 杠 (KAN)
5. 立直 (RIICHI)

请返回 JSON 格式的列表，每个元素包含 "type" 和 "tile" (使用 mpsz 格式)。
- m = 万子 (1m-9m)
- p = 筒子 (1p-9p)
- s = 索子/条子 (1s-9s)
- z = 字牌 (1z=东, 2z=南, 3z=西, 4z=北, 5z=白, 6z=发, 7z=中)

示例输出:
[
    {{"type": "DISCARD", "tile": "5s"}},
    {{"type": "PON", "tile": "6z"}}
]

要求：
- 只输出纯 JSON 数组，不要包含 Markdown 标记 (如 ```json)。
- 如果无法识别任何事件或文本无关，返回空数组 []。
"""

    start_time = time.time()
    try:
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": "You are a helpful assistant that outputs raw JSON."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.1
        )
        end_time = time.time()
        elapsed = end_time - start_time
        
        content = response.choices[0].message.content.strip()
        
        print(f"Request completed in {elapsed:.4f} seconds.")
        print("-" * 50)
        print("Raw Response Content:")
        print(content)
        print("-" * 50)
        
        # Validate JSON
        try:
            # Clean up markdown if present
            clean_content = content
            if clean_content.startswith("```json"):
                clean_content = clean_content[7:]
            if clean_content.startswith("```"):
                clean_content = clean_content[3:]
            if clean_content.endswith("```"):
                clean_content = clean_content[:-3]
                
            parsed = json.loads(clean_content.strip())
            print("Parsed JSON Successfully:")
            print(json.dumps(parsed, indent=2, ensure_ascii=False))
            
            # Simple validation logic
            expected_types = {"PON", "DISCARD", "RIICHI"}
            found_types = {item.get("type") for item in parsed}
            print(f"Found Event Types: {found_types}")
            
            if expected_types.issubset(found_types):
                print("✅ Test PASSED: All expected events found.")
            else:
                print("⚠️ Test PARTIAL: Some expected events missing.")
                
        except json.JSONDecodeError as e:
            print(f"❌ JSON Parse Error: {e}")
            
    except Exception as e:
        print(f"❌ Connection/API Error: {e}")

if __name__ == "__main__":
    run_test()
