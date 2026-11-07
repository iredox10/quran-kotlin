"""Build edition navigation from bundled publisher corpora and the verified original IndoPak PDF.
Does not modify Quran text or map planner/completion records across narrations.
"""
import json, re, unicodedata, pathlib
import pymupdf
ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets/data'
chapters = json.loads((ASSETS / 'chapters.json').read_text())
full = {r['verse_key']: r['text_uthmani'] for r in json.loads((ASSETS / 'quran_full.json').read_text())}
kt = (ROOT / 'app/src/main/java/com/nur/quran/data/QuranNavigation.kt').read_text().split('val HIZB_STARTS = listOf(')[1]
hizb = re.findall(r'DivisionStart\((\d+),\s*"([\d:]+)",\s*(\d+)\)', kt)
assert len(hizb) == 60

def norm(s):
    s = unicodedata.normalize('NFKD', s).translate(str.maketrans({'ٱ':'ا', 'ى':'ي', 'ے':'ي', 'ی':'ي', 'ک':'ك', 'ٰ':'ا', 'ـ':'', 'ء':''}))
    return ''.join(c for c in s if '\u0622' <= c <= '\u064a')

def page(row):
    return int(re.search(r'\d+', row['page'])[0])

def write(edition, value):
    (ASSETS / 'mushafs' / f'{edition}_browse.json').write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

for edition in ['warsh', 'qaloon']:
    rows = json.loads((ASSETS / 'mushafs' / f'{edition}.json').read_text())
    groups = {c['id']: [r for r in rows if r['sura_no'] == c['id']] for c in chapters}
    divisions = []
    for number, key, _ in hizb:
        ch, verse = map(int, key.split(':'))
        candidates = groups[ch]
        proof = 'surah-start'
        if verse == 1:
            target = candidates[0]
        else:
            flat = ''.join(norm(r['aya_text']) for r in candidates)
            words = [w for w in full[key].split() if norm(w)]
            match = None
            for skipped in [0, 1]:
                for size in [40, 32, 24, 16]:
                    anchor = norm(' '.join(words[skipped:]))[:size]
                    positions = list(re.finditer(re.escape(anchor), flat)) if len(anchor) >= 12 else []
                    if len(positions) == 1:
                        match = positions[0].start()
                        proof = anchor
                        break
                if match is not None:
                    break
            assert match is not None, (edition, number, key)
            offset = 0
            for candidate in candidates:
                length = len(norm(candidate['aya_text']))
                if offset <= match < offset + length:
                    target = candidate
                    break
                offset += length
            else:
                raise AssertionError((edition, number, key))
        divisions.append({'id': int(number), 'page': page(target), 'surah': ch, 'nativeAyahId': target['id'], 'anchor': proof})
    juz = []
    for number in range(1, 31):
        target = next(r for r in rows if r['jozz'] == number)
        juz.append({'id': number, 'page': page(target), 'surah': target['sura_no']})
    write(edition, {'firstPage': 1, 'lastPage': 604,
        'chapters': [{'id': ch, 'page': page(rs[0]), 'count': len(rs)} for ch, rs in groups.items()],
        'juz': juz, 'hizb': divisions})

pdf = ROOT / 'build/indopak-13-line.pdf'
import hashlib
assert hashlib.sha256(pdf.read_bytes()).hexdigest() == 'ea71bbf6b01ecf6e9d827bb763a6618c0c10de170688c497c5bfd1b472ba8b48'
toc = pymupdf.open(pdf).get_toc()
starts = {int(re.search(r'Juz\s+(\d+)', title)[1]): p-2 for _, title, p in toc if re.fullmatch(r'\s*Juz\s+\d+\s*', title)}
halves = {int(re.search(r'(\d+)-2', title)[1]): p-2 for _, title, p in toc if re.fullmatch(r'\s*\d{2}-2\s*', title)}
assert len(starts) == len(halves) == 30
navigation = json.loads((ASSETS / 'mushafs/indopak_navigation.json').read_text())
def surah_at(p):
    start = max(r['page'] for r in navigation if r['page'] <= p)
    return next(r['surah'] for r in navigation if r['page'] == start)
write('indopak', {'firstPage': 2, 'lastPage': 848,
    'chapters': [{'id': r['surah'], 'page': r['page'], 'count': chapters[r['surah'] - 1]['verses_count']} for r in navigation],
    'juz': [{'id': i, 'page': p, 'surah': surah_at(p)} for i, p in sorted(starts.items())],
    'hizb': [{'id': 2*i - 1 + half, 'page': p, 'surah': surah_at(p)} for i in range(1, 31) for half, p in enumerate([starts[i], halves[i]])]})
