package com.nur.quran.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Lucide icons (lucide.dev, ISC license) ported to Compose ImageVectors.
 * Path data extracted verbatim from the web app's lucide-react dependency
 * so the Android UI renders the exact same iconography as the web app.
 */
object NurIcons {
    val Flame: ImageVector by lazy {
        lucideIcon("Flame",
        "M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4")
    }

    val Clock: ImageVector by lazy {
        lucideIcon("Clock",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "M12 6v6l4 2")
    }

    val BarChart3: ImageVector by lazy {
        lucideIcon("BarChart3",
        "M3 3v16a2 2 0 0 0 2 2h16",
        "M18 17V9",
        "M13 17V5",
        "M8 17v-3")
    }

    val Sparkles: ImageVector by lazy {
        lucideIcon("Sparkles",
        "M11.017 2.814a1 1 0 0 1 1.966 0l1.051 5.558a2 2 0 0 0 1.594 1.594l5.558 1.051a1 1 0 0 1 0 1.966l-5.558 1.051a2 2 0 0 0-1.594 1.594l-1.051 5.558a1 1 0 0 1-1.966 0l-1.051-5.558a2 2 0 0 0-1.594-1.594l-5.558-1.051a1 1 0 0 1 0-1.966l5.558-1.051a2 2 0 0 0 1.594-1.594z",
        "M20 2v4",
        "M22 4h-4",
        "M 2 20 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0")
    }

    val Share2: ImageVector by lazy {
        lucideIcon("Share2",
        "M 15 5 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 3 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 15 19 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 8.59 13.51 L 15.42 17.49",
        "M 15.41 6.51 L 8.59 10.49")
    }

    val Copy: ImageVector by lazy {
        lucideIcon("Copy",
        "M 10 8 H 20 A 2 2 0 0 1 22 10 V 20 A 2 2 0 0 1 20 22 H 10 A 2 2 0 0 1 8 20 V 10 A 2 2 0 0 1 10 8 Z",
        "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2")
    }

    val Check: ImageVector by lazy {
        lucideIcon("Check",
        "M20 6 9 17l-5-5")
    }

    val BookOpen: ImageVector by lazy {
        lucideIcon("BookOpen",
        "M12 7v14",
        "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z")
    }

    val ArrowRight: ImageVector by lazy {
        lucideIcon("ArrowRight",
        "M5 12h14",
        "m12 5 7 7-7 7")
    }

    val ArrowLeft: ImageVector by lazy {
        lucideIcon("ArrowLeft",
        "m12 19-7-7 7-7",
        "M19 12H5")
    }

    val Users: ImageVector by lazy {
        lucideIcon("Users",
        "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
        "M16 3.128a4 4 0 0 1 0 7.744",
        "M22 21v-2a4 4 0 0 0-3-3.87",
        "M 5 7 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0")
    }

    val Bookmark: ImageVector by lazy {
        lucideIcon("Bookmark",
        "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z")
    }

    val Search: ImageVector by lazy {
        lucideIcon("Search",
        "m21 21-4.34-4.34",
        "M 3 11 a 8 8 0 1 0 16 0 a 8 8 0 1 0 -16 0")
    }

    val Hash: ImageVector by lazy {
        lucideIcon("Hash",
        "M 4 9 L 20 9",
        "M 4 15 L 20 15",
        "M 10 3 L 8 21",
        "M 16 3 L 14 21")
    }

    val Rows3: ImageVector by lazy {
        lucideIcon("Rows3",
        "M 5 3 H 19 A 2 2 0 0 1 21 5 V 19 A 2 2 0 0 1 19 21 H 5 A 2 2 0 0 1 3 19 V 5 A 2 2 0 0 1 5 3 Z",
        "M21 9H3",
        "M21 15H3")
    }

    val LibraryBig: ImageVector by lazy {
        lucideIcon("LibraryBig",
        "M 4 3 H 10 A 1 1 0 0 1 11 4 V 20 A 1 1 0 0 1 10 21 H 4 A 1 1 0 0 1 3 20 V 4 A 1 1 0 0 1 4 3 Z",
        "M7 3v18",
        "M20.4 18.9c.2.5-.1 1.1-.6 1.3l-1.9.7c-.5.2-1.1-.1-1.3-.6L11.1 5.1c-.2-.5.1-1.1.6-1.3l1.9-.7c.5-.2 1.1.1 1.3.6Z")
    }

    val Layers3: ImageVector by lazy {
        lucideIcon("Layers3",
        "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z",
        "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12",
        "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17")
    }

    val Play: ImageVector by lazy {
        lucideIcon("Play",
        "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z")
    }

    /** Filled variant (web renders play/pause/bookmark with fill="currentColor"). */
    val PlayFilled: ImageVector by lazy {
        lucideIconFilled("PlayFilled",
        "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z")
    }

    val PauseFilled: ImageVector by lazy {
        lucideIconFilled("PauseFilled",
        "M 15 3 H 18 A 1 1 0 0 1 19 4 V 20 A 1 1 0 0 1 18 21 H 15 A 1 1 0 0 1 14 20 V 4 A 1 1 0 0 1 15 3 Z",
        "M 6 3 H 9 A 1 1 0 0 1 10 4 V 20 A 1 1 0 0 1 9 21 H 6 A 1 1 0 0 1 5 20 V 4 A 1 1 0 0 1 6 3 Z")
    }

