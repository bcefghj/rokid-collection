"""
LeetCode Hot100 AI 讲解批量生成脚本
=====================================
功能：使用 MiniMax-M2 LLM 为 100 道经典题生成极简中文讲解，输出 hot100_data.json
用法：python3 generate_hot100.py [--output hot100_data.json]

生成格式：每题 4 页，用 [PAGE] 分隔
  第1页：题号 题名(难度) + 一句话概括
  第2页：核心思路
  第3页：关键代码（伪代码）
  第4页：复杂度 + 易错点

输出文件结构：
[
  {
    "id": "1",
    "title": "两数之和",
    "slug": "two-sum",
    "difficulty": "Easy",
    "tags": ["数组", "哈希表"],
    "pages": ["第1页内容", "第2页内容", "第3页内容", "第4页内容"]
  },
  ...
]

注意：
- 脚本支持断点续跑（已生成的题目自动跳过）
- 每 10 题自动保存一次，防止意外中断丢失
- 使用 MiniMax-M2 模型（需确认账户有余额）
- 生成完成后运行 build.py 将数据嵌入 HTML

开发者：bcefghj (bcefghj@163.com)
"""

import json
import time
import sys
import re
import argparse
from openai import OpenAI

# ===================== 配置 =====================
API_KEY = "YOUR_MINIMAX_API_KEY"  # 替换为你的 MiniMax API Key
MINIMAX_BASE_URL = "https://api.minimaxi.com/v1"
MODEL_NAME = "MiniMax-M2"
# ================================================

client = OpenAI(api_key=API_KEY, base_url=MINIMAX_BASE_URL)

