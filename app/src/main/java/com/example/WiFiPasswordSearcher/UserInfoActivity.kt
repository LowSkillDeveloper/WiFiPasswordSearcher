package com.example.WiFiPasswordSearcher

import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.example.WiFiPasswordSearcher.databinding.ActivityUserBinding
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by пк on 20.12.2015.
 */
class UserInfoActivity : Activity() {
    private lateinit var binding: ActivityUserBinding
    private lateinit var info: String
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        info = intent.extras?.getString("showInfo") ?: "user"
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

        override fun doInBackground(input: Array<String?>): String {
            val hc = DefaultHttpClient()
            val res: ResponseHandler<String> = BasicResponseHandler()
            val mSettings = Settings(applicationContext)
            mSettings.Reload()
            val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
            val http = HttpGet("$serverURI/wpspin")
            var str = ""
            try {
                str = hc.execute(http, res)
            } catch (ignored: Exception) {
            }
            return str
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