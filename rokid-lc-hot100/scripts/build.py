"""
LC Hot100 构建脚本
==================
功能：将 hot100_data.json 数据内嵌到 index.html，生成 app_index.html
然后将 app_index.html 复制到 Android 项目 res/raw/ 目录，最后构建 APK

用法：
  python3 build.py                    # 使用默认路径
  python3 build.py --data 自定义.json # 指定数据文件

完整构建流程：
  1. python3 generate_hot100.py       # 生成 AI 讲解数据（需要 MiniMax API Key）
  2. python3 build.py                 # 嵌入数据 + 构建 APK
  3. adb install -r ../LC-Hot100.apk  # 安装到眼镜

开发者：bcefghj (bcefghj@163.com)
"""

import json
import sys
import os
import shutil
import subprocess
import argparse

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def build(data_file: str, skip_apk: bool = False):
    data_path = data_file if os.path.isabs(data_file) else os.path.join(BASE_DIR, "data", data_file)
    html_template = os.path.join(BASE_DIR, "android", "res", "raw", "index.html")
    res_raw_dir = os.path.join(BASE_DIR, "android", "res", "raw")
    output_html = os.path.join(res_raw_dir, "index.html")  # 直接覆盖

    # 1. 读取数据
    print(f"读取数据: {data_path}")
    with open(data_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    # 统计
    by_diff = {}
    for p in data:
        d = p.get("difficulty", "Unknown")
        by_diff[d] = by_diff.get(d, 0) + 1
    print(f"  共 {len(data)} 题: Easy={by_diff.get('Easy',0)}, Medium={by_diff.get('Medium',0)}, Hard={by_diff.get('Hard',0)}")

    # 2. 读取 HTML 模板（从 android/res/raw/index.html）
    with open(html_template, "r", encoding="utf-8") as f:
        html = f.read()

    if "/*PROBLEMS_DATA_PLACEHOLDER*/" not in html:
        print("⚠ index.html 中没有找到 /*PROBLEMS_DATA_PLACEHOLDER*/，请检查模板")
        sys.exit(1)

    # 3. 嵌入数据
    js_data = f"var PROBLEMS_DATA = {json.dumps(data, ensure_ascii=False)};"
    app_html = html.replace("/*PROBLEMS_DATA_PLACEHOLDER*/", js_data)

    # 4. 生成 app_index.html（供调试用）
    app_index_path = os.path.join(BASE_DIR, "android", "res", "raw", "app_index.html")
    with open(app_index_path, "w", encoding="utf-8") as f:
        f.write(app_html)
    print(f"✓ 生成调试文件: {app_index_path}")

    # 5. 构建 APK
    if not skip_apk:
        build_script = os.path.join(BASE_DIR, "..", "build_apk.sh")
        android_dir = os.path.join(BASE_DIR, "android")

        if not os.path.exists(build_script):
            print(f"⚠ 未找到 build_apk.sh: {build_script}")
            print("  请手动运行：bash ../build_apk.sh android/")
            return

        # 将 app_html 写入 android/res/raw/index.html 供构建使用
        with open(output_html, "w", encoding="utf-8") as f:
            f.write(app_html)
        print(f"✓ 数据已嵌入: {output_html}")

        print("\n正在构建 APK...")
        result = subprocess.run(
            ["bash", build_script, android_dir, "com.rokid.lchot100", "LC Hot100"],
            capture_output=True, text=True
        )
        if result.returncode == 0:
            # 查找生成的 APK
            apk_src = os.path.join(android_dir, "com-rokid-lchot100.apk")
            apk_dst = os.path.join(BASE_DIR, "LC-Hot100.apk")
            if os.path.exists(apk_src):
                shutil.copy(apk_src, apk_dst)
                print(f"✅ APK 构建成功: {apk_dst}")
                print(f"\n安装命令：adb install -r \"{apk_dst}\"")
            else:
                print(f"⚠ 构建完成但未找到 APK，请检查 build_apk.sh 输出")
                print(result.stdout[-2000:] if result.stdout else "")
        else:
            print("❌ APK 构建失败:")
            print(result.stderr[-2000:] if result.stderr else "")
            print(result.stdout[-2000:] if result.stdout else "")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="构建 LC Hot100 APK")
    parser.add_argument("--data", default="hot100_data.json", help="数据文件名（在 data/ 目录下）")
    parser.add_argument("--no-apk", action="store_true", help="只嵌入数据，不构建 APK")
    args = parser.parse_args()
    build(args.data, skip_apk=args.no_apk)