# LeetCode Hot100 题目列表
# 按以下分类整理：数组/哈希 → 双指针/滑动窗口 → 链表 → 树 → 图/回溯 → 二分 → 栈/堆 → 贪心 → 动态规划 → 位运算
HOT100 = [
    # --- 数组 & 哈希 ---
    {"id": "1", "title": "两数之和", "slug": "two-sum", "difficulty": "Easy", "tags": ["数组", "哈希表"]},
    {"id": "49", "title": "字母异位词分组", "slug": "group-anagrams", "difficulty": "Medium", "tags": ["哈希表", "字符串", "排序"]},
    {"id": "128", "title": "最长连续序列", "slug": "longest-consecutive-sequence", "difficulty": "Medium", "tags": ["数组", "哈希表"]},
    {"id": "283", "title": "移动零", "slug": "move-zeroes", "difficulty": "Easy", "tags": ["数组", "双指针"]},
    # --- 双指针 & 滑动窗口 ---
    {"id": "11", "title": "盛最多水的容器", "slug": "container-with-most-water", "difficulty": "Medium", "tags": ["数组", "双指针", "贪心"]},
    {"id": "15", "title": "三数之和", "slug": "3sum", "difficulty": "Medium", "tags": ["数组", "双指针", "排序"]},
    {"id": "42", "title": "接雨水", "slug": "trapping-rain-water", "difficulty": "Hard", "tags": ["栈", "数组", "双指针", "动态规划"]},
    {"id": "3", "title": "无重复字符的最长子串", "slug": "longest-substring-without-repeating-characters", "difficulty": "Medium", "tags": ["哈希表", "字符串", "滑动窗口"]},
    {"id": "438", "title": "找到字符串中所有字母异位词", "slug": "find-all-anagrams-in-a-string", "difficulty": "Medium", "tags": ["哈希表", "字符串", "滑动窗口"]},
    {"id": "560", "title": "和为K的子数组", "slug": "subarray-sum-equals-k", "difficulty": "Medium", "tags": ["数组", "哈希表", "前缀和"]},
    {"id": "239", "title": "滑动窗口最大值", "slug": "sliding-window-maximum", "difficulty": "Hard", "tags": ["数组", "队列", "滑动窗口", "单调队列"]},
    {"id": "76", "title": "最小覆盖子串", "slug": "minimum-window-substring", "difficulty": "Hard", "tags": ["哈希表", "字符串", "滑动窗口"]},
    # --- 子数组 & 矩阵 ---
    {"id": "53", "title": "最大子数组和", "slug": "maximum-subarray", "difficulty": "Medium", "tags": ["数组", "分治", "动态规划"]},
    {"id": "56", "title": "合并区间", "slug": "merge-intervals", "difficulty": "Medium", "tags": ["数组", "排序"]},
    {"id": "189", "title": "轮转数组", "slug": "rotate-array", "difficulty": "Medium", "tags": ["数组", "数学"]},
    {"id": "238", "title": "除自身以外数组的乘积", "slug": "product-of-array-except-self", "difficulty": "Medium", "tags": ["数组", "前缀和"]},
    {"id": "41", "title": "缺失的第一个正数", "slug": "first-missing-positive", "difficulty": "Hard", "tags": ["数组", "哈希表"]},
    {"id": "73", "title": "矩阵置零", "slug": "set-matrix-zeroes", "difficulty": "Medium", "tags": ["数组", "矩阵"]},
    {"id": "54", "title": "螺旋矩阵", "slug": "spiral-matrix", "difficulty": "Medium", "tags": ["数组", "矩阵", "模拟"]},
    {"id": "48", "title": "旋转图像", "slug": "rotate-image", "difficulty": "Medium", "tags": ["数组", "矩阵"]},
    {"id": "240", "title": "搜索二维矩阵 II", "slug": "search-a-2d-matrix-ii", "difficulty": "Medium", "tags": ["数组", "二分", "矩阵"]},
    # --- 链表 ---
    {"id": "160", "title": "相交链表", "slug": "intersection-of-two-linked-lists", "difficulty": "Easy", "tags": ["链表", "双指针"]},
    {"id": "206", "title": "反转链表", "slug": "reverse-linked-list", "difficulty": "Easy", "tags": ["链表", "递归"]},
    {"id": "234", "title": "回文链表", "slug": "palindrome-linked-list", "difficulty": "Easy", "tags": ["链表", "双指针", "栈"]},
    {"id": "141", "title": "环形链表", "slug": "linked-list-cycle", "difficulty": "Easy", "tags": ["链表", "双指针"]},
    {"id": "142", "title": "环形链表 II", "slug": "linked-list-cycle-ii", "difficulty": "Medium", "tags": ["链表", "双指针"]},
    {"id": "21", "title": "合并两个有序链表", "slug": "merge-two-sorted-lists", "difficulty": "Easy", "tags": ["链表", "递归"]},
    {"id": "2", "title": "两数相加", "slug": "add-two-numbers", "difficulty": "Medium", "tags": ["链表", "数学"]},
    {"id": "19", "title": "删除链表的倒数第N个结点", "slug": "remove-nth-node-from-end-of-list", "difficulty": "Medium", "tags": ["链表", "双指针"]},
    {"id": "24", "title": "两两交换链表中的节点", "slug": "swap-nodes-in-pairs", "difficulty": "Medium", "tags": ["链表", "递归"]},
    {"id": "25", "title": "K个一组翻转链表", "slug": "reverse-nodes-in-k-group", "difficulty": "Hard", "tags": ["链表", "递归"]},
    {"id": "138", "title": "随机链表的复制", "slug": "copy-list-with-random-pointer", "difficulty": "Medium", "tags": ["链表", "哈希表"]},
    {"id": "148", "title": "排序链表", "slug": "sort-list", "difficulty": "Medium", "tags": ["链表", "排序", "归并"]},
    {"id": "23", "title": "合并K个升序链表", "slug": "merge-k-sorted-lists", "difficulty": "Hard", "tags": ["链表", "堆"]},
    {"id": "146", "title": "LRU 缓存", "slug": "lru-cache", "difficulty": "Medium", "tags": ["哈希表", "链表", "设计"]},
    # --- 二叉树 ---
    {"id": "94", "title": "二叉树的中序遍历", "slug": "binary-tree-inorder-traversal", "difficulty": "Easy", "tags": ["树", "栈", "递归"]},
    {"id": "104", "title": "二叉树的最大深度", "slug": "maximum-depth-of-binary-tree", "difficulty": "Easy", "tags": ["树", "DFS", "BFS"]},
    {"id": "226", "title": "翻转二叉树", "slug": "invert-binary-tree", "difficulty": "Easy", "tags": ["树", "DFS", "BFS"]},
    {"id": "101", "title": "对称二叉树", "slug": "symmetric-tree", "difficulty": "Easy", "tags": ["树", "DFS", "BFS"]},
    {"id": "543", "title": "二叉树的直径", "slug": "diameter-of-binary-tree", "difficulty": "Easy", "tags": ["树", "DFS"]},
    {"id": "102", "title": "二叉树的层序遍历", "slug": "binary-tree-level-order-traversal", "difficulty": "Medium", "tags": ["树", "BFS"]},
    {"id": "108", "title": "将有序数组转换为二叉搜索树", "slug": "convert-sorted-array-to-binary-search-tree", "difficulty": "Easy", "tags": ["树", "分治"]},
    {"id": "98", "title": "验证二叉搜索树", "slug": "validate-binary-search-tree", "difficulty": "Medium", "tags": ["树", "DFS"]},
    {"id": "230", "title": "二叉搜索树中第K小的元素", "slug": "kth-smallest-element-in-a-bst", "difficulty": "Medium", "tags": ["树", "DFS"]},
    {"id": "199", "title": "二叉树的右视图", "slug": "binary-tree-right-side-view", "difficulty": "Medium", "tags": ["树", "BFS"]},
    {"id": "114", "title": "二叉树展开为链表", "slug": "flatten-binary-tree-to-linked-list", "difficulty": "Medium", "tags": ["树", "DFS", "链表"]},
    {"id": "105", "title": "从前序与中序遍历序列构造二叉树", "slug": "construct-binary-tree-from-preorder-and-inorder-traversal", "difficulty": "Medium", "tags": ["树", "分治"]},
    {"id": "437", "title": "路径总和 III", "slug": "path-sum-iii", "difficulty": "Medium", "tags": ["树", "DFS", "前缀和"]},
    {"id": "236", "title": "二叉树的最近公共祖先", "slug": "lowest-common-ancestor-of-a-binary-tree", "difficulty": "Medium", "tags": ["树", "DFS"]},
    {"id": "124", "title": "二叉树中的最大路径和", "slug": "binary-tree-maximum-path-sum", "difficulty": "Hard", "tags": ["树", "DFS", "动态规划"]},
    # --- 图 & 回溯 ---
    {"id": "200", "title": "岛屿数量", "slug": "number-of-islands", "difficulty": "Medium", "tags": ["图", "DFS", "BFS"]},
    {"id": "994", "title": "腐烂的橘子", "slug": "rotting-oranges", "difficulty": "Medium", "tags": ["图", "BFS"]},
    {"id": "207", "title": "课程表", "slug": "course-schedule", "difficulty": "Medium", "tags": ["图", "拓扑排序"]},
    {"id": "208", "title": "实现 Trie (前缀树)", "slug": "implement-trie-prefix-tree", "difficulty": "Medium", "tags": ["字符串", "设计", "Trie"]},
    {"id": "46", "title": "全排列", "slug": "permutations", "difficulty": "Medium", "tags": ["数组", "回溯"]},
    {"id": "78", "title": "子集", "slug": "subsets", "difficulty": "Medium", "tags": ["数组", "回溯"]},
    {"id": "17", "title": "电话号码的字母组合", "slug": "letter-combinations-of-a-phone-number", "difficulty": "Medium", "tags": ["字符串", "回溯"]},
    {"id": "39", "title": "组合总和", "slug": "combination-sum", "difficulty": "Medium", "tags": ["数组", "回溯"]},
    {"id": "22", "title": "括号生成", "slug": "generate-parentheses", "difficulty": "Medium", "tags": ["字符串", "回溯"]},
    {"id": "79", "title": "单词搜索", "slug": "word-search", "difficulty": "Medium", "tags": ["数组", "回溯", "矩阵"]},
    {"id": "131", "title": "分割回文串", "slug": "palindrome-partitioning", "difficulty": "Medium", "tags": ["字符串", "回溯", "动态规划"]},
    {"id": "51", "title": "N 皇后", "slug": "n-queens", "difficulty": "Hard", "tags": ["数组", "回溯"]},
    # --- 二分查找 ---
    {"id": "35", "title": "搜索插入位置", "slug": "search-insert-position", "difficulty": "Easy", "tags": ["数组", "二分查找"]},
    {"id": "74", "title": "搜索二维矩阵", "slug": "search-a-2d-matrix", "difficulty": "Medium", "tags": ["数组", "二分查找"]},
    {"id": "34", "title": "在排序数组中查找元素的第一个和最后一个位置", "slug": "find-first-and-last-position-of-element-in-sorted-array", "difficulty": "Medium", "tags": ["数组", "二分查找"]},
    {"id": "33", "title": "搜索旋转排序数组", "slug": "search-in-rotated-sorted-array", "difficulty": "Medium", "tags": ["数组", "二分查找"]},
    {"id": "153", "title": "寻找旋转排序数组中的最小值", "slug": "find-minimum-in-rotated-sorted-array", "difficulty": "Medium", "tags": ["数组", "二分查找"]},
    {"id": "4", "title": "寻找两个正序数组的中位数", "slug": "median-of-two-sorted-arrays", "difficulty": "Hard", "tags": ["数组", "二分查找"]},
    # --- 栈 & 单调栈 & 堆 ---
    {"id": "20", "title": "有效的括号", "slug": "valid-parentheses", "difficulty": "Easy", "tags": ["栈", "字符串"]},
    {"id": "155", "title": "最小栈", "slug": "min-stack", "difficulty": "Medium", "tags": ["栈", "设计"]},
    {"id": "394", "title": "字符串解码", "slug": "decode-string", "difficulty": "Medium", "tags": ["栈", "字符串", "递归"]},
    {"id": "739", "title": "每日温度", "slug": "daily-temperatures", "difficulty": "Medium", "tags": ["栈", "数组", "单调栈"]},
    {"id": "84", "title": "柱状图中最大的矩形", "slug": "largest-rectangle-in-histogram", "difficulty": "Hard", "tags": ["栈", "数组", "单调栈"]},
    {"id": "215", "title": "数组中的第K个最大元素", "slug": "kth-largest-element-in-an-array", "difficulty": "Medium", "tags": ["数组", "堆", "快选"]},
    {"id": "347", "title": "前 K 个高频元素", "slug": "top-k-frequent-elements", "difficulty": "Medium", "tags": ["数组", "哈希表", "堆"]},
    {"id": "295", "title": "数据流的中位数", "slug": "find-median-from-data-stream", "difficulty": "Hard", "tags": ["设计", "堆"]},
    # --- 贪心 ---
    {"id": "121", "title": "买卖股票的最佳时机", "slug": "best-time-to-buy-and-sell-stock", "difficulty": "Easy", "tags": ["数组", "动态规划"]},
    {"id": "55", "title": "跳跃游戏", "slug": "jump-game", "difficulty": "Medium", "tags": ["数组", "贪心", "动态规划"]},
    {"id": "45", "title": "跳跃游戏 II", "slug": "jump-game-ii", "difficulty": "Medium", "tags": ["数组", "贪心"]},
    {"id": "763", "title": "划分字母区间", "slug": "partition-labels", "difficulty": "Medium", "tags": ["字符串", "贪心"]},
    # --- 动态规划 ---
    {"id": "70", "title": "爬楼梯", "slug": "climbing-stairs", "difficulty": "Easy", "tags": ["动态规划"]},
    {"id": "118", "title": "杨辉三角", "slug": "pascals-triangle", "difficulty": "Easy", "tags": ["数组", "动态规划"]},
    {"id": "198", "title": "打家劫舍", "slug": "house-robber", "difficulty": "Medium", "tags": ["数组", "动态规划"]},
    {"id": "279", "title": "完全平方数", "slug": "perfect-squares", "difficulty": "Medium", "tags": ["数学", "动态规划", "BFS"]},
    {"id": "322", "title": "零钱兑换", "slug": "coin-change", "difficulty": "Medium", "tags": ["数组", "动态规划"]},
    {"id": "139", "title": "单词拆分", "slug": "word-break", "difficulty": "Medium", "tags": ["字符串", "动态规划"]},
    {"id": "300", "title": "最长递增子序列", "slug": "longest-increasing-subsequence", "difficulty": "Medium", "tags": ["数组", "动态规划", "二分"]},
    {"id": "152", "title": "乘积最大子数组", "slug": "maximum-product-subarray", "difficulty": "Medium", "tags": ["数组", "动态规划"]},
    {"id": "416", "title": "分割等和子集", "slug": "partition-equal-subset-sum", "difficulty": "Medium", "tags": ["数组", "动态规划"]},
    {"id": "32", "title": "最长有效括号", "slug": "longest-valid-parentheses", "difficulty": "Hard", "tags": ["字符串", "动态规划", "栈"]},
    {"id": "62", "title": "不同路径", "slug": "unique-paths", "difficulty": "Medium", "tags": ["数学", "动态规划"]},
    {"id": "64", "title": "最小路径和", "slug": "minimum-path-sum", "difficulty": "Medium", "tags": ["数组", "动态规划", "矩阵"]},
    {"id": "5", "title": "最长回文子串", "slug": "longest-palindromic-substring", "difficulty": "Medium", "tags": ["字符串", "动态规划"]},
    {"id": "1143", "title": "最长公共子序列", "slug": "longest-common-subsequence", "difficulty": "Medium", "tags": ["字符串", "动态规划"]},
    {"id": "72", "title": "编辑距离", "slug": "edit-distance", "difficulty": "Hard", "tags": ["字符串", "动态规划"]},
    # --- 位运算 & 其他 ---
    {"id": "136", "title": "只出现一次的数字", "slug": "single-number", "difficulty": "Easy", "tags": ["数组", "位运算"]},
    {"id": "169", "title": "多数元素", "slug": "majority-element", "difficulty": "Easy", "tags": ["数组", "排序", "摩尔投票"]},
    {"id": "75", "title": "颜色分类", "slug": "sort-colors", "difficulty": "Medium", "tags": ["数组", "双指针", "排序"]},
    {"id": "31", "title": "下一个排列", "slug": "next-permutation", "difficulty": "Medium", "tags": ["数组", "双指针"]},
    {"id": "287", "title": "寻找重复数", "slug": "find-the-duplicate-number", "difficulty": "Medium", "tags": ["数组", "双指针", "位运算"]},
]

