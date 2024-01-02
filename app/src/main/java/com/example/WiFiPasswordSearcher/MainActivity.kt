package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.example.WiFiPasswordSearcher.databinding.ActivityMainBinding
import com.larvalabs.svgandroid.SVG
import com.larvalabs.svgandroid.SVGParser
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class APData {
    var bssid: String? = null
    var keys: ArrayList<String>? = null
    var generated: ArrayList<Boolean>? = null
    var wps: ArrayList<String>? = null
}

internal class MyScanResult {
    var bssid: String? = null
    var essid: String? = null
    var frequency = 0
    var level = 0
    var capabilities: String? = null
}

internal class WiFiListSimpleAdapter(private val context: Context, data: List<MutableMap<String, *>?>, resource: Int, from: Array<String>?, to: IntArray?) : SimpleAdapter(context, data, resource, from, to) {
    private val dataList: List<*>
    private fun deleteInTextTags(text: String): String {
        var text = text
        if (text.length > 2 && text.substring(0, 2) == "*[") {
            val stylePref = text.substring(2, text.indexOf("]*"))
            text = text.substring(stylePref.length + 4)
        }
        return text
    }

    private fun parseInTextTags(txtView: TextView) {
        val text = "" + txtView.text
        if (text.length > 2 && text.substring(0, 2) == "*[") {
            val stylePref = text.substring(2, text.indexOf("]*"))
            txtView.text = text.substring(stylePref.length + 4)
            if (stylePref.indexOf(":") > 0) {
                val style = stylePref.substring(0, stylePref.indexOf(":"))
                val value = stylePref.substring(stylePref.indexOf(":") + 1)
                if (style == "color") {
                    when (value) {
                        "red" -> txtView.setTextColor(Color.rgb(153, 0, 0))
                        "green" -> txtView.setTextColor(Color.GREEN)
                        "greendark" -> txtView.setTextColor(Color.rgb(0, 153, 76))
                        "blue" -> txtView.setTextColor(Color.BLUE)
                        "yellow" -> txtView.setTextColor(Color.rgb(153, 153, 0))
                        "gray" -> txtView.setTextColor(Color.rgb(105, 105, 105))
                    }
                }
            }
        } else {
            txtView.setTextColor(Color.WHITE)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val imgSec = view.findViewById<View>(R.id.imgSec) as ImageView
        val imgWPS = view.findViewById<View>(R.id.imgWps) as ImageView
        val elemWiFi: HashMap<String, String> = dataList[position] as HashMap<String, String>
        val capability = elemWiFi["CAPABILITY"]
        imgSec.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        imgWPS.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        var svgImg: SVG
        if (capability!!.contains("WPA2")) {
            var img = SvgImageCache["WPA2"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wpa2_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPA2"] = img
            }
            imgSec.setImageDrawable(img)
        } else if (capability.contains("WPA")) {
            var img = SvgImageCache["WPA"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wpa_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPA"] = img
            }
            imgSec.setImageDrawable(img)
        } else if (capability.contains("WEP")) {
            var img = SvgImageCache["WEP"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wep_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WEP"] = img
            }
            imgSec.setImageDrawable(img)
        } else {
            var img = SvgImageCache["OPEN"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.open_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["OPEN"] = img
            }
            imgSec.setImageDrawable(img)
        }
        if (capability.contains("WPS")) {
            var img = SvgImageCache["WPS"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wps_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPS"] = img
            }
            imgWPS.setImageDrawable(img)
        } else {
            imgWPS.setImageResource(android.R.color.transparent)
        }
        val txtKey = view.findViewById<View>(R.id.KEY) as TextView
        val txtSignal = view.findViewById<View>(R.id.txtSignal) as TextView
        val txtRowId = view.findViewById<View>(R.id.txtRowId) as TextView
        val txtKeysCount = view.findViewById<View>(R.id.txtKeysCount) as TextView
        val txtWPS = view.findViewById<View>(R.id.txtWPS) as TextView
        val llKeys = view.findViewById<View>(R.id.llKeys) as LinearLayout
        llKeys.setOnClickListener(onKeyClick)
        parseInTextTags(txtKey)
        parseInTextTags(txtSignal)
        parseInTextTags(txtKeysCount)
        parseInTextTags(txtWPS)
        val keysCount = txtKeysCount.text.toString().toInt()
        llKeys.isClickable = keysCount > 1
        txtRowId.text = position.toString()
        return view
    }

    private val onKeyClick = View.OnClickListener { v ->
        if (MyActivity.WiFiKeys == null || MyActivity.WiFiKeys!!.size == 0) return@OnClickListener
        val llRow = v.parent.parent as LinearLayout
        val txtRowId = llRow.findViewById<View>(R.id.txtRowId) as TextView
        val rowId = txtRowId.text.toString().toInt()
        val keys = MyActivity.WiFiKeys!![rowId].keys
        val wpss = MyActivity.WiFiKeys!![rowId].wps
        if (keys!!.size <= 1) return@OnClickListener
        val keysList = arrayOfNulls<String>(keys.size)
        for (i in keysList.indices) {
            val wps = wpss!![i]
            if (wps.isEmpty()) {
                keysList[i] = context.getString(R.string.dialog_choose_key_key) + deleteInTextTags(keys[i])
            } else {
                keysList[i] = context.getString(R.string.dialog_choose_key_key) + deleteInTextTags(keys[i]) + context.getString(R.string.dialog_choose_key_wps) + deleteInTextTags(wps)
            }
        }
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle(context.getString(R.string.dialog_choose_key))
        dialogBuilder.setItems(keysList) { dialog, item -> passwordChoose(rowId, item) }
        dialogBuilder.show()
    }

    private fun passwordChoose(rowID: Int, passId: Int) {
        var row: View? = null

        for (i in 0 until MyActivity.WiFiList!!.childCount) {
            row = MyActivity.WiFiList!!.getChildAt(i)
            val txtRowId = row.findViewById<View>(R.id.txtRowId) as TextView
            val rid = txtRowId.text.toString().toInt()
            row = if (rid != rowID) null else break
        }
        if (row == null) return
        val keys = MyActivity.WiFiKeys!![rowID].keys
        val gen = MyActivity.WiFiKeys!![rowID].generated
        val wps = MyActivity.WiFiKeys!![rowID].wps
        val chosenPassword = keys!![passId]
        val isGen = gen!![passId]
        val curWPS = wps!![passId]
        val keyColor: String
        keys[passId] = keys[0]
        keys[0] = chosenPassword
        gen[passId] = gen[0]
        gen[0] = isGen
        wps[passId] = wps[0]
        wps[0] = curWPS
        val txtKey = row.findViewById<View>(R.id.KEY) as TextView
        keyColor = if (isGen) "*[color:red]*" else "*[color:green]*"
        txtKey.text = keyColor + chosenPassword
        parseInTextTags(txtKey)
        val txtWPS = row.findViewById<View>(R.id.txtWPS) as TextView
        txtWPS.text = if (curWPS.isEmpty()) "*[color:gray]*[unknown]" else "*[color:blue]*$curWPS"
        parseInTextTags(txtWPS)
    }

    companion object {
        private val SvgImageCache = HashMap<String, Drawable?>()
    }

    init {
        dataList = data
    }
}

class MyActivity : AppCompatActivity() {
    /**
     * Called when the activity is first created.
     */
    private lateinit var binding: ActivityMainBinding

