#!/usr/bin/env python3
"""用 farfarfun 品牌的 game 系列 mark(透明底 1024)生成各端应用图标。

用法: python3 scripts/gen-icons.py <path/to/game-1024.png>
来源: https://github.com/farfarfun/.github  brand/png/mark/game-1024.png
依赖: Pillow。输出会覆盖已有图标文件。
"""
import json
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
WHITE = (255, 255, 255, 255)


def on_white(mark: Image.Image, size: int) -> Image.Image:
    base = Image.new("RGBA", mark.size, WHITE)
    base.alpha_composite(mark)
    return base.resize((size, size), Image.LANCZOS)


def save(img: Image.Image, path: Path, opaque: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB" if opaque else "RGBA").save(path, optimize=True)


def main(src: str) -> None:
    mark = Image.open(src).convert("RGBA")
    assert mark.size == (1024, 1024), mark.size

    # 通用:README 展示与源图
    save(mark, ROOT / "assets/mark-1024.png")
    save(mark.resize((256, 256), Image.LANCZOS), ROOT / "assets/mark-256.png")

    # 桌面:electron-builder 默认读取 build/icon.png(mac 自动转 icns,linux 直接用)
    save(on_white(mark, 1024), ROOT / "desktop/build/icon.png", opaque=True)

    # iOS:单一 1024,必须不透明
    ios = ROOT / "ios/App/Assets.xcassets"
    save(on_white(mark, 1024), ios / "AppIcon.appiconset/icon-1024.png", opaque=True)
    (ios / "Contents.json").write_text(json.dumps({"info": {"author": "xcode", "version": 1}}, indent=2) + "\n")
    (ios / "AppIcon.appiconset/Contents.json").write_text(json.dumps({
        "images": [{"filename": "icon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"}],
        "info": {"author": "xcode", "version": 1},
    }, indent=2) + "\n")

    # Android:旧版 mipmap(<26)+ 自适应图标前景(>=26),背景为白色
    res = ROOT / "android/app/src/main/res"
    for name, px in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        legacy = on_white(mark, px)
        save(legacy, res / f"mipmap-{name}/ic_launcher.png")
        save(legacy, res / f"mipmap-{name}/ic_launcher_round.png")
    for name, px in {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}.items():
        # mark 本身四周已留白,整张缩放后字形落在 66dp 安全区内
        save(mark.resize((px, px), Image.LANCZOS), res / f"drawable-{name}/ic_launcher_foreground.png")
    (res / "values/ic_launcher_background.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
        '    <color name="ic_launcher_background">#FFFFFF</color>\n</resources>\n')
    adaptive = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background" />\n'
        '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
        '</adaptive-icon>\n')
    for n in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (res / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
        (res / "mipmap-anydpi-v26" / n).write_text(adaptive)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
