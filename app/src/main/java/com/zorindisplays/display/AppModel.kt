package com.zorindisplays.display

import android.content.Context
import com.zorindisplays.display.host.BigHostServer
import com.zorindisplays.display.net.TableClient
import com.zorindisplays.display.net.protocol.Cmd
import com.zorindisplays.display.net.protocol.Role
import com.zorindisplays.display.settings.AppSettings
import com.zorindisplays.display.settings.SettingsState
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AppModel(ctx: Context) {
    private val settingsStore = AppSettings(ctx)

    var settings: SettingsState = settingsStore.load()
        private set

    private var bigServer: BigHostServer? = null
    private var tableClient: TableClient? = null

    val tableState: StateFlow<com.zorindisplays.display.net.protocol.RoundStateDto?>?
        get() = tableClient?.state

    val tableConnected: StateFlow<Boolean>?
        get() = tableClient?.connected

    fun start() = restartNetworking()

    fun stop() {
        bigServer?.stop()
        bigServer = null
        tableClient?.stop()
        tableClient = null
    }

    fun applySettings(newSettings: SettingsState) {
        settings = newSettings
        settingsStore.save(newSettings)
        restartNetworking()
    }

    fun send(cmd: Cmd) {
        tableClient?.send(cmd)
    }

    private fun restartNetworking() {
        bigServer?.stop()
        bigServer = null
        tableClient?.stop()
        tableClient = null

        when (settings.role) {
            Role.BIG -> {
                // у тебя BigHostServer только (port)
                bigServer = BigHostServer(port = settings.port).also { it.start() }
            }
            Role.TABLE -> {
                tableClient = TableClient(
                    deviceId = UUID.randomUUID().toString(),
                    tableId = settings.tableId,
                    host = settings.host,
                    port = settings.port
                ).also { it.connect() }
            }
        }
    }
}
