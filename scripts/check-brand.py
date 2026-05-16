from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {".git", "node_modules", "target", "dist", ".idea", ".vscode"}

# RuoYi-Vue 是本项目脚手架来源（MIT），允许在署名/许可证/文档/商业版条款里出现。
# 但不能在源代码、配置、SQL、测试等正常工程文件里出现 —— 那意味着没改干净。
RUOYI_TERMS = [
    "ruoyi",
    "RuoYi",
    "RUOYI",
    "若依",
    "com.ruoyi",
    "y_project",
]
RUOYI_ALLOWED_FILES = {
    "README.md",
    "LICENSE",
    "LICENSE-COMMERCIAL.md",
    "NOTICE.md",
}
RUOYI_ALLOWED_DIRS = (
    "licenses/",
    "docs/",
    "tools/",
    "scripts/",  # 这个脚本自身被跳过；其他脚本若需引用 RuoYi 也允许
)

# 这些是历史品牌残留，无论在哪儿都不能出现。
HARD_FORBIDDEN = [
    "scaffold.vip",
    "doc.scaffold",
    "qm.qq",
    "QQ群",
]


def should_skip(rel_path: Path) -> bool:
    if rel_path.as_posix() == "scripts/check-brand.py":
        return True
    return any(part in SKIP_DIRS for part in rel_path.parts)


def is_binary(data: bytes) -> bool:
    return b"\0" in data[:2048]


def is_ruoyi_attribution_file(rel_posix: str) -> bool:
    if rel_posix in RUOYI_ALLOWED_FILES:
        return True
    return any(rel_posix.startswith(prefix) for prefix in RUOYI_ALLOWED_DIRS)


def main() -> int:
    hits: list[str] = []
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        rel = path.relative_to(ROOT)
        if should_skip(rel):
            continue
        data = path.read_bytes()
        if is_binary(data):
            continue
        text = data.decode("utf-8", errors="ignore")
        rel_posix = rel.as_posix()

        for word in HARD_FORBIDDEN:
            if word in text:
                hits.append(f"{rel_posix}: contains {word!r} (hard-forbidden)")

        if not is_ruoyi_attribution_file(rel_posix):
            for word in RUOYI_TERMS:
                if word in text:
                    hits.append(
                        f"{rel_posix}: contains {word!r} (RuoYi mentions only allowed in "
                        f"README.md / LICENSE / LICENSE-COMMERCIAL.md / NOTICE.md / "
                        f"licenses/ / docs/ / tools/ / scripts/)"
                    )

    if hits:
        print("Forbidden branding found:")
        print(os.linesep.join(hits))
        return 1
    print("No forbidden branding found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
