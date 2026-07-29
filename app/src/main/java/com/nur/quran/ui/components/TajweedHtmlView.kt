package com.nur.quran.ui.components

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

fun getArabicFontFileName(name: String): String {
    return when (name.trim().lowercase()) {
        "kfgqpc-hafs", "kfgqpc hafs" -> "kfgqpc_hafs.ttf"
        "uthman-taha-naskh", "uthman taha naskh" -> "uthman_taha_naskh.ttf"
        "amiri-quran", "amiri quran" -> "amiri_regular.ttf"
        "noto-naskh-arabic", "noto naskh arabic" -> "noto_regular.ttf"
        "scheherazade-new", "scheherazade new" -> "scheherazade_regular.ttf"
        else -> "scheherazade_regular.ttf"
    }
}

@Composable
fun TajweedHtmlView(
    tajweedHtml: String,
    fontSizeSp: Float = 26f,
    fontFamilyName: String = "CustomArabicFont",
    fontFileName: String = "scheherazade_regular.ttf",
    textColorHex: String = "#2B3F3C",
    backgroundColorHex: String = "transparent",
    modifier: Modifier = Modifier
) {
    val cleanHtml = remember(tajweedHtml) {
        tajweedHtml.replace("[\u06df\u06e0\u06e2\u06ea-\u06ec\u25cc]".toRegex(), "")
    }

    val pageData = remember(cleanHtml, fontSizeSp, fontFamilyName, fontFileName, textColorHex, backgroundColorHex) {
        """
        <!DOCTYPE html>
        <html dir="rtl">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
        @font-face {
          font-family: '$fontFamilyName';
          src: url('file:///android_asset/fonts/$fontFileName');
        }
        * {
          -webkit-tap-highlight-color: transparent;
        }
        body {
          margin: 0;
          padding: 2px 0;
          background-color: $backgroundColorHex;
          color: $textColorHex;
          font-family: '$fontFamilyName', serif;
          font-size: ${fontSizeSp.toInt()}px;
          line-height: 1.8;
          text-align: right;
          direction: rtl;
          word-spacing: 2px;
          -webkit-user-select: none;
          user-select: none;
        }
        tajweed.ham_wasl, rule.ham_wasl, span.ham_wasl { color: #AAAAAA; }
        tajweed.laam_shamsiyah, rule.laam_shamsiyah, span.laam_shamsiyah { color: #AAAAAA; }
        tajweed.madda_normal, rule.madda_normal, span.madda_normal { color: #537FFF; }
        tajweed.madda_permissible, rule.madda_permissible, span.madda_permissible { color: #4050FF; }
        tajweed.madda_obligatory, rule.madda_obligatory, span.madda_obligatory { color: #000FB5; }
        tajweed.madda_necessary, rule.madda_necessary, span.madda_necessary { color: #2142c7; }
        tajweed.qalaqah, rule.qalaqah, span.qalaqah { color: #DD0008; }
        tajweed.qalpiala, rule.qalpiala, span.qalpiala { color: #DD0008; }
        tajweed.ikhafa_shafawi, rule.ikhafa_shafawi, span.ikhafa_shafawi { color: #D500B7; }
        tajweed.ikhfa_shafawi, rule.ikhfa_shafawi, span.ikhfa_shafawi { color: #D500B7; }
        tajweed.ikhafa, rule.ikhafa, span.ikhafa { color: #26BFFD; }
        tajweed.ikhfa, rule.ikhfa, span.ikhfa { color: #26BFFD; }
        tajweed.idgham_shafawi, rule.idgham_shafawi, span.idgham_shafawi { color: #169777; }
        tajweed.idghaam_shafawi, rule.idghaam_shafawi, span.idghaam_shafawi { color: #169777; }
        tajweed.idgham_ghunnah, rule.idgham_ghunnah, span.idgham_ghunnah { color: #169200; }
        tajweed.idghaam_ghunnah, rule.idghaam_ghunnah, span.idghaam_ghunnah { color: #169200; }
        tajweed.idgham_wo_ghunnah, rule.idgham_wo_ghunnah, span.idgham_wo_ghunnah { color: #169200; }
        tajweed.idghaam_no_ghunnah, rule.idghaam_no_ghunnah, span.idghaam_no_ghunnah { color: #169200; }
        tajweed.iqlab, rule.iqlab, span.iqlab { color: #26BFFD; }
        tajweed.ghunnah, rule.ghunnah, span.ghunnah { color: #FF7E1E; }
        tajweed.slnt, rule.slnt, span.slnt, tajweed.silent, rule.silent, span.silent { color: #AAAAAA; }
        .end { color: #CBA135; font-weight: normal; margin: 0 4px; }
        </style>
        </head>
        <body>$cleanHtml</body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = false
                    allowFileAccess = true
                    allowContentAccess = true
                }
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                pageData,
                "text/html",
                "utf-8",
                null
            )
        }
    )
}
