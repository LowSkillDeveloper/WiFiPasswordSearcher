package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.WiFiPasswordSearcher.databinding.ActivityStartBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Created by пк on 20.12.2015.
 */
class StartActivity : Activity() {
    private lateinit var binding: ActivityStartBinding
    private lateinit var mSettings: Settings
    private lateinit var user: UserManager
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onConfigurationChanged(resources.configuration)
        mSettings = Settings(applicationContext)
        user = UserManager(applicationContext)
        val apiKeysValid = mSettings.AppSettings!!.getBoolean(Settings.API_KEYS_VALID, false)
        val savedLogin = mSettings.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
        val savedPassword = mSettings.AppSettings!!.getString(Settings.APP_SERVER_PASSWORD, "")
        val checkUpdates = mSettings.AppSettings!!.getBoolean(Settings.APP_CHECK_UPDATES, true)
        if (apiKeysValid) {
            binding.btnGetApiKeys.visibility = View.GONE
            binding.edtLogin.isEnabled = false
            binding.edtPassword.isEnabled = false
            binding.llStartMenu.visibility = View.VISIBLE
            if (checkUpdates && !VersionAlreadyChecked) {
                Thread(Runnable {
                    val appVersion = AppVersion(applicationContext)
                    val result = appVersion.isActualyVersion(applicationContext, false)
                    if (!result) {
                        runOnUiThread { appVersion.ShowUpdateDialog(this@StartActivity) }
                    }
                    VersionAlreadyChecked = true
                }).start()
            }
        }
        binding.edtLogin.setText(savedLogin)
        binding.edtPassword.setText(savedPassword)
        binding.btnGetApiKeys.setOnClickListener {
            val dProcess = ProgressDialog(this@StartActivity)
            dProcess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            dProcess.setMessage(resources.getString(R.string.status_signing_in))
            dProcess.setCanceledOnTouchOutside(false)
            dProcess.show()
            Thread(Runnable {
                var res = false
                try {
                    res = getApiKeys(binding.edtLogin.text.toString(), binding.edtPassword.text.toString())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (res) {
                    user.fromSettings
                    runOnUiThread {
                        binding.btnGetApiKeys.visibility = View.GONE
                        binding.llStartMenu.visibility = View.VISIBLE
                        binding.edtLogin.isEnabled = false
                        binding.edtPassword.isEnabled = false
                    }
                }
                dProcess.dismiss()
            }).start()
        }
        binding.btnUserInfo.setOnClickListener {
            val userActivity = Intent(this@StartActivity, UserInfoActivity::class.java)
            userActivity.putExtra("showInfo", "user")
            startActivity(userActivity)
        }
        binding.btnStart.setOnClickListener {
            val mainActivity = Intent(this@StartActivity, MyActivity::class.java)
            startActivity(mainActivity)
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.rootLayout.orientation = LinearLayout.VERTICAL
            binding.layoutPadding.visibility = View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.rootLayout.orientation = LinearLayout.HORIZONTAL
            binding.layoutPadding.visibility = View.GONE
        }
    }

    @Throws(IOException::class)
    private fun getApiKeys(Login: String, Password: String): Boolean {
        val args = "/api/apikeys"
        val reader: BufferedReader
        var readLine: String?
        val rawData = StringBuilder()
        try {
            mSettings.Reload()
            val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))!!
            val uri = URL(serverURI + args)
            val connection = uri.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val writer = DataOutputStream(
                    connection.outputStream)
            writer.writeBytes(
                    "login=" + URLEncoder.encode(Login, "UTF-8") +
                            "&password=" + URLEncoder.encode(Password, "UTF-8") +
                            "&genread=1")
            connection.readTimeout = 10 * 1000
            connection.connect()
            reader = BufferedReader(InputStreamReader(connection.inputStream))
            while (reader.readLine().also { readLine = it } != null) {
                rawData.append(readLine)
            }
            try {
                var readApiKey: String? = null
                var writeApiKey: String? = null
                val json = JSONObject(rawData.toString())
                val successes = json.getBoolean("result")
                return if (successes) {
                    val profile = json.getJSONObject("profile")
                    val keys = json.getJSONArray("data")
                    for (i in 0 until keys.length()) {
                        val keyData = keys.getJSONObject(i)
                        val access = keyData.getString("access")
                        if (access == "read") {
                            readApiKey = keyData.getString("key")
                        } else if (access == "write") {
                            writeApiKey = keyData.getString("key")
                        }
                        if (readApiKey != null && writeApiKey != null) break
                    }
                    if (readApiKey == null) {
                        runOnUiThread {
                            val t = Toast.makeText(applicationContext, resources.getString(R.string.toast_no_api_keys), Toast.LENGTH_SHORT)
                            t.show()
                        }
                        return false
                    }
                    mSettings.Editor!!.putString(Settings.APP_SERVER_LOGIN, Login)
                    mSettings.Editor!!.putString(Settings.APP_SERVER_PASSWORD, Password)
                    mSettings.Editor!!.putString(Settings.API_READ_KEY, readApiKey)
                    mSettings.Editor!!.putString(Settings.API_WRITE_KEY, writeApiKey)
                    mSettings.Editor!!.putBoolean(Settings.API_KEYS_VALID, true)
                    mSettings.Editor!!.putString(Settings.USER_NICK, profile.getString("nick"))
                    mSettings.Editor!!.putString(Settings.USER_REGDATE, profile.getString("regdate"))
                    mSettings.Editor!!.putInt(Settings.USER_GROUP, profile.getInt("level"))
                    mSettings.Editor!!.commit()
                    true
                } else {
                    val error = json.getString("error")
                    val errorDesc = user.getErrorDesc(error, this)
                    runOnUiThread {
                        val t = Toast.makeText(applicationContext, errorDesc, Toast.LENGTH_SHORT)
                        t.show()
                    }
                    false
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        runOnUiThread {
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> btnOffline()
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
                dialog.dismiss()
            }
            val builder = AlertDialog.Builder(this@StartActivity)
            builder.setTitle(resources.getString(R.string.status_no_internet))
                    .setMessage(resources.getString(R.string.dialog_work_offline))
                    .setPositiveButton(resources.getString(R.string.dialog_yes), dialogClickListener)
                    .setNegativeButton(resources.getString(R.string.dialog_no), dialogClickListener).show()
        }
        return false
    }

    fun btnOffline() {
        mSettings.Editor!!.putString(Settings.API_READ_KEY, "offline")
        mSettings.Editor!!.commit()
        val offlineActivityIntent = Intent(this@StartActivity, MyActivity::class.java)
        startActivity(offlineActivityIntent)
    }

    companion object {
        private var VersionAlreadyChecked = false
    }
}