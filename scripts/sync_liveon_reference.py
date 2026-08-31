#!/usr/bin/env python3
"""Vendor the exact public Live On client source into this work branch.

The source is pinned to the Live On master commit that was reviewed for this
NightLegion port.  This script intentionally copies the Java/resource files
byte-for-byte into src/main/java/com/liveon and src/main/resources so we can
adapt NightLegion around proven code rather than reimplementing it.
"""
from __future__ import annotations

import json
import pathlib
import re
import urllib.request

OWNER = "MilicoOSRS"
REPO = "live-on-clan"
REF = "8f69ae9b1906f75fe2896d780fb6858d4a969139"
API = f"https://api.github.com/repos/{OWNER}/{REPO}"
ROOT = pathlib.Path(__file__).resolve().parents[1]


def get(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "NightLegion-LiveOn-Port/1.0"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return response.read()


def main() -> None:
    tree = json.loads(get(f"{API}/git/trees/{REF}?recursive=1"))
    copied: list[str] = []
    java_texts: dict[str, str] = {}
    for entry in tree.get("tree", []):
        path = str(entry.get("path") or "")
        if entry.get("type") != "blob":
            continue
        if not (path.startswith("src/main/java/com/liveon/") or path.startswith("src/main/resources/")):
            continue
        raw = f"https://raw.githubusercontent.com/{OWNER}/{REPO}/{REF}/{path}"
        data = get(raw)
        target = ROOT / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        copied.append(path)
        if path.endswith(".java"):
            java_texts[path] = data.decode("utf-8")

    # Keep the exact upstream license in-tree with the vendored source.
    license_data = get(f"https://raw.githubusercontent.com/{OWNER}/{REPO}/{REF}/LICENSE")
    license_target = ROOT / "vendor" / "live-on-clan" / "LICENSE"
    license_target.parent.mkdir(parents=True, exist_ok=True)
    license_target.write_bytes(license_data)

    plugin = java_texts.get("src/main/java/com/liveon/ClanMessagesPlugin.java", "")
    endpoints = sorted(set(re.findall(r'addPathSegment\("([^"]+)"\)', plugin)))
    urls = sorted(set(re.findall(r'https?://[^"\\s)]+', plugin)))
    report = [
        "# Live On exact-source inventory",
        "",
        f"Pinned upstream commit: `{REF}`",
        f"Vendored Java/resource files: **{len(copied)}**",
        "",
        "## HTTP path segments referenced by the upstream plugin",
        "",
    ]
    report.extend(f"- `{value}`" for value in endpoints)
    report.extend(["", "## Literal URLs referenced by the upstream plugin", ""])
    report.extend(f"- `{value}`" for value in urls)
    report.extend(["", "## Vendored files", ""])
    report.extend(f"- `{path}`" for path in sorted(copied))
    (ROOT / "LIVEON_SOURCE_INVENTORY.md").write_text("\n".join(report) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
