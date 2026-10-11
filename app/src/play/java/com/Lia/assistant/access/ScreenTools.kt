package com.Lia.assistant.access

import android.content.Context
import org.json.JSONObject

/** Play build: no screen control. Always reports that the tool does not exist. */
object ScreenTools {
    fun execute(ctx: Context, name: String, args: JSONObject): JSONObject? = null
}
