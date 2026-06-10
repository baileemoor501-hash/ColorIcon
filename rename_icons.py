#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
前景图图标批量重命名脚本
将 mate图标分层 文件夹中的前景图重命名为内置前景图格式
"""

import os
import sys
from pathlib import Path


def get_new_name(old_name):
    """
    根据重命名规则获取新文件名

    规则:
    1. l_theme 前缀 -> wp_theme 前缀
    2. ic_tool_box 前缀 -> wp_tool_box 前缀
    3. icon_base.png -> wp_icon_base.png
    4. setting_1/2/3.png -> wp_setting_1/2/3.png
    5. 其他图标添加 wp_ 前缀
    """
    # 规则1: l_theme 前缀替换
    if old_name.startswith('l_theme'):
        return old_name.replace('l_theme', 'wp_theme', 1)

    # 规则2: ic_tool_box 前缀替换
    if old_name.startswith('ic_tool_box'):
        return old_name.replace('ic_tool_box', 'wp_tool_box', 1)

    # 规则3: icon_base.png
    if old_name == 'icon_base.png':
        return 'wp_icon_base.png'

    # 规则4: setting_1/2/3.png
    if old_name in ['setting_1.png', 'setting_2.png', 'setting_3.png']:
        return f'wp_{old_name}'

    # 规则5: 其他图标添加 wp_ 前缀
    if not old_name.startswith('wp_'):
        return f'wp_{old_name}'

    return old_name


def main():
    # 目标文件夹路径
    folder_path = Path(r'E:\Git\颜色工具\mate图标分层')

    # 检查文件夹是否存在
    if not folder_path.exists():
        print(f"[错误] 文件夹不存在: {folder_path}")
        return

    if not folder_path.is_dir():
        print(f"[错误] 路径不是文件夹: {folder_path}")
        return

    print(f"[目标文件夹] {folder_path}")
    print(f"{'=' * 80}\n")

    # 获取所有 PNG 文件
    png_files = list(folder_path.glob('*.png'))

    if not png_files:
        print("[警告] 文件夹中没有找到 PNG 文件")
        return

    # 生成重命名映射
    rename_map = []
    for png_file in sorted(png_files):
        old_name = png_file.name
        new_name = get_new_name(old_name)

        if old_name != new_name:
            rename_map.append((png_file, old_name, new_name))

    if not rename_map:
        print("[信息] 所有文件已经符合命名规则，无需重命名")
        return

    # 显示重命名预览
    print(f"[重命名预览] 将要重命名 {len(rename_map)} 个文件:\n")
    print(f"{'序号':<6} {'原文件名':<45} {'新文件名':<45}")
    print(f"{'-' * 6} {'-' * 45} {'-' * 45}")

    for idx, (file_path, old_name, new_name) in enumerate(rename_map, 1):
        print(f"{idx:<6} {old_name:<45} {new_name:<45}")

    # 确认执行
    print(f"\n{'=' * 80}")

    # 检查是否有命令行参数 -y 或 --yes 来自动确认
    auto_confirm = len(sys.argv) > 1 and sys.argv[1] in ['-y', '--yes']

    if not auto_confirm:
        confirm = input("\n是否执行重命名操作? (y/n): ").strip().lower()
        if confirm != 'y':
            print("[已取消] 操作已取消")
            return
    else:
        print("\n[自动确认] 自动执行重命名操作")

    # 执行重命名
    print(f"\n{'=' * 80}")
    print("[开始执行] 开始执行重命名操作...\n")

    success_count = 0
    error_count = 0

    for idx, (file_path, old_name, new_name) in enumerate(rename_map, 1):
        try:
            new_path = file_path.parent / new_name

            # 检查目标文件是否已存在
            if new_path.exists():
                print(f"[警告] [{idx}] 跳过: {old_name} (目标文件已存在: {new_name})")
                error_count += 1
                continue

            # 执行重命名
            file_path.rename(new_path)
            print(f"[成功] [{idx}] {old_name} -> {new_name}")
            success_count += 1

        except Exception as e:
            print(f"[失败] [{idx}] {old_name} - 错误: {e}")
            error_count += 1

    # 输出结果统计
    print(f"\n{'=' * 80}")
    print(f"[重命名完成统计]")
    print(f"   成功: {success_count} 个文件")
    print(f"   失败: {error_count} 个文件")
    print(f"   总计: {len(rename_map)} 个文件")
    print(f"{'=' * 80}\n")


if __name__ == '__main__':
    main()
