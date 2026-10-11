package com.Lia.assistant.action

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.Lia.assistant.access.ScreenTools
import com.Lia.assistant.license.AccessKeyManager
import org.json.JSONObject

/**
 * Runs the tools Gemini asked for. Every result is a small JSON object.
 * The message tool only opens a composer. The user always taps Send.
 */
object ActionExecutor {

    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject {
        val id = ToolLog.begin(name, args)
        val started = System.currentTimeMillis()
        val result = if (!AccessKeyManager.isActive()) {
            JSONObject().put("result", "license_required").put("message", AccessKeyManager.refusalMessage())
        } else {
            runTool(ctx, name, args)
        }
        ToolLog.finish(id, result, System.currentTimeMillis() - started)
        return result
    }

    private fun runTool(ctx: Context, name: String, args: JSONObject): JSONObject = try {
        when (name) {
            "open_app" -> openApp(ctx, args.optString("app_name"))
            "call_contact" -> callContact(ctx, args.optString("contact_name"))
            "message_contact" -> messageContact(ctx, args.optString("contact_name"), args.optString("message"))
            "search_youtube" -> searchYouTube(ctx, args.optString("query"))
            "web_search" -> webSearch(ctx, args.optString("query"))
            else -> InfoTools.execute(ctx, name, args)
                ?: ScreenTools.execute(ctx, name, args)
                ?: JSONObject().put("error", "Unknown tool: $name")
        }
    } catch (e: Exception) {
        result("failed")
    }

    private fun result(value: String): JSONObject = JSONObject().put("result", value)

    private fun openApp(ctx: Context, appName: String): JSONObject {
        val index = AppIndex.get(ctx)
        val pkg = AppMatcher.match(BanglaNames.toEnglish(appName), index)
            ?: AppMatcher.match(appName, index)
            ?: return result("app_not_found")
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return result("app_not_found")
        return launch(ctx, intent, "opened")
    }

    private fun callContact(ctx: Context, query: String): JSONObject =
        when (val found = ContactResolver.lookup(ctx, query)) {
            is ContactResolver.Lookup.Found -> {
                val canCall = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED
                val action = if (canCall) Intent.ACTION_CALL else Intent.ACTION_DIAL
                launch(
                    ctx,
                    Intent(action, Uri.parse("tel:${found.number}")),
                    if (canCall) "calling" else "dialer_opened_no_permission"
                )
            }
            ContactResolver.Lookup.NoPermission -> result("contacts_permission_missing")
            ContactResolver.Lookup.NotFound -> result("contact_not_found")
        }

    private fun messageContact(ctx: Context, query: String, message: String): JSONObject =
        when (val found = ContactResolver.lookup(ctx, query)) {
            is ContactResolver.Lookup.Found -> {
                val digits = found.number.filter { it.isDigit() }
                val hasWhatsApp = ctx.packageManager.getLaunchIntentForPackage("com.whatsapp") != null
                val intent = if (hasWhatsApp) {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}"))
                        .setPackage("com.whatsapp")
                } else {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${found.number}"))
                        .putExtra("sms_body", message)
                }
                launch(ctx, intent, "composer_opened")
            }
            ContactResolver.Lookup.NoPermission -> result("contacts_permission_missing")
            ContactResolver.Lookup.NotFound -> result("contact_not_found")
        }

    private fun searchYouTube(ctx: Context, query: String): JSONObject {
        val q = query.trim()
        if (q.isEmpty()) return result("nothing_to_search")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(q)}"))
        if (ctx.packageManager.getLaunchIntentForPackage("com.google.android.youtube") != null) {
            intent.setPackage("com.google.android.youtube")
        }
        return launch(ctx, intent, "youtube_search_opened")
    }

    private fun webSearch(ctx: Context, query: String): JSONObject {
        val q = query.trim()
        if (q.isEmpty()) return result("nothing_to_search")
        return launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(q)}")), "search_opened")
    }

    private fun launch(ctx: Context, intent: Intent, success: String): JSONObject {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            result(success)
        } catch (e: ActivityNotFoundException) {
            result("app_not_found")
        } catch (e: SecurityException) {
            result("permission_denied")
        }
    }
}