    val BookmarkFilled: ImageVector by lazy {
        lucideIconFilled("BookmarkFilled",
        "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z")
    }

    val Pause: ImageVector by lazy {
        lucideIcon("Pause",
        "M 15 3 H 18 A 1 1 0 0 1 19 4 V 20 A 1 1 0 0 1 18 21 H 15 A 1 1 0 0 1 14 20 V 4 A 1 1 0 0 1 15 3 Z",
        "M 6 3 H 9 A 1 1 0 0 1 10 4 V 20 A 1 1 0 0 1 9 21 H 6 A 1 1 0 0 1 5 20 V 4 A 1 1 0 0 1 6 3 Z")
    }

    val Info: ImageVector by lazy {
        lucideIcon("Info",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "M12 16v-4",
        "M12 8h.01")
    }

    val X: ImageVector by lazy {
        lucideIcon("X",
        "M18 6 6 18",
        "m6 6 12 12")
    }

    val Download: ImageVector by lazy {
        lucideIcon("Download",
        "M12 15V3",
        "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4",
        "m7 10 5 5 5-5")
    }

    val Plus: ImageVector by lazy {
        lucideIcon("Plus",
        "M5 12h14",
        "M12 5v14")
    }

    val Highlighter: ImageVector by lazy {
        lucideIcon("Highlighter",
        "m9 11-6 6v3h9l3-3",
        "m22 12-4.6 4.6a2 2 0 0 1-2.8 0l-5.2-5.2a2 2 0 0 1 0-2.8L14 4")
    }

    val Type: ImageVector by lazy {
        lucideIcon("Type",
        "M12 4v16",
        "M4 7V5a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2",
        "M9 20h6")
    }

    val CheckCircle2: ImageVector by lazy {
        lucideIcon("CheckCircle2",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "m9 12 2 2 4-4")
    }

    val Loader2: ImageVector by lazy {
        lucideIcon("Loader2",
        "M21 12a9 9 0 1 1-6.219-8.56")
    }

    val Brain: ImageVector by lazy {
        lucideIcon("Brain",
        "M12 5a3 3 0 1 0-5.997.125 4 4 0 0 0-2.526 5.77 4 4 0 0 0 .556 6.588A4 4 0 1 0 12 18Z",
        "M12 5a3 3 0 1 1 5.997.125 4 4 0 0 1 2.526 5.77 4 4 0 0 1-.556 6.588A4 4 0 1 1 12 18Z",
        "M12 5v14")
    }

    val Eye: ImageVector by lazy {
        lucideIcon("Eye",
        "M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z",
        "M 9 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0")
    }

    val EyeOff: ImageVector by lazy {
        lucideIcon("EyeOff",
        "M9.88 9.88a3 3 0 1 0 4.24 4.24",
        "M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68",
        "M6.61 6.61A13.52 13.52 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61",
        "L2 2l20 20")
    }

    val Minus: ImageVector by lazy {
        lucideIcon("Minus",
        "M5 12h14")
    }

    val ChevronDown: ImageVector by lazy {
        lucideIcon("ChevronDown",
        "m6 9 6 6 6-6")
    }

    val ChevronsDown: ImageVector by lazy {
        lucideIcon("ChevronsDown",
        "m7 6 5 5 5-5",
        "m7 13 5 5 5-5")
    }

    val Volume2: ImageVector by lazy {
        lucideIcon("Volume2",
        "M 11 5 L 6 9 H 2 V 15 H 6 L 11 19 V 5 Z",
        "M 15.54 8.46 a 5 5 0 0 1 0 7.07",
        "M 19.07 4.93 a 10 10 0 0 1 0 14.14")
    }

    val Moon: ImageVector by lazy {
        lucideIcon("Moon",
        "M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z")
    }

    val Sun: ImageVector by lazy {
        lucideIcon("Sun",
        "M12 2v2",
        "M12 20v2",
        "M4.93 4.93l1.41 1.41",
        "M17.66 17.66l1.41 1.41",
        "M2 12h2",
        "M20 12h2",
        "M6.34 17.66l-1.41 1.41",
        "M19.07 4.93l-1.41 1.41",
        "M 12 12 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0")
    }

    val CalendarDays: ImageVector by lazy {
        lucideIcon("CalendarDays",
        "M8 2v4",
        "M16 2v4",
        "M3 10h18",
        "M8 14h.01",
        "M12 14h.01",
        "M16 14h.01",
        "M8 18h.01",
        "M12 18h.01",
        "M16 18h.01",
        "M 5 4 H 19 A 2 2 0 0 1 21 6 V 20 A 2 2 0 0 1 19 22 H 5 A 2 2 0 0 1 3 20 V 6 A 2 2 0 0 1 5 4 Z")
    }

    val User: ImageVector by lazy {
        lucideIcon("User",
        "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
        "M 8 7 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0")
    }

    val TrendingUp: ImageVector by lazy {
        lucideIcon("TrendingUp",
        "M22 7 13.5 15.5 8.5 10.5 2 17",
        "M16 7h6v6")
    }
}

private fun lucideIcon(name: String, vararg pathData: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    pathData.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}

private fun lucideIconFilled(name: String, vararg pathData: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    pathData.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}
