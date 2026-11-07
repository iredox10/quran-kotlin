#!/usr/bin/env node
// Read the web app's actual Lucide exports; preserve paths and convert SVG primitives exactly.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const args = process.argv.slice(2);
const webRoot = path.resolve(args.find(arg => !arg.startsWith('--')) || path.join(root, '../quran-app'));
const library = path.join(webRoot, 'node_modules/lucide-react');
const lucide = require(library);
const version = require(path.join(library, 'package.json')).version;
const target = path.join(root, 'app/src/main/java/com/nur/quran/ui/components/NurIcons.kt');
const names = [...fs.readFileSync(target, 'utf8').matchAll(/^\s+val (\w+): ImageVector/gm)].map(match => match[1]);
for (const name of ['Settings2', 'SkipBack', 'SkipForward', 'SquareFilled']) if (!names.includes(name)) names.push(name);
const n = value => String(Number(value));
function geometry(tag, a) {
    switch (tag) {
        case 'path': return a.d;
        case 'circle': {
            const x = +a.cx, y = +a.cy, r = +a.r;
            return `M${n(x-r)} ${n(y)}a${n(r)} ${n(r)} 0 1 0 ${n(2*r)} 0a${n(r)} ${n(r)} 0 1 0 ${n(-2*r)} 0Z`;
        }
        case 'ellipse': {
            const x = +a.cx, y = +a.cy, rx = +a.rx, ry = +a.ry;
            return `M${n(x-rx)} ${n(y)}a${n(rx)} ${n(ry)} 0 1 0 ${n(2*rx)} 0a${n(rx)} ${n(ry)} 0 1 0 ${n(-2*rx)} 0Z`;
        }
        case 'line': return `M${n(a.x1)} ${n(a.y1)}L${n(a.x2)} ${n(a.y2)}`;
        case 'polyline': case 'polygon': return `M${a.points}${tag === 'polygon' ? 'Z' : ''}`;
        case 'rect': {
            const x = +(a.x || 0), y = +(a.y || 0), w = +a.width, h = +a.height;
            const rx = Math.min(+(a.rx ?? a.ry ?? 0), w/2), ry = Math.min(+(a.ry ?? a.rx ?? 0), h/2);
            if (!rx || !ry) return `M${n(x)} ${n(y)}h${n(w)}v${n(h)}h${n(-w)}Z`;
            return `M${n(x+rx)} ${n(y)}H${n(x+w-rx)}a${n(rx)} ${n(ry)} 0 0 1 ${n(rx)} ${n(ry)}V${n(y+h-ry)}a${n(rx)} ${n(ry)} 0 0 1 ${n(-rx)} ${n(ry)}H${n(x+rx)}a${n(rx)} ${n(ry)} 0 0 1 ${n(-rx)} ${n(-ry)}V${n(y+ry)}a${n(rx)} ${n(ry)} 0 0 1 ${n(rx)} ${n(-ry)}Z`;
        }
        default: throw new Error(`Unsupported SVG primitive ${tag}`);
    }
}
const icons = names.map(name => {
    const filled = name.endsWith('Filled');
    const original = filled ? name.slice(0, -6) : name;
    if (!lucide[original]) throw new Error(`Missing web icon: ${original}`);
    const nodes = lucide[original].render({}, null).props.iconNode;
    for (const [tag, attrs] of nodes) {
        const allowed = new Set(['key', 'd', 'cx', 'cy', 'r', 'rx', 'ry', 'x', 'y', 'width', 'height', 'points', 'x1', 'x2', 'y1', 'y2']);
        for (const attr of Object.keys(attrs)) if (!allowed.has(attr)) throw new Error(`Review SVG attribute ${original}/${tag}/${attr}`);
    }
    return { name, filled, nodes };
});
const properties = icons.map(icon => `    val ${icon.name}: ImageVector by lazy {\n        lucideIcon(${JSON.stringify(icon.name)}, ${icon.filled},\n${icon.nodes.map(([tag, attrs]) => '            ' + JSON.stringify(geometry(tag, attrs))).join(',\n')}\n        )\n    }`).join('\n\n');
const kotlin = `package com.nur.quran.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Generated from the web app's lucide-react ${version} SVG nodes.
 * Run node scripts/sync-web-icons.cjs [web-repo] to update; do not hand-copy SVG shapes.
 * Original paths, centered circles, rounded rectangles, 24-unit viewport and 2-unit round strokes.
 * Filled variants match the web's fill="currentColor" with the same outline geometry/stroke.
 * License: assets/licenses/lucide-ISC.txt.
 */
object NurIcons {
${properties}
}

private fun lucideIcon(name: String, filled: Boolean, vararg pathData: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
        autoMirror = name in setOf("ArrowLeft", "ArrowRight", "ChevronLeft", "ChevronRight")
    )
    pathData.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}
`;
const outputs = new Map([
    [target, kotlin],
    [path.join(root, 'app/src/androidTest/assets/lucide-web-icons.json'), JSON.stringify({ library: 'lucide-react', version, icons }, null, 2) + '\n'],
    [path.join(root, 'app/src/main/assets/licenses/lucide-ISC.txt'), fs.readFileSync(path.join(library, 'LICENSE'), 'utf8')]
]);
for (const [file, content] of outputs) {
    if (args.includes('--check')) {
        if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== content) throw new Error(`Web icon drift: ${file}`);
    } else { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, content); }
}
console.log(`${args.includes('--check') ? 'Verified' : 'Generated'} ${icons.length} icons from lucide-react ${version}`);
