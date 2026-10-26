package com.nur.quran.data.api.rn

import com.nur.quran.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Render.com backend configuration (quran-nur-api) — the shared home of the
 * migration switch. [BuildConfig.IS_RENDER_API_ENABLED] is a build-time flag
 * that defaults OFF: when it is false, every consumer (sync, realtime, auth,
 * word packs, verses refresh) keeps using Appwrite / api.quran.com exactly as
 * before (verbatim code paths, no behavior change).
 *
 * There is no user-facing runtime toggle for backend switches in the app
 * today, so the flag is BuildConfig-only by design.
 *
 * Endpoint paths + shapes: `quran-backend/docs/contract-content.md` and
 * `docs/contract-auth-sync.md` (auth/sync surface) — sync DOCUMENT field
 * parity comes from the app's own [com.nur.quran.data.sync.SyncContract].
 */
@Singleton
class RnBackend @Inject constructor() {
    /** Test seam: forces the flag independently of the build-configured default. */
    @androidx.annotation.VisibleForTesting
    internal var injectedEnabled: Boolean? = null

    /** Test seam: forces the base URL independently of the build-configured default. */
    @androidx.annotation.VisibleForTesting
    internal var injectedBaseUrl: String? = null

    val isRenderApiEnabled: Boolean
        get() = injectedEnabled ?: BuildConfig.IS_RENDER_API_ENABLED

    val configuredBaseUrl: String
        get() = injectedBaseUrl ?: BuildConfig.QURAN_NUR_API_BASE_URL

    /** Retrofit always wants a trailing slash on the base URL. */
    val baseUrl: String get() = configuredBaseUrl.trimEnd('/') + "/"

    companion object {
        /** Verse-level translation resource the RN contract pins (`translation=20`). */
        const val TRANSLATION_ID = 20
    }
}
