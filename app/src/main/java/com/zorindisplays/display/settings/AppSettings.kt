package com.zorindisplays.display.settings

import android.content.Context
import com.zorindisplays.display.net.protocol.Role

private const val PREFS = "hilo_settings"
private const val KEY_ROLE = "role"
private const val KEY_HOST = "host"
private const val KEY_PORT = "port"
private const val KEY_TABLE_ID = "tableId"

data class SettingsState(
    val role: Role = Role.TABLE,
    val host: String = "10.0.2.2",
    val port: Int = 8080,
    val tableId: Int = 1
)

class AppSettings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): SettingsState = SettingsState(
        role = runCatching { Role.valueOf(prefs.getString(KEY_ROLE, Role.TABLE.name)!!) }
            .getOrElse { Role.TABLE },
        host = prefs.getString(KEY_HOST, "10.0.2.2") ?: "10.0.2.2",
        port = prefs.getInt(KEY_PORT, 8080),
        tableId = prefs.getInt(KEY_TABLE_ID, 1)
    )

    fun save(s: SettingsState) {
        prefs.edit()
            .putString(KEY_ROLE, s.role.name)
            .putString(KEY_HOST, s.host)
            .putInt(KEY_PORT, s.port)
            .putInt(KEY_TABLE_ID, s.tableId)
            .apply()
    }
}
