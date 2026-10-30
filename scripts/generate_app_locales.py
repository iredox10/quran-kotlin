#!/usr/bin/env python3
"""Build-time assisted UI translations. Runtime uses bundled Android resources only.

Existing hand-written locale resources win. Cached results are keyed by source text.
Numbered Android format tokens and escapes are protected and verified per string.
Use --check for an offline coverage/placeholder audit without making requests.
"""
from __future__ import annotations
import argparse, concurrent.futures, hashlib, json, os, re, threading, time
import urllib.parse, urllib.request, xml.etree.ElementTree as ET
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
LANGUAGES = ['ar','fr','es','pt','de','it','ru','zh-Hans','ja','ko','hi','bn','ur','id','ms','tr','fa','sw','ha','yo']
CACHE = Path.home() / '.cache/quran-nur/ui-translation-cache.json'
LOCK = threading.Lock()
FORMAT = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]')
PROTECT = re.compile(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]|\\[ntr]|\\u[0-9a-fA-F]{4}|<[^>]+>|Quran Nur|QPC|IndoPak|Hafs|Hizbs?', re.I)


def folder(code): return 'values-b+zh+Hans' if code == 'zh-Hans' else f'values-{code}'
def read_entries(paths):
    entries = {}
    for path in sorted(paths):
        root = ET.parse(path).getroot()
        for item in root.findall('string'):
            if item.get('translatable') == 'false': continue
            name = item.get('name')
            if name in entries: raise ValueError(f'Duplicate resource {name}: {path}')
            entries[name] = ''.join(item.itertext())
    return entries


def protected(text):
    text = text.replace("\\'", "'").replace('\\"','"')
    tokens = []
    def mark(match):
        tokens.append(match.group(0))
        return f'ZXQPLACE{len(tokens)-1}ZXQ'
    return PROTECT.sub(mark,text),tokens


def encode_android(text, tokens):
    text = text.strip().replace('\\n','\n')
    for index in range(len(tokens)):
        marker = re.compile(r'Z\s*X\s*Q\s*P\s*L\s*A\s*C\s*E\s*'+str(index)+r'\s*Z\s*X\s*Q', re.I)
        text = marker.sub(f'ZXQPLACE{index}ZXQ',text)
    if any(text.count(f'ZXQPLACE{i}ZXQ') != 1 for i in range(len(tokens))):
        raise ValueError('Translation changed a protected placeholder')
    text = text.replace('\\','\\\\').replace("'","\\'").replace('"','\\"').replace('\n','\\n')
    for index, token in enumerate(tokens): text = text.replace(f'ZXQPLACE{index}ZXQ',token)
    return text


def valid_translation(value, source):
    if not value.strip() or re.search(r'ZXQPLACE|[\[【]\d{4}[\]】]', value, re.I): return False
    if value.count(chr(92)+'n') > source.count(chr(92)+'n') + source.count(chr(10)): return False
    return sorted(FORMAT.findall(value)) == sorted(FORMAT.findall(source))


def request_translation(text, language):
    query = urllib.parse.urlencode({'client':'gtx','sl':'en','tl':'zh-CN' if language=='zh-Hans' else language,'dt':'t','q':text})
    request = urllib.request.Request('https://translate.googleapis.com/translate_a/single?'+query, headers={'User-Agent':'Mozilla/5.0'})
    for attempt in range(6):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                result=json.load(response)
            return ''.join(part[0] or '' for part in result[0])
        except Exception:
            if attempt==5: raise
            time.sleep(min(45,2**attempt*2))


