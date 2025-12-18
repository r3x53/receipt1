# Manual labeling instructions (OCR lines)

Goal: manually label OCR **lines** as either `PRICE_LINE` or `TEXT_LINE`.

This repo uses a *very simple* 2-class labeling scheme on purpose. When in doubt, pick the label that is most useful for teaching the model to find monetary amounts.

---

## Labels

### `PRICE_LINE`
A line that **contains a monetary amount** (price, subtotal, tax, total, discount, change) **or is primarily made of price-like numbers**.

Typical signals:
- Contains a number with **decimal comma or point**: `1,20`, `45.99`
- Contains keywords like: `TOTAL`, `SUBTOTAL`, `TAX`, `IVA`, `VAT`, `IMPORTO`, `DUE`, `CHANGE`
- Looks like: `qty x unit_price = line_total` or `two amounts on the same line`

Examples:
- `"2.50"`
- `"TOTAL 45.99"`
- `"10,80 15,60"`
- `"DISCOUNT -0.50"`
- `"IVA 21% 2,34"`
- `"MILK 1 GAL 3.48"` (item + trailing price)


### `TEXT_LINE`
Any line that is **not primarily a price**.

Common cases:
- Store name, address, cashier, greetings
- Dates/times
- Receipt/transaction IDs
- Phone numbers
- Product descriptions *without* a price on the same line

Examples:
- `"EL COBERTIZO"`
- `"Qty 2"`
- `"Thank you"`
- `"TEL 555-0199"`
- `"2025-12-18 20:15"`

---

## Edge cases (how to decide)

Use this table when you’re unsure:

| Line type | Example | Label | Why |
|---|---|---|---|
| Date/time | `"12/10/2025 14:33"` | `TEXT_LINE` | Not a money amount |
| Phone number | `"TEL 555-0199"` | `TEXT_LINE` | Digits, but not a price |
| Receipt/transaction id | `"TXN 004512"` | `TEXT_LINE` | Identifier, not money |
| Tax line with amount | `"VAT 21% 2,34"` | `PRICE_LINE` | Contains a monetary amount |
| Discount | `"DISCOUNT -0.50"` | `PRICE_LINE` | Still money |
| Item line with price | `"BREAD 1.97"` | `PRICE_LINE` | Contains a price |
| Quantity-only | `"2 x"` / `"Qty 2"` | `TEXT_LINE` | No monetary amount |
| Mixed numbers but not money | `"TABLE 7"` | `TEXT_LINE` | Not money |

If a line contains **at least one real price amount**, it is almost always `PRICE_LINE`.

---

## Quick reference (copy/paste)

- **PRICE_LINE** = line has money amount(s) (including totals/taxes/discounts) or is mostly price-like numbers.
- **TEXT_LINE** = everything else (names, addresses, dates, IDs, phone numbers, greetings, etc.).
- When uncertain: *Would I want a model to treat this as “a price is here”*? If yes → `PRICE_LINE`.

---

## How to use the CSV template

1. Open `data/labeling_template.csv` in Google Sheets / Excel.
2. Copy the header + example rows into a new file (or start from `data/unlabeled_lines.csv`).
3. Fill the `label` column with **exactly** one of:
   - `PRICE_LINE`
   - `TEXT_LINE`
4. Do not change column order.
5. Keep `line_text` exactly as it appeared from OCR (including punctuation).
6. Save the completed file as something like:
   - `data/labeled_receipts_batch_001.csv`