    private lateinit var mSettings: Settings
    private lateinit var user: UserManager
    private lateinit var wifiMgr: WifiManager
    private lateinit var locationMgr: LocationManager
    private lateinit var sClipboard: ClipboardManager
    private lateinit var lastWiFiClickItem: LinearLayout
    private lateinit var listContextMenuItems: Array<String>

    private fun refreshNetworkList() {
        if (ScanInProcess) return
        WiFiKeys?.clear()
        WiFiScanResult?.clear()
        val context = applicationContext
        val list = ArrayList<HashMap<String, String?>>()
        val adapter = SimpleAdapter(context, list, R.layout.row, arrayOf("ESSID", "BSSID"), intArrayOf(R.id.ESSID, R.id.BSSID))
        WiFiList!!.adapter = adapter
        scanAndShowWiFi()
    }

    private val fabCheckFromBaseOnClick = View.OnClickListener {
        if (ScanInProcess) return@OnClickListener
        if (WiFiKeys != null) WiFiKeys!!.clear()
        val dProcess = ProgressDialog(this@MyActivity)
        dProcess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dProcess.setMessage(resources.getString(R.string.status_searching))
        dProcess.setCanceledOnTouchOutside(false)
        binding.btnCheckFromBase.isEnabled = false
        dProcess.show()
        Thread(Runnable {
            checkFromBase()
            dProcess.dismiss()
        }).start()
    }