def translate_batch(batch, language, cache):
    prepared=[]
    for i,(name,source) in enumerate(batch):
        plain,tokens=protected(source)
        prepared.append((name,source,tokens,f'[{i:04d}] {plain}'))
    result=request_translation('\n'.join(row[3] for row in prepared),language).replace('\\n','\n')
    markers=list(re.finditer(r'\[(\d{4})\]\s*',result))
    outputs={}
    for i,m in enumerate(markers):
        index=int(m.group(1)); end=markers[i+1].start() if i+1<len(markers) else len(result)
        outputs[index]=result[m.end():end]
    # Some languages remove repeated list numbers but preserve one output line per input.
    # Use positional lines only when counts and every retained explicit index agree.
    lines=[line.strip() for line in result.splitlines() if line.strip()]
    if len(markers) != len(prepared) and len(lines) == len(prepared):
        valid=True
        positional={}
        for index,line in enumerate(lines):
            marker=re.match(r'\[(\d{4})\]\s*',line)
            if marker and int(marker.group(1)) != index:
                valid=False;break
            positional[index]=line[marker.end():] if marker else line
        if valid:outputs=positional
    for index,(name,source,tokens,_) in enumerate(prepared):
        try:
            value=encode_android(outputs[index],tokens)
            if not valid_translation(value, source):
                raise ValueError('Invalid batch translation')
        except (KeyError,ValueError):
            # Retry the individual string when the provider reformats batch markers.
            raw=request_translation(protected(source)[0],language)
            if not (chr(92)+'n') in source and chr(10) not in source:
                raw=raw.replace(chr(10),' ').replace(chr(92)+'n',' ')
            value=encode_android(raw,tokens)
            if not valid_translation(value,source):
                raise ValueError(f'Invalid individual translation for {language}:{name}')
        with LOCK: cache[f'{language}|{source}']=value
    with LOCK:
        CACHE.parent.mkdir(parents=True,exist_ok=True)
        temp=CACHE.with_suffix('.tmp');temp.write_text(json.dumps(cache,ensure_ascii=False),encoding='utf-8');temp.replace(CACHE)


def generate(language, defaults, cache):
    target=RES/folder(language); target.mkdir(exist_ok=True)
    generated=target/'generated_strings.xml'
    existing=read_entries(p for p in target.glob('*.xml') if p!=generated)
    needed=[(name,text) for name,text in defaults.items() if name not in existing]
    pending=[entry for entry in needed if f'{language}|{entry[1]}' not in cache]
    batches=[];batch=[];size=0
    for entry in pending:
        if size+len(entry[1])+25>2000 or len(batch)>=30:
            if batch:batches.append(batch)
            batch=[];size=0
        batch.append(entry);size+=len(entry[1])+25
    if batch:batches.append(batch)
    for index,batch in enumerate(batches):
        translate_batch(batch,language,cache)
        if index%5==0: print(f'{language}: {index+1}/{len(batches)} batches',flush=True)
        time.sleep(.2)
    lines=['<?xml version="1.0" encoding="utf-8"?>','<!-- Assisted UI translations; scripture text is never translated by this script. -->','<resources>']
    for name,text in needed:
        with LOCK:value=cache[f'{language}|{text}']
        lines.append(f'    <string name="{name}">{escape(value)}</string>')
    lines.append('</resources>')
    generated.write_text('\n'.join(lines)+'\n',encoding='utf-8')
    print(f'{language}: complete ({len(needed)} generated, {len(existing)} reviewed)',flush=True)


def check(defaults):
    errors=[]
    for language in LANGUAGES:
        entries=read_entries((RES/folder(language)).glob('*.xml'))
        missing=defaults.keys()-entries.keys()
        if missing: errors.append(f'{language}: {len(missing)} missing, e.g. {sorted(missing)[:3]}')
        for name in defaults.keys()&entries.keys():
            if not valid_translation(entries[name],defaults[name]):
                errors.append(f'{language}:{name}: invalid text or placeholders')
    if errors: raise SystemExit('\n'.join(errors))
    print(f'OK: {len(defaults)} resources across {len(LANGUAGES)+1} languages; format tokens preserved.')


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');parser.add_argument('--languages',nargs='*');args=parser.parse_args()
    defaults=read_entries((RES/'values').glob('*.xml'))
    if args.check: check(defaults);return
    cache=json.loads(CACHE.read_text()) if CACHE.exists() else {}
    invalid=[key for key,value in cache.items() if not valid_translation(value,key.split('|',1)[1])]
    for key in invalid:del cache[key]
    if invalid:print(f'Retrying {len(invalid)} invalid cached translations individually as needed.',flush=True)
    languages=args.languages or LANGUAGES
    with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
        futures={pool.submit(generate,code,defaults,cache):code for code in languages}
        for future in concurrent.futures.as_completed(futures):
            try:future.result()
            except Exception as exc:
                print(f'FAILED {futures[future]}: {exc}',flush=True)
                raise
    if languages==LANGUAGES:check(defaults)

if __name__=='__main__':main()
