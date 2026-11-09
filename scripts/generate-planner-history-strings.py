"""Generate history strings, requiring every configured app locale explicitly."""
from pathlib import Path
import json
import xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
strings = json.loads((root / 'scripts/planner-history-strings.json').read_text())
for entry in ET.parse(res / 'xml/locales_config.xml').getroot():
    locale = entry.attrib['{http://schemas.android.com/apk/res/android}name']
    folder = 'values' if locale == 'en' else ('values-b+zh+Hans' if locale == 'zh-Hans' else f'values-{locale}')
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for key, translations in strings.items():
        assert translations.get(locale), (key, locale)
        text = escape(translations[locale]).replace("'", "\\'")
        lines.append(f'    <string name="{key}">{text}</string>')
    lines.append('</resources>')
    (res / folder / 'planner_history_strings.xml').write_text('\n'.join(lines)+'\n')
