package com.nur.quran.data.api.rn

import com.nur.quran.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/** HTTPS origin shared by authentication, content, sync, and realtime on Heroku. */
@Singleton
class RnBackend @Inject constructor() {
    /** Test seam: forces the base URL independently of the build-configured default. */
    @androidx.annotation.VisibleForTesting
    internal var injectedBaseUrl: String? = null

    val configuredBaseUrl: String
        get() = injectedBaseUrl ?: BuildConfig.QURAN_NUR_API_BASE_URL

    /** Retrofit always wants a trailing slash on the base URL. */
    val baseUrl: String get() = configuredBaseUrl.trimEnd('/') + "/"

    companion object {
        /** Verse-level translation resource the RN contract pins (`translation=20`). */
        const val TRANSLATION_ID = 20
    }
}