    val activity: Activity
        get() = this

    private fun getDataRowsFromLinLay(LinLay: LinearLayout?, Type: String): TextView? {
        when (Type) {
            "BSSID" -> return LinLay!!.findViewById<View>(R.id.BSSID) as TextView
            "ESSID" -> return LinLay!!.findViewById<View>(R.id.ESSID) as TextView
            "KEY" -> return LinLay!!.findViewById<View>(R.id.KEY) as TextView
        }
        return null
    }

    private var wifiListOnClick = OnItemClickListener { parent, linearLayout, position, id ->
        val item = linearLayout as LinearLayout
        lastWiFiClickItem = item
        val txtBSSID = getDataRowsFromLinLay(item, "BSSID")
        val txtESSID = getDataRowsFromLinLay(item, "ESSID")
        val dialogBuilder = AlertDialog.Builder(this@MyActivity)
        dialogBuilder.setTitle(if (txtESSID != null) txtESSID.text else "")
        val essdWps = txtESSID?.text?.toString() ?: ""
        val bssdWps = txtBSSID?.text?.toString() ?: ""
        dialogBuilder.setItems(listContextMenuItems, DialogInterface.OnClickListener { dialog, item ->
            val apdata: APData
            var needToast = false
            val scanResult = WiFiScanResult!![id.toInt()]
            when (item) {
                0 -> {
                    val detailsActivityIntent = Intent(this@MyActivity, WifiDetailsActivity::class.java)
                    val wifiInfo = HashMap<String, String?>()
                    wifiInfo["BSSID"] = scanResult.bssid
                    wifiInfo["SSID"] = scanResult.essid
                    wifiInfo["Freq"] = Integer.toString(scanResult.frequency)
                    wifiInfo["Signal"] = Integer.toString(scanResult.level)
                    wifiInfo["Capabilities"] = scanResult.capabilities
                    detailsActivityIntent.putExtra("WifiInfo", wifiInfo)
                    startActivity(detailsActivityIntent)
                }
                1 -> {
                    val txtBSSID = getDataRowsFromLinLay(lastWiFiClickItem, "ESSID")
                    val dataClip: ClipData
                    dataClip = ClipData.newPlainText("text", if (txtBSSID != null) txtBSSID.text else "")
                    sClipboard.setPrimaryClip(dataClip)
                    needToast = true
                }
                2 -> {
                    val txtESSID = getDataRowsFromLinLay(lastWiFiClickItem, "BSSID")
                    val dataClip = ClipData.newPlainText("text", if (txtESSID != null) txtESSID.text else "")
                    sClipboard.setPrimaryClip(dataClip)
                    needToast = true
                }
                3 -> run {
                    if (WiFiKeys!!.isEmpty()) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_no_data),
                                Toast.LENGTH_LONG)
                        toast.show()
                        return@run
                    }
                    apdata = WiFiKeys!![id.toInt()]
                    if (apdata.keys!!.size < 1) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_key_not_found), Toast.LENGTH_SHORT)
                        toast.show()
                        return@OnClickListener
                    }
                    val dataClip = ClipData.newPlainText("text", apdata.keys!![0])
                    sClipboard.setPrimaryClip(dataClip)
                    needToast = true
                }
                4 -> run {
                    if (WiFiKeys!!.isEmpty()) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_no_data),
                                Toast.LENGTH_LONG)
                        toast.show()
                        return@run
                    }
                    apdata = WiFiKeys!![id.toInt()]
                    if (apdata.keys!!.size < 1) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_key_not_found), Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    if (!wifiMgr.isWifiEnabled) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_wifi_disabled), Toast.LENGTH_SHORT)
                        toast.show()
                        return@run
                    }
                    val list = wifiMgr.configuredNetworks
                    var cnt = 0
                    for (wifi in list) {
                        if (wifi.SSID != null && wifi.SSID == "\"" + scanResult.essid + "\"") cnt++
                    }
                    if (cnt > 0) {
                        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> addNetworkProfile(scanResult, apdata)
                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                            dialog.dismiss()
                        }
                        val builder = AlertDialog.Builder(this@MyActivity)
                        builder.setTitle(getString(R.string.dialog_are_you_sure))
                                .setMessage(String.format(getString(R.string.dialog_already_stored), scanResult.essid, cnt))
                                .setPositiveButton(getString(R.string.dialog_yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.dialog_no), dialogClickListener).show()
                    } else addNetworkProfile(scanResult, apdata)
                }
                5 -> run {
                    if (!scanResult.capabilities!!.contains("WPS")) {
                        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> wpsGenStart(essdWps, bssdWps)
                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                            dialog.dismiss()
                        }
                        val builder = AlertDialog.Builder(this@MyActivity)
                        builder.setTitle(getString(R.string.dialog_are_you_sure))
                                .setMessage(String.format(getString(R.string.dialog_wps_disabled), scanResult.essid))
                                .setPositiveButton(getString(R.string.dialog_yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.dialog_no), dialogClickListener).show()
                        return@run
                    }
                    wpsGenStart(essdWps, bssdWps)
                }
            }
            if (needToast) {
                val toast = Toast.makeText(applicationContext,
                        getString(R.string.toast_copied), Toast.LENGTH_SHORT)
                toast.show()
            }
            dialog.dismiss()
        })
        dialogBuilder.show()
    }

    private fun wpsGenStart(essdWps: String, bssdWps: String) {
        val wpsActivityIntent = Intent(this@MyActivity, WPSActivity::class.java)
        wpsActivityIntent.putExtra("variable", essdWps)
        wpsActivityIntent.putExtra("variable1", bssdWps)
        startActivity(wpsActivityIntent)
    }

    private fun addNetworkProfile(scanResult: MyScanResult, apdata: APData) {
        val WifiCfg = WifiConfiguration()
        WifiCfg.BSSID = scanResult.bssid
        WifiCfg.SSID = String.format("\"%s\"", scanResult.essid)
        WifiCfg.hiddenSSID = false
        WifiCfg.priority = 1000
        if (scanResult.capabilities!!.contains("WEP")) {
            WifiCfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            WifiCfg.wepKeys[0] = String.format("\"%s\"", apdata.keys!![0])
            WifiCfg.wepTxKeyIndex = 0
        } else {
            WifiCfg.preSharedKey = String.format("\"%s\"", apdata.keys!![0])
        }
        val netId = wifiMgr.addNetwork(WifiCfg)
        when {
            netId > -1 -> toast(getString(R.string.toast_network_stored))
            wifiMgr.isWifiEnabled -> toast(getString(R.string.toast_failed_to_store))
            else -> toast(getString(R.string.toast_wifi_disabled))
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WIFI_ENABLE_REQUEST || requestCode == LOCATION_ENABLE_REQUEST) {
            scanAndShowWiFi()
        }
    }

    private fun apiDataTest() {
        if (!API_KEYS_VALID) {
            runOnUiThread {
                val t = Toast.makeText(applicationContext, getString(R.string.toast_enter_credentials), Toast.LENGTH_SHORT)
                t.show()
            }
            val startActivity = Intent(this, StartActivity::class.java)
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(startActivity)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(bottomAppBar)
        listContextMenuItems = resources.getStringArray(R.array.menu_network)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        APP_VERSION = resources.getString(R.string.app_version)
        mSettings = Settings(applicationContext)
        user = UserManager(applicationContext)
        API_READ_KEY = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        API_WRITE_KEY = mSettings.AppSettings!!.getString(Settings.API_WRITE_KEY, "")
        API_KEYS_VALID = mSettings.AppSettings!!.getBoolean(Settings.API_KEYS_VALID, false)
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshNetworkList()
            swipeRefreshLayout?.isRefreshing = false
        }
        wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        locationMgr = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        binding.btnCheckFromBase.setOnClickListener(fabCheckFromBaseOnClick)
        binding.WiFiList.onItemClickListener = wifiListOnClick
        if (adapter != null) {
            binding.WiFiList.adapter = adapter
            binding.btnCheckFromBase.isEnabled = true
        }
        if (ScanInProcess) {
            //   if(ScanWiFiReceiverIntent != null) unregisterReceiver(ScanWiFiReceiverIntent);
            //ScanAndShowWiFi();
        }
        scanAndShowWiFi()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottomappbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.action_refresh -> {
                refreshNetworkList()
            }
            R.id.action_monitor_network -> {
                val lay = LinearLayout(this@MyActivity)
                lay.orientation = LinearLayout.VERTICAL
                val ebss = EditText(this@MyActivity)
                ebss.hint = getString(R.string.hint_enter_bssid)
                ebss.inputType = InputType.TYPE_CLASS_TEXT
                lay.addView(ebss)
                val eess = EditText(this@MyActivity)
                eess.hint = getString(R.string.hint_enter_essid)
                eess.inputType = InputType.TYPE_CLASS_TEXT
                lay.addView(eess)
                val alert = AlertDialog.Builder(this@MyActivity)
                alert.setTitle(getString(R.string.dialog_network_properties))
                alert.setView(lay)
                alert.setPositiveButton(getString(R.string.ok)) { dialog, which ->
                    val detailsActivityIntent = Intent(this@MyActivity, WifiDetailsActivity::class.java)
                    val WifiInfo = HashMap<String, String>()
                    WifiInfo["BSSID"] = ebss.text.toString().toLowerCase()
                    WifiInfo["SSID"] = eess.text.toString()
                    WifiInfo["Freq"] = "0"
                    WifiInfo["Signal"] = "-100"
                    detailsActivityIntent.putExtra("WifiInfo", WifiInfo)
                    startActivity(detailsActivityIntent)
                }
                alert.setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog.cancel() }
                alert.show()
            }
            R.id.app_bar_settings -> {
                val intent = Intent(this@MyActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }

        return true
    }

    private fun Context.toast(message: CharSequence) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        toast.show()
    }

    private fun scanAndShowWiFi() {
        val comparator = Comparator<MyScanResult> { lhs, rhs -> if (lhs.level < rhs.level) 1 else if (lhs.level == rhs.level) 0 else -1 }
        WiFiScanResult = null
        adapter = null
        if (!wifiMgr.isWifiEnabled) {
            val action = android.provider.Settings.ACTION_WIFI_SETTINGS
            val builder = AlertDialog.Builder(this@MyActivity)
            builder.setTitle(getString(R.string.toast_wifi_disabled))
            builder.setMessage(getString(R.string.dialog_message_please_enable_wifi))
            builder.setPositiveButton(getString(R.string.button_open_settings)
            ) { d, id ->
                this@MyActivity.startActivityForResult(Intent(action), WIFI_ENABLE_REQUEST)
                d.dismiss()
            }.setNegativeButton(getString(R.string.cancel)
            ) { d, id -> d.cancel() }
            val alert = builder.create()
            alert.show()
            return
        }
        if (Build.VERSION.SDK_INT > 27 && !locationMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val action = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            val builder = AlertDialog.Builder(this@MyActivity)
            builder.setMessage(getString(R.string.dialog_message_location_disabled))
            builder.setPositiveButton(getString(R.string.button_open_settings)
            ) { d, id ->
                this@MyActivity.startActivityForResult(Intent(action), LOCATION_ENABLE_REQUEST)
                d.dismiss()
            }.setNegativeButton(getString(R.string.cancel)
            ) { d, id -> d.cancel() }
            val alert = builder.create()
            alert.show()
            return
        }
        val dProccess = ProgressDialog(this@MyActivity)
        dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dProccess.setMessage(getString(R.string.status_scanning))
        dProccess.setCanceledOnTouchOutside(false)
        dProccess.show()
        ScanWiFiReceiverIntent = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val res = wifiMgr.scanResults
                val results: MutableList<MyScanResult> = ArrayList()
                for (result in res) {
                    val sc = MyScanResult()
                    sc.bssid = result.BSSID
                    sc.essid = result.SSID
                    sc.level = result.level
                    sc.frequency = result.frequency
                    sc.capabilities = result.capabilities
                    results.add(sc)
                }
                Collections.sort(results, comparator)
                WiFiScanResult = results
                val list = ArrayList<HashMap<String, String?>?>()
                var ElemWiFi: HashMap<String, String?>
                for (result in results) {
                    ElemWiFi = HashMap()
                    ElemWiFi["ESSID"] = result.essid
                    ElemWiFi["BSSID"] = result.bssid!!.toUpperCase()
                    ElemWiFi["KEY"] = "*[color:gray]*[no data]"
                    ElemWiFi["WPS"] = "*[color:gray]*[no data]"
                    ElemWiFi["SIGNAL"] = getStrSignal(result.level)
                    ElemWiFi["KEYSCOUNT"] = "*[color:gray]*0"
                    ElemWiFi["CAPABILITY"] = result.capabilities
                    list.add(ElemWiFi)
                }
                adapter = WiFiListSimpleAdapter(activity, list, R.layout.row, arrayOf("ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"), intArrayOf(R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount))
                WiFiList!!.adapter = adapter
                ScanInProcess = false
                binding.btnCheckFromBase.isEnabled = true
                val toast = Toast.makeText(applicationContext,
                        getString(R.string.toast_scan_complete), Toast.LENGTH_SHORT)
                toast.show()
                unregisterReceiver(this)
                ScanWiFiReceiverIntent = null
                dProccess.dismiss()
            }
        }
        registerReceiver(ScanWiFiReceiverIntent, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        ScanInProcess = true
        binding.btnCheckFromBase.isEnabled = false
        wifiMgr.startScan()
    }

    private fun checkFromBase() {
        var bss = JSONObject()
        val reader: BufferedReader
        var readLine: String?
        val rawData = StringBuilder()
        val fetchESS: Boolean
        try {
            val query = JSONObject()
            query.put("key", API_READ_KEY)
            val bssids = JSONArray()
            val essids = JSONArray()
            for (result in WiFiScanResult!!) {
                bssids.put(result.bssid)
                essids.put(result.essid)
            }
            mSettings.Reload()
            fetchESS = mSettings.AppSettings!!.getBoolean(Settings.APP_FETCH_ESS, false)
            query.put("bssid", bssids)
            if (fetchESS) query.put("essid", essids)
            val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
            val uri = URL("$serverURI/api/apiquery")
            val connection = uri.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            val writer = DataOutputStream(
                    connection.outputStream)
            writer.writeBytes(query.toString())
            connection.readTimeout = 10 * 1000
            connection.connect()
            reader = BufferedReader(InputStreamReader(connection.inputStream))
            while (reader.readLine().also { readLine = it } != null) {
                rawData.append(readLine)
            }
            try {
                val json = JSONObject(rawData.toString())
                val ret = json.getBoolean("result")
                if (!ret) {
                    // API failure
                    val error = json.getString("error")
                    val errorDesc = user.getErrorDesc(error, this)
                    if (error == "loginfail") {
                        mSettings.Editor!!.putBoolean(Settings.API_KEYS_VALID, false)
                        mSettings.Editor!!.commit()
                        API_KEYS_VALID = false
                        apiDataTest()
                        return
                    }
                    runOnUiThread {
                        val t = Toast.makeText(applicationContext, errorDesc, Toast.LENGTH_SHORT)
                        t.show()
                        binding.btnCheckFromBase.isEnabled = true
                    }
                    return
                }
                if (!json.isNull("data")) {
                    bss = try {
                        json.getJSONObject("data")
                    } catch (e: Exception) {
                        // add empty object
                        JSONObject()
                    }
                }
            } catch (e: Exception) {
                // JSON error
                runOnUiThread {
                    val t = Toast.makeText(applicationContext, getString(R.string.toast_database_failure), Toast.LENGTH_SHORT)
                    t.show()
                    binding.btnCheckFromBase.isEnabled = true
                }
                return
            }
        } catch (e: Exception) {
            // Connection error
            runOnUiThread {
                val t = Toast.makeText(applicationContext, getString(R.string.status_no_internet), Toast.LENGTH_SHORT)
                t.show()
                binding.btnCheckFromBase.isEnabled = true
            }
            return
        }
        val list = ArrayList<HashMap<String, String?>?>()
        var elemWiFi: HashMap<String, String?>
        var keyColor: String
        var i = 0
        for (result in WiFiScanResult!!) {
            val apdata = getWiFiKeyByBSSID(bss, fetchESS, result.essid, result.bssid!!.toUpperCase())
            elemWiFi = HashMap()
            elemWiFi["ESSID"] = result.essid
            elemWiFi["BSSID"] = result.bssid!!.toUpperCase()
            elemWiFi["SIGNAL"] = getStrSignal(result.level)
            if (apdata.keys!!.size < 1) {
                elemWiFi["KEY"] = "*[color:gray]*[unknown]"
                elemWiFi["KEYSCOUNT"] = "*[color:gray]*" + apdata.keys!!.size.toString()
            } else {
                keyColor = if (apdata.generated!![0]) "*[color:red]*" else "*[color:green]*"
                elemWiFi["KEY"] = keyColor + apdata.keys!![0]
                elemWiFi["KEYSCOUNT"] = "*[color:green]*" + apdata.keys!!.size.toString()
            }
            if (apdata.wps!!.size < 1 || apdata.wps!![0].isEmpty()) {
                elemWiFi["WPS"] = "*[color:gray]*[unknown]"
            } else {
                elemWiFi["WPS"] = "*[color:blue]*" + apdata.wps!![0]
            }
            elemWiFi["CAPABILITY"] = result.capabilities
            list.add(elemWiFi)
            WiFiKeys!!.add(i, apdata)
            i++
        }
        adapter = WiFiListSimpleAdapter(activity, list, R.layout.row, arrayOf("ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"), intArrayOf(R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount))
        runOnUiThread(Thread(Runnable {
            WiFiList!!.adapter = adapter
            binding.btnCheckFromBase.isEnabled = true
        }
        ))
    }

    private fun keyWPSPairExists(keys: ArrayList<String>, pins: ArrayList<String>, key: String, pin: String): Boolean {
        for (i in keys.indices) {
            if (keys[i] == key && pins[i] == pin) return true
        }
        return false
    }

    private fun getWiFiKeyByBSSID(bss: JSONObject, fetchESS: Boolean, ESSID: String?, BSSID: String): APData {
        val keys = ArrayList<String>()
        val gen = ArrayList<Boolean>()
        val wpsPins = ArrayList<String>()
        try {
            val `val` = if (fetchESS) "$BSSID|$ESSID" else BSSID
            val successes = !bss.isNull(`val`)
            if (successes) {
                val rows = bss.getJSONArray(`val`)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val key = row.getString("key")
                    val wps = row.getString("wps")
                    if (keyWPSPairExists(keys, wpsPins, key, wps)) continue
                    keys.add(key)
                    wpsPins.add(wps)
                    gen.add(false)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        if (keys.size == 0) {
            val passiveKey = passiveVulnerabilityTest(ESSID, BSSID)
            if (!passiveKey.isEmpty()) {
                keys.add(passiveKey)
                gen.add(true)
                wpsPins.add("")
            }
        }
        val apdata = APData()
        apdata.bssid = BSSID
        apdata.keys = keys
        apdata.generated = gen
        apdata.wps = wpsPins
        return apdata
    }

    private fun passiveVulnerabilityTest(essid: String?, bssid: String): String {
        var ret = ""
        if (essid!!.length > 9) {
            if (essid.substring(0, 9) == "MGTS_GPON") {
                ret = bssid.replace(":", "")
                ret = ret.substring(4, 12)
            }
        }
        return ret
    }

    private fun getStrSignal(Signal: Int): String {
        var signal = Signal
        var color = ""
        signal = (100 + signal) * 2
        signal = Math.max(signal, 0).coerceAtMost(100)
        if (signal < 48) color = "*[color:red]*"
        if (signal in 48..64) color = "*[color:yellow]*"
        if (signal >= 65) color = "*[color:greendark]*"
        return "$color$signal%"
    }

    companion object {
        var APP_VERSION = ""
        var API_READ_KEY: String? = ""
        var API_WRITE_KEY: String? = ""
        var API_KEYS_VALID = false
        var WiFiList: ListView? = null
        private var WiFiScanResult: MutableList<MyScanResult>? = null
        var WiFiKeys: ArrayList<APData>? = ArrayList()
        private var ScanInProcess = false
        private var ScanWiFiReceiverIntent: BroadcastReceiver? = null
        private var adapter: WiFiListSimpleAdapter? = null
        const val WIFI_ENABLE_REQUEST = 1
        const val LOCATION_ENABLE_REQUEST = 2
    }
}