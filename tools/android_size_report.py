#!/usr/bin/env python3
"""Reproducible size report for keyless CI APK and AAB packages.

Not a Google Play download-size, startup, frame-timing, or memory measurement.
"""
from __future__ import annotations
import argparse
import glob
import json
import os
import re
import struct
import zipfile
from pathlib import Path

DEFAULT_GLOBS = {
    "debug_apk": "app/build/outputs/apk/debug/*.apk",
    "release_apk": "app/build/outputs/apk/release/*.apk",
    "release_aab": "app/build/outputs/bundle/release/*.aab",
}
BUCKETS = ("dex", "resources", "native", "assets", "other")
DEX_PATH = re.compile(r"^(?:base/dex/)?classes\\d*\\.dex$")

def classify(path: str) -> str:
    if DEX_PATH.fullmatch(path):
        return "dex"
    if path.startswith(("res/", "base/res/", "base/root/res/")) or path in (
        "resources.arsc", "base/resources.pb",
    ):
        return "resources"
    if path.startswith(("lib/", "base/lib/")):
        return "native"
    if path.startswith(("assets/", "base/assets/")):
        return "assets"
    return "other"

def unique_file(pattern: str) -> Path:
    matches = sorted(Path(path) for path in glob.glob(pattern) if Path(path).is_file())
    if len(matches) != 1:
        raise ValueError(f"Expected exactly one file for {pattern!r}; found {len(matches)}")
    return matches[0]

def inspect_package(file_path: Path) -> dict:
    buckets = {key: {"compressed_bytes": 0, "uncompressed_bytes": 0} for key in BUCKETS}
    dex_count = dex_method_refs = dex_class_defs = 0
    with zipfile.ZipFile(file_path) as archive:
        for item in archive.infolist():
            if item.is_dir():
                continue
            bucket = classify(item.filename)
            buckets[bucket]["compressed_bytes"] += item.compress_size
            buckets[bucket]["uncompressed_bytes"] += item.file_size
            if bucket == "dex":
                with archive.open(item) as dex:
                    header = dex.read(0x70)
                if len(header) < 0x70 or not header.startswith(b"dex\\n"):
                    raise ValueError(f"Invalid DEX header: {item.filename} in {file_path}")
                dex_count += 1
                dex_method_refs += struct.unpack_from("<I", header, 0x58)[0]
                dex_class_defs += struct.unpack_from("<I", header, 0x60)[0]
    if dex_count == 0:
        raise ValueError(f"No DEX entries found in {file_path}")
    return {
        "file": file_path.name,
        "archive_bytes": file_path.stat().st_size,
        "dex_files": dex_count,
        "dex_method_id_references": dex_method_refs,
        "dex_class_definitions": dex_class_defs,
        "buckets": buckets,
    }

def mib(n: int) -> str:
    return f"{n / 1048576:.3f}"

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-sha", default="")
    parser.add_argument("--output-dir", default="app/build/reports/size")
    for kind, pattern in DEFAULT_GLOBS.items():
        parser.add_argument("--" + kind.replace("_", "-"), default=pattern, help="Input file glob")
    args = parser.parse_args()
    report = {
        "source_sha": args.source_sha,
        "unit": "bytes (MiB only in Markdown)",
        "note": "Unsigned/keyless CI packages; no runtime or Play device-delivery claims.",
        "artifacts": {
            kind: inspect_package(unique_file(getattr(args, kind)))
            for kind in DEFAULT_GLOBS
        },
    }
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    (output_dir / "metrics.json").write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    lines = [
        "## Android package-size baseline",
        "",
        f"Source commit: {args.source_sha or 'local / unspecified'}",
        "",
        "| CI artifact | Archive (MiB) | Compressed DEX (MiB) | Raw DEX (MiB) | DEX files | DEX method-ID refs* |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    for kind, info in report["artifacts"].items():
        dex = info["buckets"]["dex"]
        lines.append(
            f"| {kind} | {mib(info['archive_bytes'])} | {mib(dex['compressed_bytes'])} | "
            f"{mib(dex['uncompressed_bytes'])} | {info['dex_files']} | "
            f"{info['dex_method_id_references']:,} |"
        )
    lines.extend([
        "",
        "*Method-ID references are summed per DEX, not unique methods.*",
        "",
        "### Compressed component sizes (MiB)",
        "",
        "| CI artifact | DEX | Resources | Native libraries | Assets | Other |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ])
    for kind, info in report["artifacts"].items():
        sizes = [mib(info["buckets"][bucket]["compressed_bytes"]) for bucket in BUCKETS]
        lines.append(f"| {kind} | " + " | ".join(sizes) + " |")
    lines.extend([
        "",
        "**Scope:** unsigned/keyless CI packages. Release AAB is not Play download size; "
        "startup, frame timing, memory and provider/network/cookie behavior require "
        "separate measurement.",
        "",
    ])
    md = "\n".join(lines)
    (output_dir / "metrics.md").write_text(md, encoding="utf-8")
    if summary_file := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(summary_file, "a", encoding="utf-8") as handle:
            handle.write(md)
    print(md)

if __name__ == "__main__":
    main()
