package com.nur.quran.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Generated from the web app's lucide-react 0.577.0 SVG nodes.
 * Run node scripts/sync-web-icons.cjs [web-repo] to update; do not hand-copy SVG shapes.
 * Original paths, centered circles, rounded rectangles, 24-unit viewport and 2-unit round strokes.
 * Filled variants match the web's fill="currentColor" with the same outline geometry/stroke.
 * License: assets/licenses/lucide-ISC.txt.
 */
object NurIcons {
    val Flame: ImageVector by lazy {
        lucideIcon("Flame", false,
            "M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4"
        )
    }

    val Clock: ImageVector by lazy {
        lucideIcon("Clock", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "M12 6v6l4 2"
        )
    }

    val Lock: ImageVector by lazy {
        lucideIcon("Lock", false,
            "M5 11H19a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V13a2 2 0 0 1 2 -2Z",
            "M7 11V7a5 5 0 0 1 10 0v4"
        )
    }

    val RotateCcw: ImageVector by lazy {
        lucideIcon("RotateCcw", false,
            "M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8",
            "M3 3v5h5"
        )
    }

    val BarChart3: ImageVector by lazy {
        lucideIcon("BarChart3", false,
            "M3 3v16a2 2 0 0 0 2 2h16",
            "M18 17V9",
            "M13 17V5",
            "M8 17v-3"
        )
    }

    val Sparkles: ImageVector by lazy {
        lucideIcon("Sparkles", false,
            "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
            "M20 2v4",
            "M22 4h-4",
            "M2 20a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z"
        )
    }

    val Share2: ImageVector by lazy {
        lucideIcon("Share2", false,
            "M15 5a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M3 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M15 19a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M8.59 13.51L15.42 17.49",
            "M15.41 6.51L8.59 10.49"
        )
    }

    val Copy: ImageVector by lazy {
        lucideIcon("Copy", false,
            "M10 8H20a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H10a2 2 0 0 1 -2 -2V10a2 2 0 0 1 2 -2Z",
            "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
        )
    }

    val Check: ImageVector by lazy {
        lucideIcon("Check", false,
            "M20 6 9 17l-5-5"
        )
    }

    val BookOpen: ImageVector by lazy {
        lucideIcon("BookOpen", false,
            "M12 7v14",
            "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"
        )
    }

    val ArrowRight: ImageVector by lazy {
        lucideIcon("ArrowRight", false,
            "M5 12h14",
            "m12 5 7 7-7 7"
        )
    }

    val ArrowLeft: ImageVector by lazy {
        lucideIcon("ArrowLeft", false,
            "m12 19-7-7 7-7",
            "M19 12H5"
        )
    }

    val ChevronRight: ImageVector by lazy {
        lucideIcon("ChevronRight", false,
            "m9 18 6-6-6-6"
        )
    }

    val ChevronLeft: ImageVector by lazy {
        lucideIcon("ChevronLeft", false,
            "m15 18-6-6 6-6"
        )
    }

    val Users: ImageVector by lazy {
        lucideIcon("Users", false,
            "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
            "M16 3.128a4 4 0 0 1 0 7.744",
            "M22 21v-2a4 4 0 0 0-3-3.87",
            "M5 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z"
        )
    }

    val Bookmark: ImageVector by lazy {
        lucideIcon("Bookmark", false,
            "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z"
        )
    }

    val Search: ImageVector by lazy {
        lucideIcon("Search", false,
            "m21 21-4.34-4.34",
            "M3 11a8 8 0 1 0 16 0a8 8 0 1 0 -16 0Z"
        )
    }

    val Hash: ImageVector by lazy {
        lucideIcon("Hash", false,
            "M4 9L20 9",
            "M4 15L20 15",
            "M10 3L8 21",
            "M16 3L14 21"
        )
    }

    val Rows3: ImageVector by lazy {
        lucideIcon("Rows3", false,
            "M5 3H19a2 2 0 0 1 2 2V19a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2Z",
            "M21 9H3",
            "M21 15H3"
        )
    }

