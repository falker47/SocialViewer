#!/usr/bin/env python3
"""Reproducible size report for keyless CI APK and AAB packages.

Not a Google Play download-size, startup, frame-timing, or memory measurement.
"""
from __future__ import annotations
import argparse
import glob
import hashlib
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
DEX_PATH = re.compile(r"^(?:base/dex/)?classes\d*\.dex$")
APP_NAMESPACE = "io.github.falker47.socialviewer"
MAPPING_ENTRY = "BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map"


def defined_class_names(dex: bytes) -> set[str]:
    """Read class_defs, excluding class names which only occur in references."""
    if (len(dex) < 0x70 or dex[:8] not in
            (b"dex\n035\0", b"dex\n037\0", b"dex\n038\0", b"dex\n039\0", b"dex\n040\0")
            or struct.unpack_from("<I", dex, 0x28)[0] != 0x12345678):
        raise ValueError("Unsupported DEX version or byte order for class-name verification")

    def table(header_offset: int, entry_size: int) -> tuple[int, int]:
        count, offset = struct.unpack_from("<II", dex, header_offset)
        if count and (offset < 0x70 or offset + count * entry_size > len(dex)):
            raise ValueError("Invalid DEX table bounds")
        return count, offset

    string_count, strings = table(0x38, 4)
    type_count, types = table(0x40, 4)
    class_count, classes = table(0x60, 32)
    names = set()
    for position in range(classes, classes + 32 * class_count, 32):
        type_index, = struct.unpack_from("<I", dex, position)
        if type_index >= type_count:
            raise ValueError("Class definition has an invalid type index")
        string_index, = struct.unpack_from("<I", dex, types + 4 * type_index)
        if string_index >= string_count:
            raise ValueError("Class definition has an invalid descriptor index")
        offset, = struct.unpack_from("<I", dex, strings + 4 * string_index)
        # string_data_item starts with a ULEB128 UTF-16 length, then MUTF-8.
        for _ in range(5):
            if offset >= len(dex):
                raise ValueError("Truncated DEX class descriptor")
            byte = dex[offset]
            offset += 1
            if byte < 128:
                break
        else:
            raise ValueError("Invalid DEX class descriptor length")
        end = dex.find(b"\0", offset)
        if end < 0 or dex[offset:offset + 1] != b"L" or dex[end-1:end] != b";":
            raise ValueError("Invalid DEX class descriptor")
        # MUTF-8 encodes supplementary characters as two UTF-16 surrogates.
        name = dex[offset + 1:end - 1].decode("utf-8", "surrogatepass")
        name = name.encode("utf-16-le", "surrogatepass").decode("utf-16-le", "surrogatepass")
        names.add(name.replace("/", "."))
    return names


def audit_mapping(archive: zipfile.ZipFile, definitions: set[str]) -> dict:
    if MAPPING_ENTRY not in archive.namelist():
        return {"status": "embedded_mapping_missing"}
    digest = hashlib.sha256()
    compiler = None
    mappings = set()
    prefix = (APP_NAMESPACE + ".").encode()
    with archive.open(MAPPING_ENTRY) as stream:
        for raw in stream:
            digest.update(raw)
            line = raw.rstrip(b"\r\n")
            if line == b"# compiler: R8":
                compiler = "R8"
            if line.startswith(prefix) and line.endswith(b":"):
                original, arrow, target = line[:-1].partition(b" -> ")
                if arrow and target:
                    mappings.add((original.decode(), target.decode()))
    renamed = {(original, target) for original, target in mappings if original != target}
    confirmed = {(original, target) for original, target in renamed if target in definitions}
    return {
        "status": "verified" if compiler == "R8" and confirmed else "unconfirmed",
        "source_namespace": APP_NAMESPACE, "compiler": compiler,
        "mapping_sha256": digest.hexdigest(), "mapped_app_class_pairs": len(mappings),
        "renamed_app_class_pairs": len(renamed),
        "renamed_app_class_pairs_with_dex_definition": len(confirmed),
        "distinct_confirmed_dex_classes": len({target for _, target in confirmed}),
    }

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

