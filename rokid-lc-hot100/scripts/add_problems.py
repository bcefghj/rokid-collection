"""
自定义题目添加工具
==================
功能：交互式地向 hot100_data.json 添加、导入或 AI 生成自定义题目讲解
支持三种模式：
  1. 手动添加：手动输入题目 ID、标题、难度等信息
  2. 批量导入：从 JSON 文件导入（格式见下方说明）
  3. AI 生成：输入题目基本信息，调用 MiniMax API 生成讲解

用法：python3 add_problems.py

批量导入 JSON 格式：
[
  {
    "id": "101",
    "title": "题目名称",
    "difficulty": "Easy|Medium|Hard",
    "tags": ["标签1", "标签2"],
    "pages": ["第1页内容", "第2页内容", ...]  // 可选，不填则由 AI 生成
  }
]

开发者：bcefghj (bcefghj@163.com)
"""

import json
import os
import sys
import re

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_FILE = os.path.join(BASE_DIR, "data", "hot100_data.json")


def load_data() -> list:
    try:
        with open(DATA_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"数据文件不存在，将创建新文件: {DATA_FILE}")
        return []


def save_data(data: list):
    with open(DATA_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"✓ 已保存 {len(data)} 题到 {DATA_FILE}")


def ai_generate(problem: dict, api_key: str) -> list[str]:
    """调用 MiniMax API 生成讲解"""
    try:
        from openai import OpenAI
        client = OpenAI(api_key=api_key, base_url="https://api.minimaxi.com/v1")
        SYSTEM = """你是算法讲师。请用极简中文讲解这道LeetCode题，严格按以下格式输出，用[PAGE]分隔每页：

[PAGE]题号 题名(难度)
一句话概括题意
[PAGE]核心思路：XX
2-3句关键思路
[PAGE]关键代码(伪代码)
3-5行核心逻辑
[PAGE]复杂度
时间O(?) 空间O(?)
易错点提醒

每页不超过45个汉字。不要输出多余内容。"""
        prompt = f"#{problem['id']} {problem['title']}（{problem['difficulty']}）标签：{', '.join(problem.get('tags', []))}"
        resp = client.chat.completions.create(
            model="MiniMax-M2",
            messages=[
                {"role": "system", "content": SYSTEM},
                {"role": "user", "content": prompt},
            ],
            max_tokens=600, temperature=0.5,
        )
        text = resp.choices[0].message.content
        text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
        pages = [p.strip() for p in text.split("[PAGE]") if p.strip()]
        return pages
    except Exception as e:
        print(f"AI 生成失败: {e}")
        return [f"#{problem['id']} {problem['title']}", f"难度: {problem['difficulty']}", "讲解生成失败"]


def manual_add(data: list):
    """手动添加单题"""
    print("\n--- 手动添加题目 ---")
    pid = input("题目 ID（如 101）: ").strip()
    if any(p["id"] == pid for p in data):
        print(f"⚠ ID={pid} 已存在")
        return

    title = input("题目名称: ").strip()
    difficulty = input("难度 (Easy/Medium/Hard): ").strip()
    tags_str = input("标签（逗号分隔，如 数组,哈希表）: ").strip()
    tags = [t.strip() for t in tags_str.split(",") if t.strip()]

    print("讲解内容（直接输入内容后用 AI 生成，或自己输入分页）")
    use_ai = input("使用 AI 生成讲解? (y/n): ").strip().lower() == "y"

    if use_ai:
        api_key = input("输入 MiniMax API Key: ").strip()
        pages = ai_generate({"id": pid, "title": title, "difficulty": difficulty, "tags": tags}, api_key)
        print(f"✓ AI 生成了 {len(pages)} 页讲解")
    else:
        print("请逐页输入内容（每页输入完后按回车，全部输完后输入 END）:")
        pages = []
        while True:
            page = input(f"第{len(pages)+1}页内容（或 END 结束）: ").strip()
            if page.upper() == "END":
                break
            if page:
                pages.append(page)

    data.append({
        "id": pid, "title": title,
        "slug": title.lower().replace(" ", "-"),
        "difficulty": difficulty, "tags": tags, "pages": pages
    })
    save_data(data)


def batch_import(data: list):
    """批量从 JSON 导入"""
    print("\n--- 批量导入题目 ---")
    filepath = input("输入 JSON 文件路径: ").strip()
    if not os.path.exists(filepath):
        print(f"文件不存在: {filepath}")
        return

    with open(filepath, "r", encoding="utf-8") as f:
        new_problems = json.load(f)

    existing_ids = {p["id"] for p in data}
    need_ai = []
    added = 0

    for prob in new_problems:
        if prob["id"] in existing_ids:
            print(f"  跳过（已存在）: #{prob['id']} {prob.get('title', '')}")
            continue
        if not prob.get("pages"):
            need_ai.append(prob)
        else:
            data.append(prob)
            added += 1
            print(f"  ✓ 导入: #{prob['id']} {prob.get('title', '')}")

    if need_ai:
        print(f"\n{len(need_ai)} 道题没有讲解内容，是否使用 AI 生成?")
        use_ai = input("(y/n): ").strip().lower() == "y"
        if use_ai:
            api_key = input("输入 MiniMax API Key: ").strip()
            import time
            for prob in need_ai:
                print(f"  生成: #{prob['id']} {prob.get('title', '')} ...", end=" ", flush=True)
                prob["pages"] = ai_generate(prob, api_key)
                print(f"✓ {len(prob['pages'])} 页")
                data.append(prob)
                added += 1
                time.sleep(1)

    save_data(data)
    print(f"✅ 共导入 {added} 道新题")


def show_stats(data: list):
    """显示统计信息"""
    by_diff = {}
    for p in data:
        d = p.get("difficulty", "Unknown")
        by_diff[d] = by_diff.get(d, 0) + 1
    print(f"\n当前题库: 共 {len(data)} 题")
    for d, c in sorted(by_diff.items()):
        print(f"  {d}: {c} 题")


def main():
    print("=" * 50)
    print("  LC Hot100 自定义题目添加工具")
    print("  开发者: bcefghj (bcefghj@163.com)")
    print("=" * 50)

    data = load_data()
    show_stats(data)

    while True:
        print("\n选择操作：")
        print("  1. 手动添加单道题目")
        print("  2. 批量从 JSON 文件导入")
        print("  3. 查看当前题库统计")
        print("  4. 构建 APK")
        print("  0. 退出")

        choice = input("请选择 (0-4): ").strip()

        if choice == "1":
            manual_add(data)
        elif choice == "2":
            batch_import(data)
        elif choice == "3":
            show_stats(data)
        elif choice == "4":
            print("\n正在构建 APK...")
            build_script = os.path.join(BASE_DIR, "scripts", "build.py")
            os.system(f"python3 '{build_script}'")
        elif choice == "0":
            print("退出")
            break
        else:
            print("无效选择")


if __name__ == "__main__":
    main()
