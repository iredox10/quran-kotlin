"""Compare Android's rendered icon grid to the web's original SVG primitives.
Run WebIconRenderingTest, pull its PNG/bounds JSON into build/, then run this script.
MuPDF is the independent SVG renderer; small antialiasing differences are allowed.
"""
from pathlib import Path
import json, html
import pymupdf
from PIL import Image, ImageChops, ImageStat

ROOT = Path(__file__).resolve().parents[1]
fixture = json.loads((ROOT / 'app/src/androidTest/assets/lucide-web-icons.json').read_text())
bounds = json.loads((ROOT / 'build/lucide-native-bounds.json').read_text())
grid = Image.open(ROOT / 'build/lucide-native-grid.png').convert('RGB')
failures = []
worst_error = 0
for icon in fixture['icons']:
    name = icon['name']
    elements = []
    for tag, attrs in icon['nodes']:
        attributes = ' '.join(f'{k}="{html.escape(str(v), quote=True)}"' for k, v in attrs.items() if k != 'key')
        elements.append(f'<{tag} {attributes}/>')
    fill = 'black' if icon['filled'] else 'none'
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="{fill}" stroke="black" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">{"".join(elements)}</svg>'
    doc = pymupdf.open(stream=svg.encode(), filetype='svg')
    size = bounds[name][2] - bounds[name][0]
    page = doc[0]
    pix = page.get_pixmap(matrix=pymupdf.Matrix(size / page.rect.width, size / page.rect.height), alpha=False)
    reference = Image.frombytes('RGB', (pix.width, pix.height), pix.samples)
    actual = grid.crop(tuple(bounds[name]))
    assert actual.size == reference.size, (name, actual.size, reference.size)
    error = sum(ImageStat.Stat(ImageChops.difference(actual, reference)).mean) / (3 * 255)
    worst_error = max(worst_error, error)
    native_ink = actual.convert('L').point(lambda v: 255 if v < 160 else 0).getbbox()
    web_ink = reference.convert('L').point(lambda v: 255 if v < 160 else 0).getbbox()
    bbox_delta = max(abs(a-b) for a, b in zip(native_ink, web_ink)) if native_ink and web_ink else 999
    if error > .025 or bbox_delta > 1:
        failures.append({'name': name, 'mean_pixel_error': round(error, 5), 'bounds_delta_px': bbox_delta})
report = {'web_library': fixture['library'], 'version': fixture['version'], 'icons': len(fixture['icons']),
          'worst_mean_pixel_error': round(worst_error, 5), 'failures': failures}
(ROOT / 'build/lucide-rendering-comparison.json').write_text(json.dumps(report, indent=2)+'\n')
print(json.dumps(report, indent=2))
assert not failures, 'Android icon geometry differs from the original web SVG'