    val LibraryBig: ImageVector by lazy {
        lucideIcon("LibraryBig", false,
            "M4 3H10a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H4a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z",
            "M7 3v18",
            "M20.4 18.9c.2.5-.1 1.1-.6 1.3l-1.9.7c-.5.2-1.1-.1-1.3-.6L11.1 5.1c-.2-.5.1-1.1.6-1.3l1.9-.7c.5-.2 1.1.1 1.3.6Z"
        )
    }

    val Layers3: ImageVector by lazy {
        lucideIcon("Layers3", false,
            "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z",
            "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12",
            "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17"
        )
    }

    val Play: ImageVector by lazy {
        lucideIcon("Play", false,
            "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"
        )
    }

    val PlayFilled: ImageVector by lazy {
        lucideIcon("PlayFilled", true,
            "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"
        )
    }

    val PauseFilled: ImageVector by lazy {
        lucideIcon("PauseFilled", true,
            "M15 3H18a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H15a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z",
            "M6 3H9a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H6a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z"
        )
    }

    val BookmarkFilled: ImageVector by lazy {
        lucideIcon("BookmarkFilled", true,
            "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z"
        )
    }

    val Pause: ImageVector by lazy {
        lucideIcon("Pause", false,
            "M15 3H18a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H15a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z",
            "M6 3H9a1 1 0 0 1 1 1V20a1 1 0 0 1 -1 1H6a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z"
        )
    }

    val Info: ImageVector by lazy {
        lucideIcon("Info", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "M12 16v-4",
            "M12 8h.01"
        )
    }

    val X: ImageVector by lazy {
        lucideIcon("X", false,
            "M18 6 6 18",
            "m6 6 12 12"
        )
    }

    val Download: ImageVector by lazy {
        lucideIcon("Download", false,
            "M12 15V3",
            "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
            "m7 10 5 5 5-5"
        )
    }

    val Plus: ImageVector by lazy {
        lucideIcon("Plus", false,
            "M5 12h14",
            "M12 5v14"
        )
    }

    val Highlighter: ImageVector by lazy {
        lucideIcon("Highlighter", false,
            "m9 11-6 6v3h9l3-3",
            "m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4"
        )
    }

    val Type: ImageVector by lazy {
        lucideIcon("Type", false,
            "M12 4v16",
            "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2",
            "M9 20h6"
        )
    }

    val CheckCircle2: ImageVector by lazy {
        lucideIcon("CheckCircle2", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "m9 12 2 2 4-4"
        )
    }

    val XCircle: ImageVector by lazy {
        lucideIcon("XCircle", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "m15 9-6 6",
            "m9 9 6 6"
        )
    }

    val RefreshCw: ImageVector by lazy {
        lucideIcon("RefreshCw", false,
            "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
            "M21 3v5h-5",
            "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
            "M8 16H3v5"
        )
    }

    val Loader2: ImageVector by lazy {
        lucideIcon("Loader2", false,
            "M21 12a9 9 0 1 1-6.219-8.56"
        )
    }

    val Brain: ImageVector by lazy {
        lucideIcon("Brain", false,
            "M12 18V5",
            "M15 13a4.17 4.17 0 0 1-3-4 4.17 4.17 0 0 1-3 4",
            "M17.598 6.5A3 3 0 1 0 12 5a3 3 0 1 0-5.598 1.5",
            "M17.997 5.125a4 4 0 0 1 2.526 5.77",
            "M18 18a4 4 0 0 0 2-7.464",
            "M19.967 17.483A4 4 0 1 1 12 18a4 4 0 1 1-7.967-.517",
            "M6 18a4 4 0 0 1-2-7.464",
            "M6.003 5.125a4 4 0 0 0-2.526 5.77"
        )
    }

    val Eye: ImageVector by lazy {
        lucideIcon("Eye", false,
            "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0",
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"
        )
    }

    val EyeOff: ImageVector by lazy {
        lucideIcon("EyeOff", false,
            "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49",
            "M14.084 14.158a3 3 0 0 1-4.242-4.242",
            "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143",
            "m2 2 20 20"
        )
    }

    val Minus: ImageVector by lazy {
        lucideIcon("Minus", false,
            "M5 12h14"
        )
    }

