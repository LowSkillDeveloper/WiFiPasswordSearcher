package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

class AppVersion(private val context: Context) {
    private var ActualyVersion = 0f
    private var WhatNews = ""
    private var LoadSuccesses = false
    private val wpsInternalDate = "2020-01-12 21:00:00"
    private val mSettings: Settings
    fun ShowUpdateDialog(activity: Activity?) {
        val ad = AlertDialog.Builder(activity)
        ad.setTitle(String.format(context.getString(R.string.dialog_title_update), ActualyVersion))
        ad.setMessage(WhatNews)
        ad.setCancelable(false)
        ad.setPositiveButton(context.getString(R.string.download)) { dialog, arg1 ->
            mSettings.Reload()
            val SERVER_URI = mSettings.AppSettings?.getString(Settings.APP_SERVER_URI, context.resources.getString(R.string.SERVER_URI_DEFAULT))
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("$SERVER_URI/api/app.latest.apk"))
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(browserIntent)
        }
        ad.setNegativeButton(context.getString(R.string.ask_later)) { dialog, arg1 -> }
        ad.show()
    }

    @Throws(IOException::class)
    private fun GetActualyVersion() {
        val Args = "/api/ajax.php?Query=AppVersion"
        var ReadLine: String?
        val RawData = StringBuilder()
        mSettings.Reload()
        val SERVER_URI = mSettings.AppSettings?.getString(Settings.APP_SERVER_URI, context.resources.getString(R.string.SERVER_URI_DEFAULT))
        val Uri = URL(SERVER_URI + Args)
        val Connection = Uri.openConnection() as HttpURLConnection
        Connection.requestMethod = "GET"
        Connection.readTimeout = 10 * 1000
        Connection.connect()
        val Reader = BufferedReader(InputStreamReader(Connection.inputStream))
        while (Reader.readLine().also { ReadLine = it } != null) {
            RawData.append(ReadLine)
        }
        try {
            val Json = JSONObject(RawData.toString())
            LoadSuccesses = Json.getBoolean("Successes")
            if (LoadSuccesses) {
                ActualyVersion = Json.getDouble("ActualyVersion").toFloat()
                WhatNews = Json.getString("WhatNews")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun isActualyVersion(context: Context, showMessage: Boolean): Boolean {
        if (showMessage) LoadSuccesses = false
        if (!LoadSuccesses) try {
            GetActualyVersion()
        } catch (e: IOException) {
            LoadSuccesses = false
        }
        if (!LoadSuccesses) {
            if (showMessage) {
                val t = Toast.makeText(context, context.getString(R.string.status_no_internet), Toast.LENGTH_SHORT)
                t.show()
            }
            return true
        }
        val CurVersion = context.resources.getString(R.string.app_version).toFloat()
        if (CurVersion >= ActualyVersion) {
            if (showMessage) {
                val t = Toast.makeText(context, context.getString(R.string.toast_using_latest_version), Toast.LENGTH_SHORT)
                t.show()
            }
            return true
        }
        return false
    }

    private fun wpsCompanionExists(): Boolean {
        val file = File(context.filesDir.absolutePath + "/wpspin.html")
        return file.exists()
    }

    fun wpsCompanionInit(force: Boolean?) {
        if (wpsCompanionExists() && !force!!) return
        val filename = "wpspin.html"
        try {
            val `in` = context.assets.open(filename)
            val size = `in`.available()
            val data = ByteArray(size)
            `in`.read(data)
            `in`.close()
            val str = String(data, Charset.forName("UTF-8"))
            val date: Date
            val format = SimpleDateFormat(context.resources.getString(R.string.DEFAULT_DATE_FORMAT), Locale.US)
            date = try {
                format.parse(wpsInternalDate)
            } catch (e: Exception) {
                Date()
            }
            wpsCompanionUpdate(str, date)
        } catch (ignored: Exception) {
        }
    }

    fun wpsCompanionUpdate(str: String, date: Date) {
        var str = str
        val outFile = File(context.filesDir.absolutePath + "/wpspin.html")
        try {
            val out: OutputStream = FileOutputStream(outFile)
            str = str.replace("a.filter((n) => b.includes(n));", "a;")
            val data = str.toByteArray(Charset.forName("UTF-8"))
            out.write(data)
            out.close()
            outFile.setLastModified(date.time)
        } catch (ignored: Exception) {
        }
    }

    fun wpsCompanionGetPath(): String? {
        return if (!wpsCompanionExists()) null else context.filesDir.absolutePath + "/wpspin.html"
    }

    fun wpsCompanionGetDate(): Date {
        val file = File(context.filesDir.absolutePath + "/wpspin.html")
        val date = Date()
        date.time = file.lastModified()
        return date
    }

    fun wpsCompanionGetSize(): Long {
        val file = File(context.filesDir.absolutePath + "/wpspin.html")
        return file.length()
    }

    fun wpsCompanionInternal(): Boolean {
        val date: Date
        val format = SimpleDateFormat(context.resources.getString(R.string.DEFAULT_DATE_FORMAT), Locale.US)
        date = try {
            format.parse(wpsInternalDate)
        } catch (e: Exception) {
            Date()
        }
        return date.compareTo(wpsCompanionGetDate()) == 0
    }

    fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KiB", "MiB", "GiB", "TiB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    init {
        mSettings = Settings(context)
    }
}