def inspect_package(file_path: Path, *, verify_mapping: bool = False) -> dict:
    buckets = {key: {"compressed_bytes": 0, "uncompressed_bytes": 0} for key in BUCKETS}
    dex_count = dex_method_refs = dex_class_defs = 0
    dex_hashes = {}
    class_names = set()
    with zipfile.ZipFile(file_path) as archive:
        for item in archive.infolist():
            if item.is_dir():
                continue
            bucket = classify(item.filename)
            buckets[bucket]["compressed_bytes"] += item.compress_size
            buckets[bucket]["uncompressed_bytes"] += item.file_size
            if bucket == "dex":
                dex = archive.read(item)
                header = dex[:0x70]
                if len(header) < 0x70 or not header.startswith(b"dex\n"):
                    raise ValueError(f"Invalid DEX header: {item.filename} in {file_path}")
                dex_count += 1
                dex_method_refs += struct.unpack_from("<I", header, 0x58)[0]
                dex_class_defs += struct.unpack_from("<I", header, 0x60)[0]
                dex_hashes[item.filename.rsplit("/", 1)[-1]] = hashlib.sha256(dex).hexdigest()
                if verify_mapping:
                    class_names.update(defined_class_names(dex))
        if verify_mapping:
            mapping = audit_mapping(archive, class_names)
    if dex_count == 0:
        raise ValueError(f"No DEX entries found in {file_path}")
    result = {
        "file": file_path.name,
        "archive_bytes": file_path.stat().st_size,
        "dex_files": dex_count,
        "dex_method_id_references": dex_method_refs,
        "dex_class_definitions": dex_class_defs,
        "dex_sha256": dex_hashes,
        "buckets": buckets,
    }
    if verify_mapping:
        result["r8_mapping"] = mapping
    return result

def mib(n: int) -> str:
    return f"{n / 1048576:.3f}"

def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-sha", default="")
    parser.add_argument("--output-dir", default="app/build/reports/size")
    parser.add_argument("--require-r8", action="store_true",
                        help="Exit unsuccessfully after reporting unless release AAB class renaming is verified")
    for kind, pattern in DEFAULT_GLOBS.items():
        parser.add_argument("--" + kind.replace("_", "-"), default=pattern, help="Input file glob")
    args = parser.parse_args()
    report = {
        "source_sha": args.source_sha,
        "unit": "bytes (MiB only in Markdown)",
        "note": "Unsigned/keyless CI packages; no runtime or Play device-delivery claims.",
        "artifacts": {
            kind: inspect_package(unique_file(getattr(args, kind)), verify_mapping=kind == "release_aab")
            for kind in DEFAULT_GLOBS
        },
    }
    artifacts = report["artifacts"]
    same_dex = artifacts["release_apk"]["dex_sha256"] == artifacts["release_aab"]["dex_sha256"]
    report["release_apk_aab_dex_byte_identical"] = same_dex
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
    mapping = artifacts["release_aab"]["r8_mapping"]
    lines.extend(["", "### R8 class renaming", ""])
    if mapping["status"] == "embedded_mapping_missing":
        lines.append("The release AAB has no embedded mapping. Class renaming remains unverified.")
    else:
        lines.append(
            f"Embedded mapping compiler: {mapping['compiler'] or 'unidentified'}. "
            f"Renamed class mappings in `{APP_NAMESPACE}` with a target defined in final AAB DEX: "
            f"{mapping['renamed_app_class_pairs_with_dex_definition']} of "
            f"{mapping['renamed_app_class_pairs']} renamed pairs, across "
            f"{mapping['distinct_confirmed_dex_classes']} distinct output classes. "
            f"Total app class mappings: {mapping['mapped_app_class_pairs']}."
        )
    lines.extend([
        "",
        f"Release APK and base AAB DEX entries have identical names and SHA-256 hashes: {same_dex}.",
        "Debug includes Compose UI tooling. Differences between debug and release package sizes "
        "combine variant dependencies, optimization and packaging.",
    ])
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
    if args.require_r8 and mapping["status"] != "verified":
        raise SystemExit(f"Required R8 class renaming evidence: {mapping['status']}")

if __name__ == "__main__":
    main()
