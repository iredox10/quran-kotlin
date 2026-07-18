package com.nur.quran.data

/**
 * Static Quran navigation data, ported verbatim from the web app's
 * src/data/quranNavigation.js so browsing by Juz/Hizb/Page matches exactly.
 */
data class DivisionStart(val id: Int, val verseKey: String, val pageNumber: Int)

val JUZ_STARTS = listOf(
    DivisionStart(1, "1:1", 1),
    DivisionStart(2, "2:142", 22),
    DivisionStart(3, "2:253", 42),
    DivisionStart(4, "3:93", 62),
    DivisionStart(5, "4:24", 82),
    DivisionStart(6, "4:148", 102),
    DivisionStart(7, "5:82", 121),
    DivisionStart(8, "6:111", 142),
    DivisionStart(9, "7:88", 162),
    DivisionStart(10, "8:41", 182),
    DivisionStart(11, "9:93", 201),
    DivisionStart(12, "11:6", 222),
    DivisionStart(13, "12:53", 242),
    DivisionStart(14, "15:1", 262),
    DivisionStart(15, "17:1", 282),
    DivisionStart(16, "18:75", 302),
    DivisionStart(17, "21:1", 322),
    DivisionStart(18, "23:1", 342),
    DivisionStart(19, "25:21", 362),
    DivisionStart(20, "27:56", 382),
    DivisionStart(21, "29:46", 402),
    DivisionStart(22, "33:31", 422),
    DivisionStart(23, "36:28", 442),
    DivisionStart(24, "39:32", 462),
    DivisionStart(25, "41:47", 482),
    DivisionStart(26, "46:1", 502),
    DivisionStart(27, "51:31", 522),
    DivisionStart(28, "58:1", 542),
    DivisionStart(29, "67:1", 562),
    DivisionStart(30, "78:1", 582)
)

val HIZB_STARTS = listOf(
    DivisionStart(1, "1:1", 1),
    DivisionStart(2, "2:75", 11),
    DivisionStart(3, "2:142", 22),
    DivisionStart(4, "2:203", 32),
    DivisionStart(5, "2:253", 42),
    DivisionStart(6, "3:15", 51),
    DivisionStart(7, "3:93", 62),
    DivisionStart(8, "3:171", 72),
    DivisionStart(9, "4:24", 82),
    DivisionStart(10, "4:88", 92),
    DivisionStart(11, "4:148", 102),
    DivisionStart(12, "5:27", 112),
    DivisionStart(13, "5:82", 121),
    DivisionStart(14, "6:36", 132),
    DivisionStart(15, "6:111", 142),
    DivisionStart(16, "7:1", 151),
    DivisionStart(17, "7:88", 162),
    DivisionStart(18, "7:171", 173),
    DivisionStart(19, "8:41", 182),
    DivisionStart(20, "9:34", 192),
    DivisionStart(21, "9:93", 201),
    DivisionStart(22, "10:26", 212),
    DivisionStart(23, "11:6", 222),
    DivisionStart(24, "11:84", 231),
    DivisionStart(25, "12:53", 242),
    DivisionStart(26, "13:19", 252),
    DivisionStart(27, "15:1", 262),
    DivisionStart(28, "16:51", 272),
    DivisionStart(29, "17:1", 282),
    DivisionStart(30, "17:99", 292),
    DivisionStart(31, "18:75", 302),
    DivisionStart(32, "20:1", 312),
    DivisionStart(33, "21:1", 322),
    DivisionStart(34, "22:1", 332),
    DivisionStart(35, "23:1", 342),
    DivisionStart(36, "24:21", 352),
    DivisionStart(37, "25:21", 362),
    DivisionStart(38, "26:111", 371),
    DivisionStart(39, "27:56", 382),
    DivisionStart(40, "28:51", 392),
    DivisionStart(41, "29:46", 402),
    DivisionStart(42, "31:22", 413),
    DivisionStart(43, "33:31", 422),
    DivisionStart(44, "34:24", 431),
    DivisionStart(45, "36:28", 442),
    DivisionStart(46, "37:145", 451),
    DivisionStart(47, "39:32", 462),
    DivisionStart(48, "40:41", 472),
    DivisionStart(49, "41:47", 482),
    DivisionStart(50, "43:24", 491),
    DivisionStart(51, "46:1", 502),
    DivisionStart(52, "48:18", 513),
    DivisionStart(53, "51:31", 522),
    DivisionStart(54, "55:1", 531),
    DivisionStart(55, "58:1", 542),
    DivisionStart(56, "62:1", 553),
    DivisionStart(57, "67:1", 562),
    DivisionStart(58, "72:1", 572),
    DivisionStart(59, "78:1", 582),
    DivisionStart(60, "87:1", 591)
)

/** Total number of pages in the Madani mushaf. */
const val MUSHAF_PAGE_COUNT = 604

fun getJuzByPage(page: Int): DivisionStart =
    JUZ_STARTS.lastOrNull { page >= it.pageNumber } ?: JUZ_STARTS.first()

fun getHizbByPage(page: Int): DivisionStart =
    HIZB_STARTS.lastOrNull { page >= it.pageNumber } ?: HIZB_STARTS.first()
