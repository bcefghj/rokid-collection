"""
曲谱查看器构建脚本：将 scores_data.json 嵌入到 index.html 中
用法: python3 build.py [scores_data.json]
"""
import json, sys

data_file = sys.argv[1] if len(sys.argv) > 1 else "scores_data.json"

with open(data_file, "r", encoding="utf-8") as f:
    data = json.load(f)

with open("index.html", "r", encoding="utf-8") as f:
    html = f.read()

js_data = f"var SCORES_DATA = {json.dumps(data, ensure_ascii=False)};"
html = html.replace("/*SCORES_DATA_PLACEHOLDER*/", js_data)

with open("index_built.html", "w", encoding="utf-8") as f:
    f.write(html)

print(f"已嵌入 {len(data)} 首曲谱到 index_built.html")
for artist in set(s["artist"] for s in data):
    count = sum(1 for s in data if s["artist"] == artist)
    print(f"  {artist}: {count} 首")