    val ChevronDown: ImageVector by lazy {
        lucideIcon("ChevronDown", false,
            "m6 9 6 6 6-6"
        )
    }

    val ChevronUp: ImageVector by lazy {
        lucideIcon("ChevronUp", false,
            "m18 15-6-6-6 6"
        )
    }

    val Repeat: ImageVector by lazy {
        lucideIcon("Repeat", false,
            "m17 2 4 4-4 4",
            "M3 11v-1a4 4 0 0 1 4-4h14",
            "m7 22-4-4 4-4",
            "M21 13v1a4 4 0 0 1-4 4H3"
        )
    }

    val ChevronsDown: ImageVector by lazy {
        lucideIcon("ChevronsDown", false,
            "m7 6 5 5 5-5",
            "m7 13 5 5 5-5"
        )
    }

    val Volume2: ImageVector by lazy {
        lucideIcon("Volume2", false,
            "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z",
            "M16 9a5 5 0 0 1 0 6",
            "M19.364 18.364a9 9 0 0 0 0-12.728"
        )
    }

    val Moon: ImageVector by lazy {
        lucideIcon("Moon", false,
            "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"
        )
    }

    val Sun: ImageVector by lazy {
        lucideIcon("Sun", false,
            "M8 12a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z",
            "M12 2v2",
            "M12 20v2",
            "m4.93 4.93 1.41 1.41",
            "m17.66 17.66 1.41 1.41",
            "M2 12h2",
            "M20 12h2",
            "m6.34 17.66-1.41 1.41",
            "m19.07 4.93-1.41 1.41"
        )
    }

    val CalendarDays: ImageVector by lazy {
        lucideIcon("CalendarDays", false,
            "M8 2v4",
            "M16 2v4",
            "M5 4H19a2 2 0 0 1 2 2V20a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V6a2 2 0 0 1 2 -2Z",
            "M3 10h18",
            "M8 14h.01",
            "M12 14h.01",
            "M16 14h.01",
            "M8 18h.01",
            "M12 18h.01",
            "M16 18h.01"
        )
    }

    val User: ImageVector by lazy {
        lucideIcon("User", false,
            "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
            "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z"
        )
    }

    val TrendingUp: ImageVector by lazy {
        lucideIcon("TrendingUp", false,
            "M16 7h6v6",
            "m22 7-8.5 8.5-5-5L2 17"
        )
    }

    val Award: ImageVector by lazy {
        lucideIcon("Award", false,
            "m15.477 12.89 1.515 8.526a.5.5 0 0 1-.81.47l-3.58-2.687a1 1 0 0 0-1.197 0l-3.586 2.686a.5.5 0 0 1-.81-.469l1.514-8.526",
            "M6 8a6 6 0 1 0 12 0a6 6 0 1 0 -12 0Z"
        )
    }

    val Settings: ImageVector by lazy {
        lucideIcon("Settings", false,
            "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915",
            "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"
        )
    }

    val AlertTriangle: ImageVector by lazy {
        lucideIcon("AlertTriangle", false,
            "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3",
            "M12 9v4",
            "M12 17h.01"
        )
    }

    val Square: ImageVector by lazy {
        lucideIcon("Square", false,
            "M5 3H19a2 2 0 0 1 2 2V19a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2Z"
        )
    }

    val Target: ImageVector by lazy {
        lucideIcon("Target", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z",
            "M6 12a6 6 0 1 0 12 0a6 6 0 1 0 -12 0Z",
            "M10 12a2 2 0 1 0 4 0a2 2 0 1 0 -4 0Z"
        )
    }

    val Lightbulb: ImageVector by lazy {
        lucideIcon("Lightbulb", false,
            "M15 14c.2-1 .7-1.7 1.5-2.5 1-.9 1.5-2.2 1.5-3.5A6 6 0 0 0 6 8c0 1 .2 2.2 1.5 3.5.7.7 1.3 1.5 1.5 2.5",
            "M9 18h6",
            "M10 22h4"
        )
    }

    val Folder: ImageVector by lazy {
        lucideIcon("Folder", false,
            "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"
        )
    }

    val Trash2: ImageVector by lazy {
        lucideIcon("Trash2", false,
            "M10 11v6",
            "M14 11v6",
            "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6",
            "M3 6h18",
            "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
        )
    }

