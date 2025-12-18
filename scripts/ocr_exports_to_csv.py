#!/usr/bin/env python3

import argparse
import os
import re
from pathlib import Path


def _quote_csv(value: str) -> str:
    return '"' + value.replace('"', '""') + '"'


def _iter_input_files(input_path: Path, glob_pattern: str) -> list[Path]:
    if input_path.is_file():
        return [input_path]

    files = sorted(input_path.glob(glob_pattern))
    return [p for p in files if p.is_file()]


def _receipt_source_for_file(path: Path, *, forced: str | None, prefix: str, index: int) -> str:
    if forced:
        return forced

    m = re.search(r"(receipt[_-]?(\d+))", path.stem, flags=re.IGNORECASE)
    if m:
        num = m.group(2)
        return f"{prefix}{int(num):03d}"

    return f"{prefix}{index:03d}"


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Convert Milestone 1 OCR line exports (text files) into a CSV ready for manual labeling.\n\n"
            "Expected input: one or more .txt files where each line is an OCR line."
        )
    )
    parser.add_argument(
        "--input",
        required=True,
        help="Path to a .txt export file OR a folder containing export files.",
    )
    parser.add_argument(
        "--output",
        required=True,
        help="Output CSV path (e.g. data/unlabeled_lines.csv).",
    )
    parser.add_argument(
        "--glob",
        default="*.txt",
        help="If --input is a folder, which files to include (default: *.txt).",
    )
    parser.add_argument(
        "--receipt-source",
        default=None,
        help="Force a single receipt_source value for all lines (optional).",
    )
    parser.add_argument(
        "--receipt-prefix",
        default="receipt_",
        help="Prefix for auto-generated receipt_source values (default: receipt_).",
    )
    parser.add_argument(
        "--skip-empty",
        action="store_true",
        help="Skip empty/whitespace-only lines.",
    )

    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise SystemExit(f"Input path does not exist: {input_path}")

    files = _iter_input_files(input_path, args.glob)
    if not files:
        raise SystemExit(f"No input files found in {input_path} (glob={args.glob})")

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    rows_written = 0

    with output_path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("line_text,label,receipt_source,notes\n")

        for i, file_path in enumerate(files, start=1):
            receipt_source = _receipt_source_for_file(
                file_path,
                forced=args.receipt_source,
                prefix=args.receipt_prefix,
                index=i,
            )

            for raw_line in file_path.read_text(encoding="utf-8", errors="replace").splitlines():
                line = raw_line.strip("\ufeff")
                if args.skip_empty and not line.strip():
                    continue

                f.write(f"{_quote_csv(line)},,{receipt_source},\n")
                rows_written += 1

    print(f"Wrote {rows_written} lines to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
