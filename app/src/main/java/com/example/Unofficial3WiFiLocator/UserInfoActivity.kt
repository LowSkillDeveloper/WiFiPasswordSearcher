package com.example.Unofficial3WiFiLocator

import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.Unofficial3WiFiLocator.databinding.ActivityUserBinding
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UserInfoActivity : Activity() {
    private lateinit var binding: ActivityUserBinding
    private lateinit var info: String
    private lateinit var mSettings: Settings
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mSettings = Settings(applicationContext)
        info = intent.extras?.getString("showInfo") ?: "user"
        val apiReadKey = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "Not Set")
        val apiWriteKey = mSettings.AppSettings!!.getString(Settings.API_WRITE_KEY, "Not Set")
        binding.txtApiKeys.text = "Read Key: $apiReadKey\nWrite Key: $apiWriteKey"
        val nick: String
        val group: String
        val date: Date
        if (info == "wpspin") {
            val updater = AppVersion(applicationContext)
            updater.wpsCompanionInit(false)
            binding.buttonsLayout.visibility = LinearLayout.VISIBLE
            binding.btnRevert.isEnabled = !updater.wpsCompanionInternal()
            nick = "WPS PIN Companion"
            binding.labRegDate.text = getString(R.string.label_last_updated)
            date = updater.wpsCompanionGetDate()
            binding.labGroup.text = getString(R.string.label_file_size)
            val size = updater.wpsCompanionGetSize()
            group = updater.readableFileSize(size)
        } else {
            val User = UserManager(applicationContext)
            User.fromSettings
            binding.buttonsLayout.visibility = LinearLayout.GONE
            val format = SimpleDateFormat(resources.getString(R.string.DEFAULT_DATE_FORMAT), Locale.US)
            date = try {
                format.parse(User.regDate)
            } catch (e: Exception) {
                Date()
            }
            nick = User.nickName
            group = User.getGroup(applicationContext)
        }
        binding.txtLogin.text = nick
        binding.txtRegDate.text = DateFormat.getDateTimeInstance().format(date)
        binding.txtGroup.text = group
    }

    private fun setAppTheme() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }
    }

    private inner class AsyncWpsUpdater : AsyncTask<String?, Void?, String>() {
        private var pd: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            pd = ProgressDialog(this@UserInfoActivity)
            pd!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            pd!!.setMessage(getString(R.string.status_updating))
            pd!!.setCanceledOnTouchOutside(false)
            pd!!.show()
        }

        override fun doInBackground(vararg input: String?): String {
            val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT)) ?: ""
            val useCustomHost = mSettings.AppSettings!!.getBoolean("USE_CUSTOM_HOST", false)
            val url = if (useCustomHost && serverURI.startsWith("http://134.0.119.34")) {
                URL("http://134.0.119.34/wpspin")
            } else {
                URL("$serverURI/wpspin")
            }

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (useCustomHost && serverURI.startsWith("http://134.0.119.34")) {
                connection.setRequestProperty("Host", "3wifi.stascorp.com")
            }

            val inputStream = connection.inputStream
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            return bufferedReader.readText()
        }

        override fun onPostExecute(str: String) {
            val msg: String
            pd!!.dismiss()
            if (str.contains("initAlgos();")) {
                val updater = AppVersion(applicationContext)
                updater.wpsCompanionUpdate(str, Date())
                binding.txtRegDate.text = DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate())
                binding.txtGroup.text = updater.readableFileSize(updater.wpsCompanionGetSize())
                binding.btnRevert.isEnabled = !updater.wpsCompanionInternal()
                msg = getString(R.string.toast_updated_successful)
            } else if (str.isEmpty()) {
                msg = getString(R.string.status_no_internet)
            } else {
                msg = getString(R.string.toast_update_failed)
            }
            val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    fun btnUpdateOnClick(view: View) {
        if (info == "wpspin") {
            AsyncWpsUpdater().execute()
        }
    }
    fun btnLogoutOnClick(view: View) {
        val mSettings = Settings(applicationContext)
        mSettings.Reload()
        mSettings.Editor!!.remove(Settings.APP_SERVER_LOGIN)
        mSettings.Editor!!.remove(Settings.APP_SERVER_PASSWORD)
        mSettings.Editor!!.remove(Settings.API_READ_KEY)
        mSettings.Editor!!.remove(Settings.API_WRITE_KEY)
        mSettings.Editor!!.remove(Settings.API_KEYS_VALID)
        mSettings.Editor!!.commit()

        val startPage = Intent(applicationContext, StartActivity::class.java)
        startPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(startPage)
    }

    fun copyApiKeys(view: View) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val apiReadKey = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        val apiWriteKey = mSettings.AppSettings!!.getString(Settings.API_WRITE_KEY, "")
        val clip = ClipData.newPlainText("API Keys", "Read Key: $apiReadKey\nWrite Key: $apiWriteKey")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "API keys copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun btnRevertOnClick(v: View) {
        if (info == "wpspin") {
            val updater = AppVersion(applicationContext)
            updater.wpsCompanionInit(true)
            binding.txtRegDate.text = DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate())
            binding.txtGroup.text = updater.readableFileSize(updater.wpsCompanionGetSize())
            v.isEnabled = !updater.wpsCompanionInternal()
            val toast = Toast.makeText(applicationContext,
                    getString(R.string.toast_reverted_to_init_state), Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}