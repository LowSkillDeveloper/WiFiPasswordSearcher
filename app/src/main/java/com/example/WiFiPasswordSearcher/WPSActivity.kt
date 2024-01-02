package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WpsCallback
import android.net.wifi.WpsInfo
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.InputType
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.Toast
import com.example.WiFiPasswordSearcher.databinding.ActivityWpsBinding
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

data class WPSPin (var mode: Int, var name: String, var pin: String = "", var sugg: Boolean = false)

class WPSActivity : Activity() {
    private lateinit var binding: ActivityWpsBinding
    private lateinit var wifiMgr: WifiManager
    var data = ArrayList<ItemWps>()
    var pins = ArrayList<WPSPin>()
    private lateinit var pd: ProgressDialog
    private lateinit var mSettings: Settings
    private var wpsCallback: WpsCallback? = null
    private var wpsConnecting = false
    private var wpsLastPin: String? = ""
    var wpsPin = ArrayList<String?>()
    var wpsMet = ArrayList<String?>()
    var wpsScore = ArrayList<String>()
    var wpsDb = ArrayList<String>()
    private lateinit var mDb: SQLiteDatabase

    @Volatile
    private var wpsReady = false
    private var cachedPins = ""
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWpsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = applicationContext
        listContextMenuItems = getResources().getStringArray(R.array.menu_wps_pin)
        val mDBHelper = DatabaseHelper(this)
        try {
            mDBHelper.updateDataBase()
        } catch (mIOException: IOException) {
            throw Error("UnableToUpdateDatabase")
        }
        mDb = try {
            mDBHelper.writableDatabase
        } catch (mSQLException: SQLException) {
            throw mSQLException
        }
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        mSettings = Settings(applicationContext)
        API_READ_KEY = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        val essdWPS = intent.extras!!.getString("variable")
        binding.ESSDWpsTextView.text = essdWPS // ESSID
        val bssdWPS = intent.extras!!.getString("variable1")
        binding.BSSDWpsTextView.text = bssdWPS // BSSID
        wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wpsCallback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wpsCallback = object : WpsCallback() {
                override fun onStarted(pin: String) {
                    wpsConnecting = true
                    pd = ProgressDialog(this@WPSActivity)
                    pd.setProgressStyle(ProgressDialog.STYLE_SPINNER)
                    pd.setMessage(getString(R.string.status_connecting_to_the_network))
                    pd.setCanceledOnTouchOutside(false)
                    pd.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { dialog, which ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            wifiMgr.cancelWps(wpsCallback)
                        }
                        wpsConnecting = false
                        dialog.dismiss()
                        Toast.makeText(applicationContext, getString(R.string.toast_connection_canceled), Toast.LENGTH_SHORT).show()
                    }
                    pd.show()
                }

                override fun onSucceeded() {
                    if (!wpsConnecting) return
                    wpsConnecting = false
                    pd.dismiss()
                    Toast.makeText(applicationContext, getString(R.string.toast_connected_successfully), Toast.LENGTH_SHORT).show()
                }

