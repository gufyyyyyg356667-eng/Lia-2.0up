package com.Lia.assistant.alerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Live information: weather, nearby pharmacies, calendar. Call from a background thread only. */
object InfoSources {
    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Place(val name: String, val lat: Double, val lon: Double, val meters: Int)

    private fun granted(ctx: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    /** Last known position from GPS or network. Null if location permission or a fix is missing. */
    fun lastLocation(ctx: Context): Location? {
        if (!granted(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) && !granted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return null
        }
        val lm = ctx.getSystemService(LocationManager::class.java) ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
    }

    /** Current temperature and rain from Open-Meteo (no key needed). */
    fun weatherText(lat: Double, lon: Double): String? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,precipitation&timezone=auto"
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val current = JSONObject(response.body?.string().orEmpty()).optJSONObject("current") ?: return null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val rain = current.optDouble("precipitation", 0.0)
            if (temp.isNaN()) return null
            val rainText = if (rain > 0) "বৃষ্টি হচ্ছে" else "বৃষ্টি নেই"
            return "এখন তাপমাত্রা ${temp.roundToInt()} ডিগ্রি, $rainText।"
        }
    }

    /** Nearby pharmacies from OpenStreetMap (no key needed), nearest first. */
    fun nearestPharmacies(lat: Double, lon: Double, limit: Int): List<Place> {
        val query = "[out:json][timeout:20];node[\"amenity\"=\"pharmacy\"](around:3000,$lat,$lon);out 30;"
        val body = FormBody.Builder().add("data", query).build()
        http.newCall(Request.Builder().url("https://overpass-api.de/api/interpreter").post(body).build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val elements = JSONObject(response.body?.string().orEmpty()).optJSONArray("elements") ?: return emptyList()
            val places = (0 until elements.length()).map { i ->
                val o = elements.getJSONObject(i)
                val name = o.optJSONObject("tags")?.optString("name").orEmpty().ifBlank { "নামবিহীন ফার্মেসি" }
                val plat = o.optDouble("lat")
                val plon = o.optDouble("lon")
                Place(name, plat, plon, Geo.distanceMeters(lat, lon, plat, plon))
            }
            return places.sortedBy { it.meters }.take(limit)
        }
    }

    /** Calendar events between two times, as (title, start time in ms). Needs calendar permission. */
    fun eventsBetween(ctx: Context, fromMs: Long, toMs: Long): List<Pair<String, Long>> {
        if (!granted(ctx, Manifest.permission.READ_CALENDAR)) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(fromMs.toString())
            .appendPath(toMs.toString())
            .build()
        val out = mutableListOf<Pair<String, Long>>()
        ctx.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN),
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                out += (cursor.getString(0) ?: "ইভেন্ট") to cursor.getLong(1)
            }
        }
        return out
    }
}