    val Mic: ImageVector by lazy {
        lucideIcon("Mic", false,
            "M12 19v3",
            "M19 10v2a7 7 0 0 1-14 0v-2",
            "M12 2H12a3 3 0 0 1 3 3V12a3 3 0 0 1 -3 3H12a3 3 0 0 1 -3 -3V5a3 3 0 0 1 3 -3Z"
        )
    }

    val Languages: ImageVector by lazy {
        lucideIcon("Languages", false,
            "m5 8 6 6",
            "m4 14 6-6 2-3",
            "M2 5h12",
            "M7 2h1",
            "m22 22-5-10-5 10",
            "M14 18h6"
        )
    }

    val HardDrive: ImageVector by lazy {
        lucideIcon("HardDrive", false,
            "M10 16h.01",
            "M2.212 11.577a2 2 0 0 0-.212.896V18a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-5.527a2 2 0 0 0-.212-.896L18.55 5.11A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z",
            "M21.946 12.013H2.054",
            "M6 16h.01"
        )
    }

    val CloudUpload: ImageVector by lazy {
        lucideIcon("CloudUpload", false,
            "M12 13v8",
            "M4 14.899A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.5 8.242",
            "m8 17 4-4 4 4"
        )
    }

    val CloudDownload: ImageVector by lazy {
        lucideIcon("CloudDownload", false,
            "M12 13v8l-4-4",
            "m12 21 4-4",
            "M4.393 15.269A7 7 0 1 1 15.71 8h1.79a4.5 4.5 0 0 1 2.436 8.284"
        )
    }

    val LogOut: ImageVector by lazy {
        lucideIcon("LogOut", false,
            "m16 17 5-5-5-5",
            "M21 12H9",
            "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"
        )
    }

    val Circle: ImageVector by lazy {
        lucideIcon("Circle", false,
            "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0Z"
        )
    }

    val MoreVertical: ImageVector by lazy {
        lucideIcon("MoreVertical", false,
            "M11 12a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M11 5a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z",
            "M11 19a1 1 0 1 0 2 0a1 1 0 1 0 -2 0Z"
        )
    }

    val Sliders: ImageVector by lazy {
        lucideIcon("Sliders", false,
            "M10 8h4",
            "M12 21v-9",
            "M12 8V3",
            "M17 16h4",
            "M19 12V3",
            "M19 21v-5",
            "M3 14h4",
            "M5 10V3",
            "M5 21v-7"
        )
    }

    val Archive: ImageVector by lazy {
        lucideIcon("Archive", false,
            "M3 3H21a1 1 0 0 1 1 1V7a1 1 0 0 1 -1 1H3a1 1 0 0 1 -1 -1V4a1 1 0 0 1 1 -1Z",
            "M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8",
            "M10 12h4"
        )
    }

    val Gem: ImageVector by lazy {
        lucideIcon("Gem", false,
            "M10.5 3 8 9l4 13 4-13-2.5-6",
            "M17 3a2 2 0 0 1 1.6.8l3 4a2 2 0 0 1 .013 2.382l-7.99 10.986a2 2 0 0 1-3.247 0l-7.99-10.986A2 2 0 0 1 2.4 7.8l2.998-3.997A2 2 0 0 1 7 3z",
            "M2 9h20"
        )
    }

    val Settings2: ImageVector by lazy {
        lucideIcon("Settings2", false,
            "M14 17H5",
            "M19 7h-9",
            "M14 17a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z",
            "M4 7a3 3 0 1 0 6 0a3 3 0 1 0 -6 0Z"
        )
    }

    val SkipBack: ImageVector by lazy {
        lucideIcon("SkipBack", false,
            "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z",
            "M3 20V4"
        )
    }

    val SkipForward: ImageVector by lazy {
        lucideIcon("SkipForward", false,
            "M21 4v16",
            "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z"
        )
    }

    val SquareFilled: ImageVector by lazy {
        lucideIcon("SquareFilled", true,
            "M5 3H19a2 2 0 0 1 2 2V19a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V5a2 2 0 0 1 2 -2Z"
        )
    }
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
