#!/usr/bin/env python3
"""
Stream-convert PostgreSQL COPY data blocks into INSERT statements.

The script scans an input SQL dump, rewrites each
`COPY <table> (<columns>) FROM stdin; ... \\.` section into INSERT batches,
and leaves any other statements untouched. It is designed to work line-by-line,
so even ~200 MB dumps can be processed without excessive memory usage.
"""

from __future__ import annotations

import argparse
import io
import re
import sys
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from typing import List, Optional, Sequence

COPY_HEADER_RE = re.compile(
    r"""^COPY\s+
        (?P<table>[^\s(]+(?:\.[^\s(]+)?)
        (?:\s*\((?P<columns>[^)]*)\))?
        \s+FROM\s+stdin;
        $""",
    re.IGNORECASE | re.VERBOSE,
)


@dataclass
class CopyContext:
    table: str
    columns: Optional[Sequence[str]]
    source_line: int


def parse_args(argv: Optional[Sequence[str]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Convert PostgreSQL COPY data blocks into INSERT statements.",
    )
    parser.add_argument(
        "input",
        nargs="?",
        default="-",
        help="Path to the dump (defaults to stdin).",
    )
    parser.add_argument(
        "-o",
        "--output",
        default="-",
        help="Path for the rewritten dump (defaults to stdout).",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=1000,
        help="How many rows to include per INSERT statement (default: 1000).",
    )
    parser.add_argument(
        "--no-bool-coerce",
        dest="coerce_bools",
        action="store_false",
        help="Keep literal 't'/'f' strings instead of converting to TRUE/FALSE.",
    )
    parser.add_argument(
        "--no-numeric-coerce",
        dest="coerce_numeric",
        action="store_false",
        help="Disable numeric detection; emit numbers as quoted strings.",
    )
    parser.set_defaults(coerce_bools=True, coerce_numeric=True)
    return parser.parse_args(argv)


def open_stream(path: str, mode: str) -> io.TextIOBase:
    if path == "-":
        return sys.stdin if "r" in mode else sys.stdout
    return open(path, mode, encoding="utf-8")


def split_columns(raw: str) -> List[str]:
    columns: List[str] = []
    current: List[str] = []
    in_quotes = False
    i = 0
    while i < len(raw):
        ch = raw[i]
        if ch == '"' and not in_quotes:
            in_quotes = True
            current.append(ch)
        elif ch == '"' and in_quotes:
            # Lookahead for escaped quote
            if i + 1 < len(raw) and raw[i + 1] == '"':
                current.extend(['"', '"'])
                i += 1
            else:
                in_quotes = False
                current.append(ch)
        elif ch == "," and not in_quotes:
            columns.append("".join(current).strip())
            current = []
        else:
            current.append(ch)
        i += 1
    if current or raw.endswith(","):
        columns.append("".join(current).strip())
    return [col for col in columns if col]


def decode_copy_field(field: str) -> Optional[str]:
    if field == r"\N":
        return None

    result: List[str] = []
    i = 0
    length = len(field)

    while i < length:
        ch = field[i]
        if ch != "\\":
            result.append(ch)
            i += 1
            continue

        i += 1
        if i >= length:
            result.append("\\")
            break

        esc = field[i]
        if esc in "01234567":
            oct_digits = esc
            i += 1
            for _ in range(2):
                if i < length and field[i] in "01234567":
                    oct_digits += field[i]
                    i += 1
                else:
                    break
            result.append(chr(int(oct_digits, 8)))
            continue

        replacements = {
            "b": "\b",
            "f": "\f",
            "n": "\n",
            "r": "\r",
            "t": "\t",
            "v": "\v",
            "\\": "\\",
            "0": "\0",
        }
        if esc in replacements:
            result.append(replacements[esc])
        else:
            # For any other escaped char, keep the char itself (e.g. \.)
            result.append(esc)
        i += 1

    return "".join(result)


def escape_sql_literal(value: str) -> str:
    needs_e = False
    escaped_parts: List[str] = []
    for ch in value:
        if ch == "'":
            escaped_parts.append("''")
        elif ch == "\\":
            escaped_parts.append("\\\\")
            needs_e = True
        elif ch == "\n":
            escaped_parts.append("\\n")
            needs_e = True
        elif ch == "\r":
            escaped_parts.append("\\r")
            needs_e = True
        elif ch == "\t":
            escaped_parts.append("\\t")
            needs_e = True
        elif ch == "\b":
            escaped_parts.append("\\b")
            needs_e = True
        elif ch == "\f":
            escaped_parts.append("\\f")
            needs_e = True
        elif ch == "\v":
            escaped_parts.append("\\v")
            needs_e = True
        elif ch == "\0":
            escaped_parts.append("\\0")
            needs_e = True
        else:
            escaped_parts.append(ch)
    literal = "".join(escaped_parts)
    prefix = "E" if needs_e else ""
    return f"{prefix}'{literal}'"


def looks_like_integer(value: str) -> bool:
    if not value:
        return False
    if value in ("0", "+0", "-0"):
        return True
    sign = 1 if value[0] in "+-" else 0
    payload = value[sign:]
    if not payload.isdigit():
        return False
    if len(payload) > 1 and payload.startswith("0"):
        return False
    return True


def looks_like_numeric(value: str) -> bool:
    if looks_like_integer(value):
        return True
    try:
        Decimal(value)
    except InvalidOperation:
        return False
    payload = value.lstrip("+-")
    if payload and payload[0] == "0" and payload.isdigit() and len(payload) > 1:
        return False
    return True


def format_value(
    raw: Optional[str],
    *,
    coerce_bools: bool,
    coerce_numeric: bool,
) -> str:
    if raw is None:
        return "NULL"
    if coerce_bools and raw in {"t", "f"}:
        return "TRUE" if raw == "t" else "FALSE"
    if coerce_numeric and looks_like_numeric(raw):
        return raw
    return escape_sql_literal(raw)


def flush_batch(
    out_stream: io.TextIOBase,
    ctx: CopyContext,
    rows: Sequence[str],
) -> None:
    if not rows:
        return
    if ctx.columns:
        column_clause = f" ({', '.join(ctx.columns)})"
    else:
        column_clause = ""
    out_stream.write(
        f"INSERT INTO {ctx.table}{column_clause} VALUES\n  "
        + ",\n  ".join(rows)
        + ";\n\n"
    )


def process_stream(
    in_stream: io.TextIOBase,
    out_stream: io.TextIOBase,
    *,
    batch_size: int,
    coerce_bools: bool,
    coerce_numeric: bool,
) -> None:
    ctx: Optional[CopyContext] = None
    batch: List[str] = []
    line_no = 0

    for raw_line in in_stream:
        line_no += 1
        stripped = raw_line.rstrip("\r\n")

        if ctx is None:
            match = COPY_HEADER_RE.match(stripped)
            if match:
                columns_raw = match.group("columns")
                columns = (
                    split_columns(columns_raw) if columns_raw is not None else None
                )
                ctx = CopyContext(
                    table=match.group("table"),
                    columns=columns,
                    source_line=line_no,
                )
                batch.clear()
                continue
            out_stream.write(raw_line)
            continue

        # Inside COPY data section
        if stripped == r"\.":
            flush_batch(out_stream, ctx, batch)
            ctx = None
            batch.clear()
            continue

        fields = stripped.split("\t")
        if ctx.columns and len(fields) != len(ctx.columns):
            raise ValueError(
                f"Line {line_no}: expected {len(ctx.columns)} columns, got {len(fields)}"
            )

        decoded = [decode_copy_field(field) for field in fields]
        values = [
            format_value(
                value,
                coerce_bools=coerce_bools,
                coerce_numeric=coerce_numeric,
            )
            for value in decoded
        ]
        batch.append(f"({', '.join(values)})")

        if len(batch) >= batch_size:
            flush_batch(out_stream, ctx, batch)
            batch.clear()

    if ctx is not None:
        raise ValueError(
            f"Unexpected end of input while reading COPY block that started on line {ctx.source_line}"
        )


def main() -> None:
    args = parse_args()

    with open_stream(args.input, "r") as in_stream:
        if args.output == "-":
            process_stream(
                in_stream,
                sys.stdout,
                batch_size=args.batch_size,
                coerce_bools=args.coerce_bools,
                coerce_numeric=args.coerce_numeric,
            )
        else:
            with open_stream(args.output, "w") as out_stream:
                process_stream(
                    in_stream,
                    out_stream,
                    batch_size=args.batch_size,
                    coerce_bools=args.coerce_bools,
                    coerce_numeric=args.coerce_numeric,
                )


if __name__ == "__main__":
    main()
