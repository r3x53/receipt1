# Labeling workflow (end-to-end)

This document explains how to collect OCR lines from the app and label them using the CSV files in `data/`.

## What you will produce

- **Unlabeled lines** (ready for manual labeling): `data/unlabeled_lines.csv`
- **Labeled batch** (after you add labels): `data/labeled_receipts_batch_001.csv`

---

## Step 1 — Scan receipts (target: 20–30 receipts)

1. Open the app.
2. Go to the **Camera** screen.
3. Scan a receipt (or pick an image from gallery).
4. Tap **View Results**.

Repeat until you have ~20–30 different receipts.

---

## Step 2 — Export OCR lines (Milestone 1 debug button)

1. In the results screen, open the **Raw OCR** tab.
2. Tap **Debug Export Lines**.
3. A message will appear showing where the file was saved (example):
   - `.../cache/ocr_export_20251218_201530.txt`

Do this once per receipt.

### Getting the exported `.txt` files onto your computer

Option A (easiest if you have Android Studio):
1. Open **Android Studio** → **View** → **Tool Windows** → **Device Explorer**.
2. Find the app package (e.g. `com.example.receipto`).
3. Go to the `cache/` folder.
4. Download all `ocr_export_*.txt` files to a folder on your computer.

Option B (using adb):
1. Connect the device via USB and enable Developer Options + USB debugging.
2. Run:
   ```bash
   adb pull /data/user/0/com.example.receipto/cache ./ocr_exports
   ```

---

## Step 3 — Convert exports to CSV (recommended)

Use the helper script in this repo:

```bash
python3 scripts/ocr_exports_to_csv.py \
  --input ./ocr_exports \
  --output data/unlabeled_lines.csv
```

What it does:
- Reads all `*.txt` files in the input folder
- Splits them into individual lines
- Writes a CSV with columns: `line_text,label,receipt_source,notes`
- Leaves `label` empty so you can fill it in manually

If you want stable receipt names, you can override the prefix:

```bash
python3 scripts/ocr_exports_to_csv.py --input ./ocr_exports --output data/unlabeled_lines.csv --receipt-prefix receipt_
```

---

## Step 4 — Label in Google Sheets / Excel

1. Open `data/unlabeled_lines.csv` in Google Sheets or Excel.
2. Fill the `label` column using:
   - `PRICE_LINE`
   - `TEXT_LINE`
3. (Optional) Add notes if something is weird (blurred OCR, cut off line, etc.).

Tips:
- Label quickly first; don’t overthink edge cases.
- If a line contains a real price amount, it’s usually `PRICE_LINE`.

---

## Step 5 — Save the labeled batch

1. Download/export the sheet back to CSV.
2. Save it to the repo as:
   - `data/labeled_receipts_batch_001.csv`

---

## Step 6 — Keep batches organized

Recommended naming:
- `data/labeled_receipts_batch_001.csv`
- `data/labeled_receipts_batch_002.csv`

Keep each batch around ~300 lines so it’s easy to review.
