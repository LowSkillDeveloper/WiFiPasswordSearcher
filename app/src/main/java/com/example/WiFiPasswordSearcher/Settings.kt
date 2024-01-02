package com.example.WiFiPasswordSearcher

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor

/**
 * Created by пк on 19.11.2015.
 */
class Settings(private val context: Context) {
    @JvmField
    var AppSettings: SharedPreferences? = null
    @JvmField
    var Editor: Editor? = null
    @SuppressLint("CommitPrefEdits")
    private fun Init() {
        AppSettings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)
        Editor = AppSettings?.edit()
    }

    fun Reload() {
        Init()
    }

    companion object {
        private const val APP_PREFERENCES = "settings"
        const val APP_SERVER_LOGIN = "SERVER_LOGIN"
        const val APP_SERVER_PASSWORD = "SERVER_PASSWORD"
        const val APP_SERVER_URI = "SERVER_URI"
        const val APP_FETCH_ESS = "FETCH_ESS"
        const val APP_CHECK_UPDATES = "CHECK_UPDATES"
        const val API_READ_KEY = "READ_KEY"
        const val API_WRITE_KEY = "WRITE_KEY"
        const val API_KEYS_VALID = "KEYS_VALID"
        const val USER_REGDATE = "USER_REGDATE"
        const val USER_NICK = "USER_NICK"
        const val USER_GROUP = "USER_GROUP"
        const val WIFI_SIGNAL = "WIFI_SIGNAL"
        const val WPS_SOURCE = "WPS_SOURCE"
    }

    init {
        Init()
    }
}