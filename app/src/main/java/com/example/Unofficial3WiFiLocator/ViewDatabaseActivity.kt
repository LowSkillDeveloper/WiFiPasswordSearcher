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
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT_DB && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    inputStream?.let { stream ->
                        val fileContents = stream.bufferedReader().use(BufferedReader::readText)
                        importDatabase(fileContents)
                        displayDatabaseInfo()
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
                wifiDatabaseHelper.addNetwork(essid, bssid, password, wpsCode)
            }
            Toast.makeText(this, getString(R.string.database_import_successful), Toast.LENGTH_SHORT).show()
            displayDatabaseInfo()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_importing_database) + ": ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            displayDatabaseInfo()
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

            return listItemView
        }
    }
}