import contextlib
import hashlib
import io
import json
import os
import struct
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

import android_size_report as report


def dex_fixture(names: list[str], defined_indices: list[int]) -> bytes:
    """Minimal tables read by the report. Unlisted types remain references."""
    strings = 0x70
    types = strings + len(names) * 4
    classes = types + len(names) * 4
    data = bytearray(classes + len(defined_indices) * 32)
    data[:8] = b"dex\n039\0"
    struct.pack_into("<II", data, 0x24, 0x70, 0x12345678)
    struct.pack_into("<II", data, 0x38, len(names), strings)
    struct.pack_into("<II", data, 0x40, len(names), types)
    struct.pack_into("<II", data, 0x60, len(defined_indices), classes)
    for i, name in enumerate(names):
        descriptor = ("L" + name.replace(".", "/") + ";").encode("ascii")
        struct.pack_into("<I", data, types + 4 * i, i)
        struct.pack_into("<I", data, strings + 4 * i, len(data))
        data.extend(bytes([len(descriptor)]) + descriptor + b"\0")
    for i, type_index in enumerate(defined_indices):
        struct.pack_into("<I", data, classes + i * 32, type_index)
    struct.pack_into("<I", data, 0x20, len(data))
    return bytes(data)


class AndroidSizeAuditTest(unittest.TestCase):
    def test_only_defined_app_renames_count_including_merged_targets(self):
        ns = report.APP_NAMESPACE
        mapping = (
            f"# compiler: R8\n{ns}.First -> a:\n    void go() -> c\n"
            f"{ns}.Second -> a:\n{ns}.ReferencedOnly -> b:\n{ns}.Kept -> {ns}.Kept:\n"
            f"{ns}extra.ThirdParty -> a:\n"
        ).encode()
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "release.aab"
            with zipfile.ZipFile(path, "w") as archive:
                archive.writestr("base/dex/classes.dex", dex_fixture(["b", ns + ".Kept"], [1]))
                archive.writestr("base/dex/classes2.dex", dex_fixture(["a"], [0]))
                archive.writestr(report.MAPPING_ENTRY, mapping)
            artifact = report.inspect_package(path, verify_mapping=True)
        self.assertEqual(artifact["dex_class_definitions"], 2)
        self.assertEqual(artifact["r8_mapping"], {
            "status": "verified", "source_namespace": ns, "compiler": "R8",
            "mapping_sha256": hashlib.sha256(mapping).hexdigest(),
            "mapped_app_class_pairs": 4, "renamed_app_class_pairs": 3,
            "renamed_app_class_pairs_with_dex_definition": 2, "distinct_confirmed_dex_classes": 1,
        })

    def test_cli_distinguishes_missing_mapping_and_byte_identity_from_equal_counts(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            apk = root / "example.apk"
            aab = root / "example.aab"
            out = root / "report"
            with zipfile.ZipFile(aab, "w") as archive:
                archive.writestr("base/dex/classes.dex", dex_fixture(["a"], [0]))
            args = ["report", "--debug-apk", str(apk), "--release-apk", str(apk),
                    "--release-aab", str(aab), "--output-dir", str(out)]
            for name, expect_same in [("a", True), ("z", False)]:
                with zipfile.ZipFile(apk, "w") as archive:
                    archive.writestr("classes.dex", dex_fixture([name], [0]))
                with patch.object(sys, "argv", args), patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": ""}):
                    with contextlib.redirect_stdout(io.StringIO()):
                        report.main()
                results = json.loads((out / "metrics.json").read_text())
                self.assertEqual(results["release_apk_aab_dex_byte_identical"], expect_same)
                self.assertEqual(results["artifacts"]["release_aab"]["r8_mapping"],
                                 {"status": "embedded_mapping_missing"})

    def test_mapping_only_and_unidentified_compiler_do_not_prove_r8(self):
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w") as archive:
            archive.writestr(report.MAPPING_ENTRY, report.APP_NAMESPACE + ".Example -> a:\n")
        with zipfile.ZipFile(stream) as archive:
            self.assertEqual(report.audit_mapping(archive, set())["status"], "unconfirmed")
            self.assertEqual(report.audit_mapping(archive, {"a"})["status"], "unconfirmed")

    def test_require_r8_exit_status_and_retained_reports(self):
        cases = [(None, "embedded_mapping_missing", 1), ("absent", "unconfirmed", 1), ("a", "verified", 0)]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            apk = root / "release.apk"
            aab = root / "release.aab"
            with zipfile.ZipFile(apk, "w") as archive:
                archive.writestr("classes.dex", dex_fixture(["different"], [0]))
            for target, status, code in cases:
                with self.subTest(status=status):
                    out = root / status
                    with zipfile.ZipFile(aab, "w") as archive:
                        archive.writestr("base/dex/classes.dex", dex_fixture(["a"], [0]))
                        if target:
                            archive.writestr(report.MAPPING_ENTRY,
                                             f"# compiler: R8\n{report.APP_NAMESPACE}.Original -> {target}:\n")
                    run = subprocess.run(
                        [sys.executable, "-B", report.__file__, "--require-r8", "--output-dir", str(out),
                         "--debug-apk", str(apk), "--release-apk", str(apk), "--release-aab", str(aab)],
                        capture_output=True, text=True, env={**os.environ, "GITHUB_STEP_SUMMARY": ""},
                    )
                    self.assertEqual(run.returncode, code, run.stderr)
                    results = json.loads((out / "metrics.json").read_text())
                    self.assertEqual(results["artifacts"]["release_aab"]["r8_mapping"]["status"], status)
                    self.assertFalse(results["release_apk_aab_dex_byte_identical"])
                    self.assertIn("R8 class renaming", (out / "metrics.md").read_text())
                    if code:
                        self.assertIn(status, run.stderr)

    def test_corrupt_class_index_is_rejected(self):
        dex = bytearray(dex_fixture(["a"], [0]))
        classes, = struct.unpack_from("<I", dex, 0x64)
        struct.pack_into("<I", dex, classes, 1)
        with self.assertRaisesRegex(ValueError, "type index"):
            report.defined_class_names(dex)


if __name__ == "__main__":
    unittest.main()
