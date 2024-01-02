package com.example.WiFiPasswordSearcher

import android.content.Context

/**
 * Created by пк on 20.12.2015.
 */
internal class UserManager(context: Context) {
    private val mSettings: Settings
    @JvmField
    var nickName = ""
    @JvmField
    var regDate = ""
    private var level = -1
    val fromSettings: Unit
        get() {
            nickName = mSettings.AppSettings!!.getString(Settings.USER_NICK, "")!!
            regDate = mSettings.AppSettings!!.getString(Settings.USER_REGDATE, "")!!
            level = mSettings.AppSettings!!.getInt(Settings.USER_GROUP, -1)
        }

    fun getGroup(context: Context): String {
        return getTextGroup(level, context)
    }

    private fun getTextGroup(Level: Int, context: Context): String {
        return when (Level) {
            -2 -> context.getString(R.string.access_level_banned)
            -1 -> context.getString(R.string.access_level_no_logged)
            0 -> context.getString(R.string.access_level_guest)
            1 -> context.getString(R.string.access_level_user)
            2 -> context.getString(R.string.access_level_developer)
            3 -> context.getString(R.string.access_level_admin)
            else -> ""
        }
    }

    fun getErrorDesc(error: String, context: Context): String {
        return when (error) {
            "database" -> context.getString(R.string.error_database_maintenance)
            "loginfail" -> context.getString(R.string.error_incorrect_credentials)
            "form" -> context.getString(R.string.error_form_fields)
            "cooldown" -> context.getString(R.string.error_cooldown)
            else -> String.format(context.getString(R.string.unknown_error), error)
        }
    }

    init {
        val APP_VERSION = context.resources.getString(R.string.app_version)
        mSettings = Settings(context)
        val API_READ_KEY = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        val login = mSettings.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
    }
}