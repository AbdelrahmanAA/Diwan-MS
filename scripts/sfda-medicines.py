#!/usr/bin/env python3
"""
Builds the medicine catalogue of diwan-medical from SFDA's public list of registered drugs
(https://www.sfda.gov.sa/en/drugs-list). SFDA publishes it only as web pages, so this reads them one by one,
slowly (one page every few seconds), as a normal client that says who it is. Run it by hand when the list should
be refreshed, then commit the CSV:

    python scripts/sfda-medicines.py diwan-medical/src/main/resources/medicines/sfda-drugs.csv

Rows that differ only in price or registration number are merged.
"""
import csv
import html
import re
import sys
import time
import urllib.request

BASE = 'https://www.sfda.gov.sa/en/drugs-list?page={}'
AGENT = 'DiwanMedicineCatalog/1.0 (personal health records app; https://github.com/AbdelrahmanAA/Diwan-MS)'
DELAY_SECONDS = 2.0
ROW = re.compile(r'<tr class="(?:odd|even)">(.*?)</tr>', re.S)
CELL = re.compile(r'<td>(.*?)</td>', re.S)
PAGE = re.compile(r'[?&](?:amp;)?page=(\d+)')


def fetch(page, attempts=3):
    for attempt in range(attempts):
        try:
            req = urllib.request.Request(BASE.format(page), headers={'User-Agent': AGENT})
            with urllib.request.urlopen(req, timeout=30) as r:
                return r.read().decode('utf-8')
        except Exception as e:  # network hiccup: wait longer and retry
            if attempt == attempts - 1:
                raise
            print(f'page {page}: {e}, retrying', file=sys.stderr)
            time.sleep(DELAY_SECONDS * 5)


def text(cell):
    return re.sub(r'\s+', ' ', html.unescape(re.sub(r'<[^>]+>', '', cell))).strip()


def rows(page_html):
    for row in ROW.findall(page_html):
        cells = [text(c) for c in CELL.findall(row)]
        if len(cells) >= 5 and cells[1]:
            scientific, trade, strength, form, price = cells[:5]
            yield {'trade': trade.rstrip('*').strip(), 'scientific': scientific, 'strength': strength,
                   'form': form, 'price': price}


def main(out):
    first = fetch(0)
    last = max(int(p) for p in PAGE.findall(first))
    seen, catalogue = set(), []
    for page in range(0, last + 1):
        body = first if page == 0 else fetch(page)
        for r in rows(body):
            key = (r['trade'].upper(), r['scientific'].upper(), r['strength'].upper(), r['form'].upper())
            if key not in seen:
                seen.add(key)
                catalogue.append(r)
        if page % 50 == 0:
            print(f'page {page}/{last}: {len(catalogue)} medicines', file=sys.stderr)
        time.sleep(DELAY_SECONDS)
    if len(catalogue) < 1000:
        sys.exit(f'only {len(catalogue)} medicines found: the page layout may have changed, nothing written')
    catalogue.sort(key=lambda r: (r['trade'].upper(), r['strength']))
    with open(out, 'w', encoding='utf-8', newline='') as f:
        w = csv.DictWriter(f, fieldnames=['trade', 'scientific', 'strength', 'form', 'price'])
        w.writeheader()
        w.writerows(catalogue)
    print(f'{len(catalogue)} medicines written to {out}', file=sys.stderr)


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
