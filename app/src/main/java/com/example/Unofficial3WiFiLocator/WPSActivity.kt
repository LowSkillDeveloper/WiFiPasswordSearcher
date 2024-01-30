package com.example.Unofficial3WiFiLocator

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.ConnectivityManager
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
import com.example.Unofficial3WiFiLocator.databinding.ActivityWpsBinding
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
import android.preference.PreferenceManager
import org.apache.http.HttpRequestInterceptor
import org.jsoup.Jsoup
import java.io.FileOutputStream
import eu.chainfire.libsuperuser.Shell


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
        setAppTheme()
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
        val dbName = "vendor.db"
        val dbPath = getDatabasePath(this, dbName)
        copyDatabaseFromAssets(this, dbName, dbPath)
        API_READ_KEY = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        val essdWPS = intent.extras!!.getString("variable")
        binding.ESSDWpsTextView.text = essdWPS // ESSID
        val bssdWPS = intent.extras!!.getString("variable1")
        binding.BSSDWpsTextView.text = bssdWPS // BSSID
        wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wpsCallback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            wpsCallback = object : WpsCallback() {
                @Deprecated("Deprecated in Java")
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

                @Deprecated("Deprecated in Java")
                override fun onSucceeded() {
                    if (!wpsConnecting) return
                    wpsConnecting = false
                    pd.dismiss()
                    Toast.makeText(applicationContext, getString(R.string.toast_connected_successfully), Toast.LENGTH_SHORT).show()
                }

                @Deprecated("Deprecated in Java")
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

    private fun setAppTheme() {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val isDarkMode = sharedPref.getBoolean("DARK_MODE", false)
        if (isDarkMode) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }
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
                2 -> {
                    connectWithWpsRoot(BSSID, pin)
                }
            }
        }
        dialogBuilder.show()
    }

    private fun connectWithWpsRoot(BSSID: String?, pin: String?) {
        if (Shell.SU.available()) {
            val command = "wpa_cli -i wlan0 wps_pin $BSSID $pin"
            val result = Shell.SU.run(command)

            if (result != null) {

                Toast.makeText(applicationContext, getString(R.string.wps_connection_initiated_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, getString(R.string.error_executing_wps_command), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(applicationContext, getString(R.string.root_access_unavailable), Toast.LENGTH_SHORT).show()
        }
    }

    private inner class AsyncInitActivity : AsyncTask<String, Void?, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            pd = ProgressDialog.show(this@WPSActivity, getString(R.string.status_please_wait), getString(R.string.status_initializing))
        }

        override fun doInBackground(vararg BSSDWps: String): String {
            val BSSID = BSSDWps[0]
            var response: String
            try {
                val url = "https://wpsfinder.com/ethernet-wifi-brand-lookup/MAC:$BSSID"
                val doc = Jsoup.connect(url).get()
                val element = doc.select("h4.text-muted > center").first()
                response = element?.text() ?: "N/A"
            } catch (e: Exception) {
                response = "N/A"
            }
            return response
        }
        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
             pd.dismiss()
            val response = if (result.length > 50 || result == "N/A") {
                "unknown vendor"
            } else {
                result
            }
            binding.VendorWpsTextView.text = "Online DB: $response"
            when (mSettings.AppSettings!!.getInt(Settings.WPS_SOURCE, 1)) {
                1 -> btnwpsbaseclick(null)
                2 -> btnGenerate(null)
                3 -> btnLocalClick(null)
            }
        }
    }

    private fun copyDatabaseFromAssets(context: Context, dbName: String, dbPath: String) {
        try {
            val inputStream = context.assets.open(dbName)
            val outputStream = FileOutputStream(dbPath)
            val buffer = ByteArray(1024)
            while (inputStream.read(buffer) > 0) {
                outputStream.write(buffer)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getDatabasePath(context: Context, dbName: String): String {
        return context.getDatabasePath(dbName).path
    }
    private fun checkVendorFromLocalDB(BSSID: String) {
        try {
            val formattedBSSID = BSSID.replace(":", "").substring(0, 6).uppercase(Locale.getDefault())
            val dbFile = getDatabasePath("vendor.db")
            val localDB = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = localDB.rawQuery("SELECT vendor FROM oui WHERE mac = ?", arrayOf(formattedBSSID))
            if (cursor.moveToFirst()) {
                val vendor = cursor.getString(cursor.getColumnIndex("vendor"))
                binding.VendorLocalDBTextView.text = "Local DB: $vendor"
            } else {
                binding.VendorLocalDBTextView.text = "Local DB: unknown vendor"
            }
            cursor.close()
            localDB.close()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.VendorLocalDBTextView.text = "Local DB: Error accessing database"
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

            if (isNetworkAvailable()) {
                val hc = DefaultHttpClient()
                hc.addRequestInterceptor(HttpRequestInterceptor { request, context ->
                    val useCustomHost = mSettings.AppSettings!!.getBoolean("USE_CUSTOM_HOST", false)
                    val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
                    if (useCustomHost && serverURI?.startsWith("http://134.0.119.34") == true) {
                        request.setHeader("Host", "3wifi.stascorp.com")
                    }
                })

                val res: ResponseHandler<String> = BasicResponseHandler()
                mSettings.Reload()
                val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
                val http = HttpGet("$serverURI/api/apiwps?key=$API_READ_KEY&bssid=$BSSID")
                try {
                    response = hc.execute(http, res)
                    saveToSharedPreferences(BSSID, response)
                } catch (e: Exception) {
                    response = "http_error"
                }
            } else {
                response = readFromSharedPreferences(BSSID) ?: "no_cached_data"
            }

            return processResponse(response, BSSID)
        }

        override fun onPostExecute(str: String) {
            pd.dismiss()
            var msg = ""
            var toast = true
            when (str) {
                "http_error" -> {
                    msg = getString(R.string.status_no_internet)
                    toast = false
                }
                "json_error" -> {
                    msg = getString(R.string.connection_failure)
                    toast = false
                }
                "api_error" -> {
                    msg = getString(R.string.toast_database_failure)
                    toast = false
                }
                "no_cached_data" -> {
                    msg = getString(R.string.no_cached_data)
                    toast = false
                }
                else -> {
                    if (data.isEmpty()) {
                        msg = getString(R.string.toast_no_pins_found)
                    }
                }
            }
            if (msg.isNotEmpty()) {
                data.add(ItemWps(null, msg, null, null))
            }
            binding.WPSlist.isEnabled = msg.isEmpty()
            binding.WPSlist.adapter = MyAdapterWps(this@WPSActivity, data)
            if (toast) toastMessage(String.format(getString(R.string.selected_source), "3WiFi Online WPS PIN"))
        }

        private fun processResponse(response: String, BSSID: String): String {
            try {
                val jObject = JSONObject(response)
                val result = jObject.getBoolean("result")
                if (result) {
                    try {
                        var dataObject = jObject.getJSONObject("data")
                        dataObject = dataObject.getJSONObject(BSSID)
                        val array = dataObject.optJSONArray("scores")
                        for (i in 0 until array.length()) {
                            var scoreObject = array.getJSONObject(i)
                            wpsPin.add(scoreObject.getString("value"))
                            wpsMet.add(scoreObject.getString("name"))
                            wpsScore.add(scoreObject.getString("score"))
                            wpsDb.add(if (scoreObject.getBoolean("fromdb")) "✔" else "")
                            val score = Math.round(wpsScore[i].toFloat() * 100)
                            wpsScore[i] = Integer.toString(score) + "%"
                            data.add(ItemWps(wpsPin[i], wpsMet[i], wpsScore[i], wpsDb[i]))
                        }
                    } catch (ignored: JSONException) {
                    }
                    return "success"
                } else {
                    return "api_error"
                }
            } catch (e: JSONException) {
                return "json_error"
            }
        }

        private fun isNetworkAvailable(): Boolean {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }

        private fun saveToSharedPreferences(key: String, data: String) {
            val sharedPref = getSharedPreferences("WPS_CACHE", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(key, data)
                apply()
            }
        }

        private fun readFromSharedPreferences(key: String): String? {
            val sharedPref = getSharedPreferences("WPS_CACHE", Context.MODE_PRIVATE)
            return sharedPref.getString(key, null)
        }
    }

    fun btnwpsbaseclick(view: View?) { //пины из базы
        binding.baseButton.background.setColorFilter(Color.parseColor("#1cd000"), PorterDuff.Mode.MULTIPLY)
        binding.wpsButton1.background.clearColorFilter()
        binding.wpsButton2.background.clearColorFilter()
        mSettings.Editor!!.putInt(Settings.WPS_SOURCE, 1)
        mSettings.Editor!!.commit()
        val bssdWPS = intent.extras!!.getString("variable1")
        bssdWPS?.let {
            checkVendorFromLocalDB(it)
        }
        GetPinsFromBase().execute(bssdWPS)
    }

    inner class myJavascriptInterface {
        @JavascriptInterface
        fun initAlgos(json: String?, bssid: String) {
            pins.clear()
            try {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val pin = WPSPin(obj.getInt("mode"), obj.getString("name"))
                    pins.add(pin)
                }
                binding.webView.post {
                    binding.webView.loadUrl("javascript:window.JavaHandler.getPins(1,JSON.stringify(pinSuggestAPI(true,'$bssid',null)), '$bssid');")
                }
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
                    val pin = if (all > 0) pins[i] else pins[obj.getInt("algo")]
                    pin.pin = obj.getString("pin")
                    pin.sugg = all <= 0
                }
                if (all > 0) {
                    binding.webView.post {
                        binding.webView.loadUrl("javascript:window.JavaHandler.getPins(0,JSON.stringify(pinSuggestAPI(false,'$bssid',null)), '');")
                    }
                } else {
                    wpsReady = true
                }
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
        val bssdWPS = intent.extras!!.getString("variable1")
        bssdWPS?.let {
            checkVendorFromLocalDB(it)
        }
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
        toastMessage(String.format(getString(R.string.selected_source), "3WIFI OFFLINE WPS PIN Companion"))
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
        bssdWPS?.let {
            checkVendorFromLocalDB(it)
        }
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
        toastMessage(String.format(getString(R.string.selected_source), "OFFLINE WPS PIN GENERATOR"))
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