# Prompt 设计：每题严格 4 页，每页 ≤45 汉字，适合 AR 眼镜小屏显示
SYSTEM_PROMPT = """你是算法讲师。请用极简中文讲解这道LeetCode题，严格按以下格式输出，用[PAGE]分隔每页：

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


def generate_explanation(problem: dict) -> list[str]:
    """调用 MiniMax API 生成单题讲解，返回分页列表"""
    prompt = f"#{problem['id']} {problem['title']}（{problem['difficulty']}）标签：{', '.join(problem['tags'])}"
    try:
        resp = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": prompt},
            ],
            max_tokens=600,
            temperature=0.5,
        )
        text = resp.choices[0].message.content
        # 去除 MiniMax-M2 的思考过程标签
        text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL).strip()
        pages = [p.strip() for p in text.split("[PAGE]") if p.strip()]
        return pages
    except Exception as e:
        print(f"  错误: {e}", file=sys.stderr)
        return [
            f"#{problem['id']} {problem['title']}",
            f"难度: {problem['difficulty']}",
            f"标签: {', '.join(problem['tags'])}",
            "讲解生成失败，请重试"
        ]


def main():
    parser = argparse.ArgumentParser(description="生成 LeetCode Hot100 AI 讲解数据")
    parser.add_argument("--output", default="../data/hot100_data.json", help="输出文件路径")
    args = parser.parse_args()

    output_file = args.output

    # 加载已有数据（支持断点续跑）
    existing = {}
    try:
        with open(output_file, "r", encoding="utf-8") as f:
            data = json.load(f)
            for p in data:
                existing[p["id"]] = p
        print(f"✓ 已有 {len(existing)} 题数据，继续生成剩余题目")
    except FileNotFoundError:
        print("从头开始生成...")
    except Exception as e:
        print(f"读取已有数据失败: {e}，从头开始")

    results = []
    for i, prob in enumerate(HOT100):
        if prob["id"] in existing:
            results.append(existing[prob["id"]])
            print(f"[{i+1:3d}/100] #{prob['id']:4s} {prob['title']} — 已有，跳过")
            continue

        print(f"[{i+1:3d}/100] #{prob['id']:4s} {prob['title']} ...", end=" ", flush=True)
        pages = generate_explanation(prob)
        print(f"✓ {len(pages)} 页")

        results.append({
            "id": prob["id"],
            "title": prob["title"],
            "slug": prob["slug"],
            "difficulty": prob["difficulty"],
            "tags": prob["tags"],
            "pages": pages
        })

        # 每 10 题保存一次
        if (i + 1) % 10 == 0:
            with open(output_file, "w", encoding="utf-8") as f:
                json.dump(results, f, ensure_ascii=False, indent=2)
            print(f"  ---- 已保存 {len(results)} 题 ----")

        time.sleep(1)  # 避免触发速率限制

    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    # 统计
    by_diff = {}
    for p in results:
        d = p["difficulty"]
        by_diff[d] = by_diff.get(d, 0) + 1

    print(f"\n✅ 完成！共 {len(results)} 题，已保存到 {output_file}")
    print(f"   Easy: {by_diff.get('Easy', 0)} 题")
    print(f"   Medium: {by_diff.get('Medium', 0)} 题")
    print(f"   Hard: {by_diff.get('Hard', 0)} 题")
    print("\n下一步：运行 python3 build.py 将数据嵌入 HTML")


if __name__ == "__main__":
    main()
