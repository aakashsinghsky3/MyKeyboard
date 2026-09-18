package com.example.mykeyboard.settings

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** One background thread for all settings disk work (dictionary, clipboard). */
object SettingsIo {
    val executor: ExecutorService = Executors.newSingleThreadExecutor { r -> Thread(r, "settings-io").apply { isDaemon = true } }
}
