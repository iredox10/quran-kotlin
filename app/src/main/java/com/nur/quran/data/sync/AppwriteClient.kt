package com.nur.quran.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Account
import io.appwrite.services.Databases
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appwrite backend client (mirrors the quran-app web client in
 * `quran-app/src/services/appwrite.js`).
 *
 * Endpoint + project/database/collection IDs are public identifiers, not
 * secrets — copied from the web `.env` / setup-script defaults. No API keys
 * are used client-side.
 */
@Singleton
class AppwriteClient @Inject constructor(
    @ApplicationContext context: Context
) {
    val client: Client = Client(context)
        .setEndpoint(ENDPOINT)
        .setProject(PROJECT_ID)
        .setSelfSigned(false)

    val account = Account(client)
    val databases = Databases(client)

    /**
     * Query that resolves this account's sync document (web parity:
     * `appwrite.js` pushState/pullState both list with
     * `Query.equal("userId", userId)` and use `documents[0]`).
     *
     * The document id is server-generated (`unique()` on create), so it is never
     * derivable from the account id and must never be guessed.
     */
    fun userSyncQueries(userId: String): List<String> =
        listOf(Query.equal(SyncContract.FIELD_USER_ID, userId))

    /** Magic id the server replaces with a generated one (same value web sends). */
    val uniqueDocumentId: String = SyncContract.UNIQUE_DOCUMENT_ID

    companion object {
        const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
        const val PROJECT_ID = "69ac08e1000402826be5"

        // Defaults from quran-app/scripts/setup-appwrite*.js + web fallback in
        // services/appwrite.js (VITE_APPWRITE_DATABASE_ID /
        // VITE_APPWRITE_USER_DATA_COLLECTION_ID are absent from web .env,
        // so these fallbacks are the live IDs).
        // TODO: replace with BuildConfig fields if these IDs ever diverge.
        const val DATABASE_ID = "quran_db"
        const val USER_SYNC_COLLECTION_ID = "user_sync"
        const val SAUKA_GROUPS_COLLECTION_ID = "sauka_groups"
        const val SAUKA_ASSIGNMENTS_COLLECTION_ID = "sauka_assignments"
        const val SAUKA_COMMENTS_COLLECTION_ID = "sauka_comments"
        const val AUDIO_NOTES_BUCKET_ID = "audio_notes"
    }
}
