"""Rokid 眼镜控制工具 - 通过 ADB 操作眼镜硬件"""

import asyncio
import logging
import os

logger = logging.getLogger(__name__)


async def _run_adb(cmd: str) -> str:
    device = os.getenv("ADB_DEVICE", "")
    adb_prefix = f"adb -s {device}" if device else "adb"
    full_cmd = f"{adb_prefix} {cmd}"

    proc = await asyncio.create_subprocess_shell(
        full_cmd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    stdout, stderr = await proc.communicate()
    if proc.returncode != 0:
        return f"ADB 命令失败: {stderr.decode()}"
    return stdout.decode().strip()


async def glasses_control(action: str, parameter: str = "") -> str:
    """控制 Rokid AI 眼镜硬件

    Args:
        action: 操作类型 - take_photo/screenshot/volume_up/volume_down/brightness/battery/device_info
        parameter: 额外参数
    """
    if action == "take_photo":
        result = await _run_adb("shell input keyevent 27")
        await asyncio.sleep(2)
        latest = await _run_adb(
            'shell "ls -t /sdcard/DCIM/Camera/*.jpg 2>/dev/null | head -1"'
        )
        if latest:
            local = f"/tmp/rokid_photo_{int(asyncio.get_event_loop().time())}.jpg"
            await _run_adb(f"pull {latest} {local}")
            return f"拍照成功，照片保存至: {local}"
        return "拍照已触发，但未找到照片文件"

    if action == "screenshot":
        local = f"/tmp/rokid_screen_{int(asyncio.get_event_loop().time())}.png"
        await _run_adb(f'exec-out screencap -p > "{local}"')
        return f"截图保存至: {local}"

    if action == "volume_up":
        await _run_adb("shell input keyevent 24")
        return "已增大音量"

    if action == "volume_down":
        await _run_adb("shell input keyevent 25")
        return "已减小音量"

    if action == "battery":
        result = await _run_adb("shell dumpsys battery")
        lines = result.split("\n")
        info = {}
        for line in lines:
            if "level" in line.lower():
                info["电量"] = line.split(":")[-1].strip() + "%"
            elif "status" in line.lower():
                status_map = {"2": "充电中", "3": "未充电", "5": "已充满"}
                val = line.split(":")[-1].strip()
                info["状态"] = status_map.get(val, val)
            elif "temperature" in line.lower():
                temp = int(line.split(":")[-1].strip()) / 10
                info["温度"] = f"{temp}°C"
        return " | ".join(f"{k}: {v}" for k, v in info.items()) or result

    if action == "device_info":
        model = await _run_adb("shell getprop ro.product.model")
        version = await _run_adb("shell getprop ro.build.version.release")
        sdk = await _run_adb("shell getprop ro.build.version.sdk")
        serial = await _run_adb("shell getprop ro.serialno")
        return (
            f"设备: {model} | Android {version} (API {sdk}) | "
            f"序列号: {serial}"
        )

    return f"未知操作: {action}"


TOOL_DEFINITION = {
    "name": "glasses_control",
    "description": "控制 Rokid AI 眼镜硬件，包括拍照、截图、音量调节、电量查询、设备信息等",
    "parameters": {
        "type": "object",
        "properties": {
            "action": {
                "type": "string",
                "description": "操作类型",
                "enum": [
                    "take_photo",
                    "screenshot",
                    "volume_up",
                    "volume_down",
                    "battery",
                    "device_info",
                ],
            },
            "parameter": {
                "type": "string",
                "description": "额外参数",
                "default": "",
            },
        },
        "required": ["action"],
    },
    "handler": glasses_control,
}
