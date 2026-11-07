"""Generate checked Android feature strings; require each supported language explicitly."""
from pathlib import Path
import json, xml.etree.ElementTree as ET
from xml.sax.saxutils import escape
root = Path(__file__).resolve().parents[1]
res = root / 'app/src/main/res'
languages = [r.attrib['{http://schemas.android.com/apk/res/android}name'] for r in ET.parse(res / 'xml/locales_config.xml').getroot()]
strings = json.loads((root / 'scripts/hifdh-strings.json').read_text())
for language in languages:
    folder = 'values' if language == 'en' else ('values-b+zh+Hans' if language == 'zh-Hans' else f'values-{language}')
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for key, translations in strings.items():
        assert translations.get(language), (key, language)
        text = escape(translations[language]).replace("'", "\\'")
        lines.append(f'    <string name="{key}">{text}</string>')
    lines.append('</resources>')
    (res / folder / 'hifdh_feature_strings.xml').write_text('\n'.join(lines)+'\n')
