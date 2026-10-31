#!/usr/bin/env python3
"""
Извлекает данные каталога из Excel-ведомости AS460 и пишет JSON для seed-загрузчика.

Использование:
    python3 scripts/extract-seed-data.py <excel.xlsx> <output.json>

Маппинг:
    - колонка "Дет или СЕ":  "СЕ" -> ASSEMBLY, "Д" -> PART
    - колонка "Обозначение"  -> number
    - колонка "Наименование" -> name

Дедупликация по number (одна и та же деталь встречается в разных узлах ведомости).
Требуется: openpyxl (pip install openpyxl).
"""
import argparse
import json

import openpyxl

# Индексы колонок главного листа "АСВ" (0-based): B=1, H=7, I=8
COL_KIND = 1
COL_NUMBER = 7
COL_NAME = 8

MAIN_SHEET = "АСВ"
HEADER_ROW = 9  # строки данных начинаются после шапки (строки 1..8)


def extract(excel_path: str) -> list[dict]:
    wb = openpyxl.load_workbook(excel_path, read_only=True, data_only=True)
    ws = wb[MAIN_SHEET]

    seen: set[str] = set()
    items: list[dict] = []

    for row in ws.iter_rows(min_row=HEADER_ROW, values_only=True):
        kind = str(row[COL_KIND]).strip() if row[COL_KIND] is not None else ""
        number = str(row[COL_NUMBER]).strip() if row[COL_NUMBER] is not None else ""
        name = str(row[COL_NAME]).strip() if row[COL_NAME] is not None else ""

        if not number or not name:
            continue
        if kind == "СЕ":
            type_ = "ASSEMBLY"
        elif kind == "Д":
            type_ = "PART"
        else:
            continue
        if number in seen:
            continue

        seen.add(number)
        items.append({"number": number, "name": name, "type": type_})

    wb.close()
    return items


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("excel", help="путь к Excel-файлу ведомости")
    parser.add_argument("output", help="путь к выходному JSON")
    args = parser.parse_args()

    items = extract(args.excel)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(items, f, ensure_ascii=False, indent=2)

    print(f"записей: {len(items)} -> {args.output}")


if __name__ == "__main__":
    main()
