package com.yogeshpaliyal.gapwise.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Backs up to the user's Google Drive "app data" folder: a hidden folder only
 * Gapwise can read, which doesn't clutter their Drive. One file, replaced on
 * every backup.
 *
 * Uses Google Identity Services for consent and the Drive REST API directly,
 * so there is no heavy client library.
 */
object DriveBackup {
    private const val FILE_NAME = "gapwise-backup.json"
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val API = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"

    sealed interface Auth {
        data class Granted(val token: String) : Auth

        /** The user has to approve Drive access first; launch this, then call [tokenFrom]. */
        data class NeedsConsent(val intent: PendingIntent) : Auth
    }

    data class RemoteBackup(val id: String, val modifiedAt: Long, val sizeBytes: Long)

    class DriveException(message: String, val code: Int) : IOException(message)

    private val json = Json { ignoreUnknownKeys = true }

    private fun request() = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(SCOPE)))
        .build()

    /** Gets a Drive token, or the consent screen to show if access isn't granted yet. */
    suspend fun authorize(context: Context): Auth = suspendCancellableCoroutine { cont ->
        Identity.getAuthorizationClient(context)
            .authorize(request())
            .addOnSuccessListener { r ->
                val pending = r.pendingIntent
                val token = r.accessToken
                when {
                    r.hasResolution() && pending != null -> cont.resume(Auth.NeedsConsent(pending))
                    token != null -> cont.resume(Auth.Granted(token))
                    else -> cont.resumeWithException(IOException("Google didn't return Drive access"))
                }
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Reads the token from the consent screen's result. Null if the user declined. */
    fun tokenFrom(context: Context, data: Intent?): String? =
        runCatching { Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken }.getOrNull()

    /** The current backup in Drive, or null if there isn't one yet. */
    suspend fun latest(token: String): RemoteBackup? = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode("name = '$FILE_NAME' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$q&orderBy=modifiedTime%20desc&fields=files(id,modifiedTime,size)"
        val body = call("GET", url, token)
        val files = json.parseToJsonElement(body).jsonObject["files"]?.jsonArray.orEmpty()
        files.firstOrNull()?.jsonObject?.toRemote()
    }

    /** Uploads [content], replacing the previous backup if there is one. */
    suspend fun upload(token: String, content: String): RemoteBackup = withContext(Dispatchers.IO) {
        val existing = latest(token)
        val fields = "fields=id,modifiedTime,size"
        val body = if (existing != null) {
            // HttpURLConnection can't send PATCH; Google APIs accept the override header.
            call(
                "POST", "$UPLOAD/files/${existing.id}?uploadType=media&$fields", token,
                contentType = "application/json; charset=UTF-8",
                payload = content.toByteArray(),
                headers = mapOf("X-HTTP-Method-Override" to "PATCH"),
            )
        } else {
            val boundary = "gapwise-${System.nanoTime()}"
            val meta = """{"name":"$FILE_NAME","parents":["appDataFolder"],"mimeType":"application/json"}"""
            val multipart = buildString {
                append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n")
                append("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$content\r\n")
                append("--$boundary--\r\n")
            }
            call(
                "POST", "$UPLOAD/files?uploadType=multipart&$fields", token,
                contentType = "multipart/related; boundary=$boundary",
                payload = multipart.toByteArray(),
            )
        }
        json.parseToJsonElement(body).jsonObject.toRemote()
    }

    suspend fun download(token: String, id: String): String = withContext(Dispatchers.IO) {
        call("GET", "$API/files/$id?alt=media", token)
    }

    private fun JsonObject.toRemote() = RemoteBackup(
        id = getValue("id").jsonPrimitive.content,
        modifiedAt = this["modifiedTime"]?.jsonPrimitive?.content?.let { Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis(),
        sizeBytes = this["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
    )

    private fun call(
        method: String,
        url: String,
        token: String,
        contentType: String? = null,
        payload: ByteArray? = null,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 20_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Authorization", "Bearer $token")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (payload != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(payload.size)
                conn.outputStream.use { it.write(payload) }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw DriveException("Drive returned $code ${err.take(200)}", code)
            }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