                override fun onFailed(reason: Int) {
                    if (!wpsConnecting && reason > 2) return
                    wpsConnecting = false
                    pd.dismiss()
                    var title = getString(R.string.dialog_title_error_occurred)
                    val errorMessage: String
                    when (reason) {
                        0 -> if (wpsLastPin!!.isEmpty()) {
                            title = getString(R.string.dialog_title_wps_failed)
                            errorMessage = getString(R.string.dialog_message_not_support_empty)
                        } else {
                            errorMessage = getString(R.string.dialog_message_generic_failure)
                        }
                        1 -> errorMessage = getString(R.string.dialog_message_operation_in_progress)
                        2 -> errorMessage = getString(R.string.dialog_message_wifi_busy)
                        WifiManager.WPS_OVERLAP_ERROR -> errorMessage = getString(R.string.dialog_message_another_transaction)
                        WifiManager.WPS_WEP_PROHIBITED -> errorMessage = getString(R.string.dialog_message_wep_prohibited)
                        WifiManager.WPS_TKIP_ONLY_PROHIBITED -> errorMessage = getString(R.string.dialog_message_tkip_prohibited)
                        WifiManager.WPS_AUTH_FAILURE -> errorMessage = getString(R.string.dialog_message_wps_pin_incorrect)
                        WifiManager.WPS_TIMED_OUT -> {
                            title = getString(R.string.dialog_title_wps_timeout)
                            errorMessage = getString(R.string.dialog_message_network_did_not_respond)
                        }
                        else -> {
                            title = getString(R.string.dialog_title_oh_shit)
                            errorMessage = getString(R.string.unexpected_error) + reason
                        }
                    }
                    val builder = AlertDialog.Builder(this@WPSActivity)
                    builder.setTitle(title)
                            .setMessage(errorMessage)
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.ok)) { dialog, id -> dialog.dismiss() }
                    val alert = builder.create()
                    alert.show()
                }
            }
        }
        binding.WPSlist.onItemClickListener = OnItemClickListener { parent, itemClicked, position, id -> showMenu(bssdWPS, wpsPin[position]) }
        binding.webView.addJavascriptInterface(myJavascriptInterface(), "JavaHandler")
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val BSSDWps = intent.extras!!.getString("variable1")
                binding.webView.loadUrl("javascript:initAlgos();window.JavaHandler.initAlgos(JSON.stringify(algos),'$BSSDWps');")
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        val wpspin = AppVersion(applicationContext)
        wpspin.wpsCompanionInit(false)
        var path = wpspin.wpsCompanionGetPath()
        if (path == null) path = "/android_asset/wpspin.html"
        binding.webView.loadUrl("file://$path")
        AsyncInitActivity().execute(bssdWPS)
    }

    private fun showMenu(BSSID: String?, pin: String?) {
        val dialogBuilder = AlertDialog.Builder(this@WPSActivity)
        var spin = pin
        if (spin!!.isEmpty()) spin = "<empty>"
        dialogBuilder.setTitle(getString(R.string.selected_pin) + spin)
        dialogBuilder.setItems(listContextMenuItems) { dialog, item ->
            when (item) {
                0 -> {
                    if (!wifiMgr.isWifiEnabled) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_wifi_disabled), Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val wpsInfo = WpsInfo()
                        wpsInfo.BSSID = BSSID
                        wpsInfo.pin = pin
                        wpsInfo.setup = WpsInfo.KEYPAD
                        wpsLastPin = pin
                        wifiMgr.startWps(wpsInfo, wpsCallback)
                    } else {
                        val builder = AlertDialog.Builder(this@WPSActivity)
                        builder.setTitle(getString(R.string.dialog_title_unsupported_android))
                                .setMessage(getString(R.string.dialog_message_unsupported_android))
                                .setCancelable(false)
                                .setPositiveButton(getString(R.string.ok)) { dialog, id -> dialog.dismiss() }
                        val alert = builder.create()
                        alert.show()
                    }
                }
                1 -> {
                    Toast.makeText(applicationContext, String.format(getString(R.string.toast_pin_copied), pin), Toast.LENGTH_SHORT).show()
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val dataClip = ClipData.newPlainText("text", pin)
                        clipboard.setPrimaryClip(dataClip)
                    } catch (e: Exception) {
                    }
                }
            }
        }
        dialogBuilder.show()
    }

    private inner class AsyncInitActivity : AsyncTask<String, Void?, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            pd = ProgressDialog.show(this@WPSActivity, getString(R.string.status_please_wait), getString(R.string.status_initializing))
        }

        override fun doInBackground(BSSDWps: Array<String>): String {
            // get MAC manufacturer
            val BSSID = BSSDWps[0]
            var response2: String
            val hc2 = DefaultHttpClient()
            val res2: ResponseHandler<String> = BasicResponseHandler()
            val http2 = HttpPost("http://wpsfinder.com/ethernet-wifi-brand-lookup/MAC:$BSSID")
            try {
                response2 = hc2.execute(http2, res2)
                response2 = response2.substring(response2.indexOf("muted'><center>") + 15, response2.indexOf("<center></h4><h6"))
            } catch (e: Exception) {
                response2 = "N/A"
            }
            var wait = 8000
            while (!wpsReady && wait > 0) {
                try {
                    Thread.sleep(100)
                    wait -= 100
                } catch (ignored: Exception) {
                }
            }
            return response2
        }

        override fun onPostExecute(response2: String) {
            var response2 = response2
            val src = mSettings.AppSettings!!.getInt(Settings.WPS_SOURCE, 1)
            if (src != 1) pd.dismiss()
            if (response2.length > 50) {
                response2 = "unknown vendor"
            }
            binding.VendorWpsTextView.text = response2
            when (src) {
                1 -> btnwpsbaseclick(null)
                2 -> btnGenerate(null)
                3 -> btnLocalClick(null)
            }
        }
    }

    private inner class GetPinsFromBase : AsyncTask<String, Void?, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            val msg = getString(R.string.status_getting_pins)
            if (pd.isShowing) {
                pd.setMessage(msg)
            } else {
                pd = ProgressDialog.show(this@WPSActivity, getString(R.string.status_please_wait), msg)
            }
        }

        override fun doInBackground(BSSDWps: Array<String>): String {
            val BSSID = BSSDWps[0]
            var response: String
            data.clear()
            wpsScore.clear()
            wpsDb.clear()
            wpsPin.clear()
            wpsMet.clear()
            val hc = DefaultHttpClient()
            val res: ResponseHandler<String> = BasicResponseHandler()
            mSettings.Reload()
            val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
            val http = HttpGet("$serverURI/api/apiwps?key=$API_READ_KEY&bssid=$BSSID")
            try {
                response = if (cachedPins.isEmpty()) hc.execute(http, res) else cachedPins
                try {
                    var jObject = JSONObject(response)
                    val result = jObject.getBoolean("result")
                    if (result) {
                        cachedPins = response
                        try {
                            jObject = jObject.getJSONObject("data")
                            jObject = jObject.getJSONObject(BSSID)
                            val array = jObject.optJSONArray("scores")
                            for (i in 0 until array.length()) {
                                jObject = array.getJSONObject(i)
                                wpsPin.add(jObject.getString("value"))
                                wpsMet.add(jObject.getString("name"))
                                wpsScore.add(jObject.getString("score"))
                                wpsDb.add(if (jObject.getBoolean("fromdb")) "✔" else "")
                                val score = Math.round(wpsScore[i].toFloat() * 100)
                                wpsScore[i] = Integer.toString(score) + "%"
                                data.add(ItemWps(wpsPin[i], wpsMet[i], wpsScore[i], wpsDb[i]))
                            }
                        } catch (ignored: Exception) {
                        }
                    } else {
                        val error = jObject.getString("error")
                        if (error == "loginfail") {
                            mSettings.Editor!!.putBoolean(Settings.API_KEYS_VALID, false)
                            mSettings.Editor!!.commit()
                            runOnUiThread {
                                val t = Toast.makeText(applicationContext, getString(R.string.toast_enter_credentials), Toast.LENGTH_SHORT)
                                t.show()
                            }
                            val startActivity = Intent(applicationContext, StartActivity::class.java)
                            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(startActivity)
                        }
                        response = "api_error"
                    }
                } catch (e: JSONException) {
                    response = "json_error"
                }
            } catch (e: Exception) {
                response = "http_error"
            }
            return response
        }

        override fun onPostExecute(str: String) {
            pd.dismiss()
            var msg = ""
            var toast = true
            when {
                str == "http_error" -> {
                    msg = getString(R.string.status_no_internet)
                    toast = false
                }
                str == "json_error" -> {
                    msg = getString(R.string.connection_failure)
                    toast = false
                }
                str == "api_error" -> {
                    msg = getString(R.string.toast_database_failure)
                    toast = false
                }
                data.isEmpty() -> {
                    msg = getString(R.string.toast_no_pins_found)
                }
            }
            if (msg.isNotEmpty()) {
                data.add(ItemWps(null, msg, null, null))
            }
            binding.WPSlist.isEnabled = msg.isEmpty()
            binding.WPSlist.adapter = MyAdapterWps(this@WPSActivity, data)
            if (toast) toastMessage(String.format(getString(R.string.selected_source), "3WiFi Online WPS PIN"))
        }
    }

    fun btnwpsbaseclick(view: View?) { //пины из базы
        binding.baseButton.background.setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY)
        binding.wpsButton1.background.clearColorFilter()
        binding.wpsButton2.background.clearColorFilter()
        mSettings.Editor!!.putInt(Settings.WPS_SOURCE, 1)
        mSettings.Editor!!.commit()
        val bssdWPS = intent.extras!!.getString("variable1")
        GetPinsFromBase().execute(bssdWPS)
    }

    private inner class myJavascriptInterface {
        @JavascriptInterface
        fun initAlgos(json: String?, bssid: String) {
            pins.clear()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val pin = WPSPin(mode=obj.getInt("mode"), name=obj.getString("name"))
                    pins.add(pin)
                }
                binding.webView.post(Runnable {
                    fun run() {
                        binding.webView.loadUrl("javascript:window.JavaHandler.getPins(1,JSON.stringify(pinSuggestAPI(true,'$bssid',null)), '$bssid');")
                    }
                })
            } catch (e: JSONException) {
                wpsReady = true
            }
        }

        @JavascriptInterface
        fun getPins(all: Int, json: String?, bssid: String) {
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (all > 0) {
                        val pin = pins[i]
                        pin.pin = obj.getString("pin")
                        pin.sugg = false
                    } else {
                        val pin = pins[obj.getInt("algo")]
                        pin.sugg = true
                    }
                }
                if (all > 0) binding.webView.loadUrl("javascript:window.JavaHandler.getPins(0,JSON.stringify(pinSuggestAPI(false,'$bssid',null)), '');") else wpsReady = true
            } catch (e: JSONException) {
                pins.clear()
                wpsReady = true
            }
        }
    }

    fun btnGenerate(view: View?) { //генераторpppppp
        binding.wpsButton1.background.setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY)
        binding.baseButton.background.clearColorFilter()
        binding.wpsButton2.background.clearColorFilter()
        mSettings.Editor!!.putInt(Settings.WPS_SOURCE, 2)
        mSettings.Editor!!.commit()
        binding.WPSlist.adapter = null
        wpsPin.clear()
        wpsMet.clear()
        data.clear()
        for (pin in pins) {
            if (!pin.sugg) continue
            wpsPin.add(pin.pin)
            wpsMet.add(pin.name)
            data.add(ItemWps(
                    if (pin.pin == "") "<empty>" else pin.pin,
                    pin.name,
                    if (pin.mode == 3) "STA" else "",
                    "✔"
            ))
        }
        for (pin in pins) {
            if (pin.sugg) continue
            wpsPin.add(pin.pin)
            wpsMet.add(pin.name)
            data.add(ItemWps(
                    if (pin.pin == "") "<empty>" else pin.pin,
                    pin.name,
                    if (pin.mode == 3) "STA" else "",
                    ""
            ))
        }
        binding.WPSlist.isEnabled = pins.size > 0
        binding.WPSlist.adapter = MyAdapterWps(this@WPSActivity, data)
        toastMessage(String.format(getString(R.string.selected_source), "WPS PIN Companion"))
    }

    private fun findAlgoByPin(pin: String?): Int {
        for ((i, p) in pins.withIndex()) {
            if (pin == p.pin) return i
        }
        return -1
    }

    private fun findAlgoByName(name: String): Int {
        for ((i, p) in pins.withIndex()) {
            if (name == p.name) return i
        }
        return -1
    }

    fun btnLocalClick(view: View?) { //локальная база
        binding.wpsButton2.background.setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY)
        binding.wpsButton1.background.clearColorFilter()
        binding.baseButton.background.clearColorFilter()
        mSettings.Editor!!.putInt(Settings.WPS_SOURCE, 3)
        mSettings.Editor!!.commit()
        binding.WPSlist.adapter = null
        val bssdWPS = intent.extras!!.getString("variable1")
        try {
            data.clear()
            wpsPin.clear()
            val cursor = mDb.rawQuery("SELECT * FROM pins WHERE mac='" + bssdWPS!!.substring(0, 8) + "'", null)
            cursor.moveToFirst()
            do {
                var p = cursor.getString(0)
                if (p == "vacante") p = "" // empty pin
                var idx = findAlgoByPin(p)
                if (idx == -1) {
                    if (p == "airocon") idx = findAlgoByName("Airocon Realtek") else if (p == "arcady") idx = findAlgoByName("Livebox Arcadyan") else if (p == "asus") idx = findAlgoByName("ASUS PIN") else if (p == "dlink") idx = findAlgoByName("D-Link PIN") else if (p == "dlink1") idx = findAlgoByName("D-Link PIN +1") else if (p == "thirtytwo") idx = findAlgoByName("32-bit PIN") else if (p == "twentyeight") idx = findAlgoByName("28-bit PIN") else if (p == "zhao") idx = findAlgoByName("24-bit PIN")
                    if (idx > -1) {
                        val algo = pins[idx]
                        p = algo.pin
                    }
                }
                if (idx > -1) {
                    val algo = pins[idx]
                    data.add(ItemWps(
                            if (p == "") "<empty>" else p,
                            algo.name,
                            if (algo.mode == 3) "STA" else "",
                            ""
                    ))
                } else {
                    data.add(ItemWps(
                            if (p == "") "<empty>" else p,
                            "Unknown",
                            if (p!!.matches(Regex.fromLiteral("[0-9]+"))) "STA" else "",
                            ""
                    ))
                }
                wpsPin.add(p)
            } while (cursor.moveToNext())
            cursor.close()
            binding.WPSlist.isEnabled = true
        } catch (e: Exception) {
            data.add(ItemWps(null, getString(R.string.toast_no_pins_found), null, null))
            binding.WPSlist.isEnabled = false
        }
        binding.WPSlist.adapter = MyAdapterWps(this@WPSActivity, data)
        toastMessage(String.format(getString(R.string.selected_source), "WPA WPS TESTER"))
    }

    fun btnCustomPin(view: View?) {
        val alert = AlertDialog.Builder(this)
        alert.setTitle(getString(R.string.dialog_enter_custom_pin))
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        alert.setView(input)
        alert.setPositiveButton(getString(R.string.ok)) { dialog, which ->
            val bssdWPS = intent.extras!!.getString("variable1")
            val pin = input.text.toString()
            showMenu(bssdWPS, pin)
        }
        alert.setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog.cancel() }
        alert.show()
    }

    //Toast
    fun toastMessage(text: String?) {
        val toast = Toast.makeText(applicationContext,
                text, Toast.LENGTH_SHORT)
        toast.show()
    }

    companion object {
        private var context: Context? = null
        var API_READ_KEY: String? = ""
        private var listContextMenuItems = arrayOfNulls<String>(2)
    }
}