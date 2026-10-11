package com.Lia.assistant.license

import android.content.Context
import android.provider.Settings
import com.Lia.assistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Access-key gate for the direct build. Keeps the key and the last known state on this phone.
 * Re-checks the key with the backend at most every 3 hours. A network error keeps the last known
 * state (fails open), so a flaky connection never locks out a valid user.
 */
object AccessKeyManager {
    private const val PREFS = "lia_access_key"
    private const val RECHECK_MS = 3L * 60 * 60 * 1000
    private const val K_KEY = "key"
    private const val K_PLAN = "plan"
    private const val K_EXPIRES = "expires_at"
    private const val K_BLOCKED = "blocked"
    private const val K_LAST = "last_remote_check"

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _info = MutableStateFlow(
        LicenseInfo(false, "", null, "Checking access key...", false, false)
    )
    val info: StateFlow<LicenseInfo> = _info

    sealed class Activation {
        data class Ok(val plan: String) : Activation()
        object Invalid : Activation()
        object Blocked : Activation()
        object Unreachable : Activation()
        object NotConfigured : Activation()
    }

    private sealed class Remote {
        data class Valid(val plan: String, val expiresAt: Long?, val blocked: Boolean) : Remote()
        object Revoked : Remote()
        object Unreachable : Remote()
    }

    private fun configured(): Boolean = BuildConfig.LICENSE_BASE_URL.isNotBlank()

    fun isActive(): Boolean = _info.value.active

    fun refusalMessage(): String = _info.value.statusText

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Loads the local state into [info]. No network. */
    fun refreshInfo(ctx: Context) {
        val sp = prefs(ctx)
        val key = sp.getString(K_KEY, null)
        val expires = sp.getLong(K_EXPIRES, 0L).takeIf { it > 0 }
        val blocked = sp.getBoolean(K_BLOCKED, false)
        val plan = sp.getString(K_PLAN, null).orEmpty()
        val status = AccessKeyRules.decide(configured(), !key.isNullOrBlank(), expires, blocked, System.currentTimeMillis())
        _info.value = LicenseInfo(
            // With no key server configured this is a personal build, so no key is required.
            active = status == KeyStatus.ACTIVE || status == KeyStatus.NOT_CONFIGURED,
            plan = plan,
            expiresAt = expires,
            statusText = AccessKeyRules.message(status),
            blocked = blocked,
            hasRememberedKey = !key.isNullOrBlank(),
        )
    }

    /** Re-checks the key with the backend, at most once per 3 hours unless [force] is set. */
    suspend fun liveCheck(ctx: Context, force: Boolean = false) = withContext(Dispatchers.IO) {
        if (configured()) {
            val sp = prefs(ctx)
            val key = sp.getString(K_KEY, null)
            val now = System.currentTimeMillis()
            if (!key.isNullOrBlank() && (force || now - sp.getLong(K_LAST, 0L) >= RECHECK_MS)) {
                when (val r = remote("check", key, deviceId(ctx))) {
                    is Remote.Valid -> store(ctx, key, r.plan, r.expiresAt, r.blocked)
                    Remote.Revoked -> store(ctx, key, "", null, blocked = true)
                    Remote.Unreachable -> Unit // fail open: keep the last known state
                }
            }
        }
        refreshInfo(ctx)
    }

    suspend fun activate(ctx: Context, rawKey: String): Activation = withContext(Dispatchers.IO) {
        if (!configured()) return@withContext Activation.NotConfigured
        val key = rawKey.trim()
        if (key.isEmpty()) return@withContext Activation.Invalid
        when (val r = remote("activate", key, deviceId(ctx))) {
            is Remote.Valid -> {
                if (r.blocked) return@withContext Activation.Blocked
                store(ctx, key, r.plan, r.expiresAt, blocked = false)
                Activation.Ok(r.plan)
            }
            Remote.Revoked -> Activation.Invalid
            Remote.Unreachable -> Activation.Unreachable
        }
    }

    fun forget(ctx: Context) {
        prefs(ctx).edit().clear().apply()
        refreshInfo(ctx)
    }

    private fun store(ctx: Context, key: String, plan: String, expiresAt: Long?, blocked: Boolean) {
        prefs(ctx).edit()
            .putString(K_KEY, key)
            .putString(K_PLAN, plan)
            .putLong(K_EXPIRES, expiresAt ?: 0L)
            .putBoolean(K_BLOCKED, blocked)
            .putLong(K_LAST, System.currentTimeMillis())
            .apply()
        refreshInfo(ctx)
    }

    /** Stable per-device id derived from the Android id. It is not a secret. */
    private fun deviceId(ctx: Context): String {
        val androidId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        return MessageDigest.getInstance("SHA-256")
            .digest("lia|$androidId".toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(32)
    }

    private fun remote(path: String, key: String, deviceId: String): Remote {
        val base = BuildConfig.LICENSE_BASE_URL.trimEnd('/')
        val body = JSONObject().put("key", key).put("device_id", deviceId)
        val request = Request.Builder()
            .url("$base/v1/$path")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            http.newCall(request).execute().use { resp ->
                when {
                    resp.code == 401 || resp.code == 403 -> Remote.Revoked
                    !resp.isSuccessful -> Remote.Unreachable
                    else -> {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        if (!json.optBoolean("valid", false)) {
                            Remote.Revoked
                        } else {
                            Remote.Valid(
                                plan = json.optString("plan", ""),
                                expiresAt = json.optLong("expires_at", 0L).takeIf { it > 0 },
                                blocked = json.optBoolean("blocked", false),
                            )
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Remote.Unreachable
        } catch (e: org.json.JSONException) {
            Remote.Unreachable
        }
    }
}
