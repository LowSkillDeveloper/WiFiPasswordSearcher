package com.example.Unofficial3WiFiLocator

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.preference.PreferenceManager
import android.os.Environment
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.os.Build
import java.io.BufferedReader


class ViewDatabaseActivity : Activity() {
    companion object {
        private const val REQUEST_CODE_IMPORT_DB = 1
    }
    private lateinit var listView: ListView
    private lateinit var wifiDatabaseHelper: WiFiDatabaseHelper
    private lateinit var menuButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_database)

        listView = findViewById(R.id.list_view_database)
        wifiDatabaseHelper = WiFiDatabaseHelper(this)

        val menuButton: ImageButton = findViewById(R.id.menu_button)
        menuButton.setOnClickListener { view ->
            showPopupMenu(view)
        }

        displayDatabaseInfo()

        listView.setOnItemClickListener { _, _, position, _ ->
            val network = listView.adapter.getItem(position) as APData
            showNetworkOptionsDialog(network)
        }
    }

    private fun showNetworkOptionsDialog(network: APData) {
        val options = arrayListOf<String>()
        options.add(getString(R.string.copy_essid))
        options.add(getString(R.string.copy_bssid))
        network.keys?.let {
            if (it.isNotEmpty()) options.add(getString(R.string.copy_password))
        }
        network.wps?.let {
            if (it.isNotEmpty()) {
                options.add(getString(R.string.copy_wps_pin))
                options.add(getString(R.string.connect_using_wps))
            }
        }
        options.add(getString(R.string.generate_wps_pin))

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(network.essid ?: getString(R.string.unknown_network))
        dialogBuilder.setItems(options.toTypedArray()) { _, which ->
            when (options[which]) {
                getString(R.string.copy_essid) -> copyToClipboard("ESSID", network.essid ?: "")
                getString(R.string.copy_bssid) -> copyToClipboard("BSSID", network.bssid ?: "")
                getString(R.string.copy_password) -> copyToClipboard("Password", network.keys?.joinToString(", ") ?: "")
                getString(R.string.copy_wps_pin) -> copyToClipboard("WPS PIN", network.wps?.joinToString(", ") ?: "")
                getString(R.string.connect_using_wps) -> connectUsingWPS(network)
                getString(R.string.generate_wps_pin) -> {
                    val intent = Intent(this, WPSActivity::class.java)
                    intent.putExtra("variable", network.essid)
                    intent.putExtra("variable1", network.bssid)
                    startActivity(intent)
                }
            }
        }
        dialogBuilder.show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.label_copied_to_clipboard, label), Toast.LENGTH_SHORT).show()
    }

    private fun connectUsingWPS(network: APData) {
        network.wps?.firstOrNull()?.let { wpsPin ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val wpsConfig = WpsInfo().apply {
                    setup = WpsInfo.KEYPAD
                    this.pin = wpsPin
                }

                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.startWps(wpsConfig, object : WifiManager.WpsCallback() {
                    override fun onStarted(pin: String) {
                        Toast.makeText(applicationContext, getString(R.string.wps_connection_started), Toast.LENGTH_SHORT).show()
                    }

                    override fun onSucceeded() {
                        Toast.makeText(applicationContext, getString(R.string.wps_connection_succeeded), Toast.LENGTH_SHORT).show()
                    }

                    override fun onFailed(reason: Int) {
                        Toast.makeText(applicationContext, getString(R.string.wps_connection_failed, reason), Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(applicationContext, getString(R.string.wps_not_supported), Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(applicationContext, getString(R.string.no_wps_pin_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.database_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.export_database -> {
                    exportDatabase()
                    true
                }
                R.id.import_database -> {
                    selectImportFile()
                    true
                }
                R.id.clear_database -> {
                    clearDatabase()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun showImportDialog(jsonContents: String) {
        val options = arrayOf(getString(R.string.replace_database), getString(R.string.update_existing_database))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_import_type))
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    wifiDatabaseHelper.clearAllNetworks()
                    importDatabase(jsonContents)
                }
                1 -> {
                    importDatabase(jsonContents)
                }
            }
        }
        builder.show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT_DB && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    inputStream?.let { stream ->
                        val fileContents = stream.bufferedReader().use(BufferedReader::readText)
                        showImportDialog(fileContents)
                    } ?: throw Exception(getString(R.string.failed_to_open_file))
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.error_importing_file) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
    private fun selectImportFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "application/json"
        val downloadsFolderUri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path)
        intent.setDataAndType(downloadsFolderUri, "application/json")

        startActivityForResult(intent, REQUEST_CODE_IMPORT_DB)
    }

    private fun importDatabase(jsonContents: String) {
        try {
            val jsonArray = JSONArray(jsonContents)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val essid = jsonObject.optString("essid")
                val bssid = jsonObject.optString("bssid")
                val password = jsonObject.optString("password")
                val wpsCode = jsonObject.optString("wpsCode")
                val adminLogin = jsonObject.optString("adminLogin")
                val adminPass = jsonObject.optString("adminPass")
                if (!wifiDatabaseHelper.networkExists(bssid, password, wpsCode, adminLogin, adminPass)) {
                    wifiDatabaseHelper.addNetwork(essid, bssid, password, wpsCode, adminLogin, adminPass)
                }
            }

            displayDatabaseInfo()
            Toast.makeText(this, getString(R.string.database_import_successful), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_importing_database) + ": ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun clearDatabase() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.clear_database_txt))
        builder.setMessage(getString(R.string.Are_you_sure_clear_database))
        builder.setPositiveButton(getString(R.string.dialog_yes)) { dialog, which ->
            wifiDatabaseHelper.clearAllNetworks()
            displayDatabaseInfo()
            toast(getString(R.string.database_cleared_successfully))
        }
        builder.setNegativeButton(getString(R.string.dialog_no)) { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun exportDatabase() {
        val wifiList = wifiDatabaseHelper.getAllNetworks()
        val jsonArray = JSONArray()
        wifiList.forEach { network ->
            val jsonObject = JSONObject().apply {
                put("essid", network.essid)
                put("bssid", network.bssid)
                put("password", network.keys?.joinToString(", "))
                put("wps", network.wps?.joinToString(", "))
                put("adminLogin", network.adminLogin)
                put("adminPass", network.adminPass)
            }
            jsonArray.put(jsonObject)
        }
        try {
            val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val file = File(folder, "wifi_database_export.json")
            val fileWriter = FileWriter(file)
            fileWriter.write(jsonArray.toString(4))
            fileWriter.close()
            toast(getString(R.string.database_exported_successfully_to) + " ${file.absolutePath}")
        } catch (e: Exception) {
            toast(getString(R.string.error_exporting_database) + ": ${e.message}")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
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

    private fun displayDatabaseInfo() {
        val wifiList = wifiDatabaseHelper.getAllNetworks()
        val adapter = WiFiNetworkAdapter(this, wifiList)
        listView.adapter = adapter
    }

    private class WiFiNetworkAdapter(context: Activity, wifiNetworks: List<APData>) :
        ArrayAdapter<APData>(context, 0, wifiNetworks) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var listItemView = convertView
            if (listItemView == null) {
                listItemView = LayoutInflater.from(context).inflate(R.layout.wifi_network_item, parent, false)
            }

            val currentNetwork = getItem(position)

            val ssidTextView = listItemView!!.findViewById<TextView>(R.id.ssid_text_view)
            ssidTextView.text = currentNetwork?.essid

            val bssidTextView = listItemView.findViewById<TextView>(R.id.bssid_text_view)
            bssidTextView.text = currentNetwork?.bssid

            val passwordTextView = listItemView.findViewById<TextView>(R.id.password_text_view)
            passwordTextView.text = currentNetwork?.keys?.joinToString(", ")

            val wpsTextView = listItemView.findViewById<TextView>(R.id.wps_text_view)
            wpsTextView.text = currentNetwork?.wps?.joinToString(", ")

            val adminLoginTextView = listItemView.findViewById<TextView>(R.id.admin_login_text_view)
            adminLoginTextView.text = currentNetwork?.adminLogin

            val adminPassTextView = listItemView.findViewById<TextView>(R.id.admin_pass_text_view)
            adminPassTextView.text = currentNetwork?.adminPass

            return listItemView
        }
    }
}