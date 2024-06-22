package com.example.Unofficial3WiFiLocator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import eu.chainfire.libsuperuser.Shell

class ViewWifiPasswordsActivity : AppCompatActivity() {

    private lateinit var wifiDatabaseHelper: WiFiDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_wifi_passwords)

        wifiDatabaseHelper = WiFiDatabaseHelper(this)

        val listView: ListView = findViewById(R.id.listViewWifiPasswords)

        val result = Shell.SU.run("cat /data/misc/wifi/*.conf")
        val wifiList = mutableListOf<String>()

        if (result != null && result.isNotEmpty()) {
            val regexSSID = Regex("""ssid="(.+?)"""")
            val regexPSK = Regex("""psk="(.+?)"""")

            var ssid: String? = null
            var psk: String? = null

            for (line in result) {
                regexSSID.find(line)?.let {
                    ssid = it.groupValues[1]
                }
                regexPSK.find(line)?.let {
                    psk = it.groupValues[1]
                }

                if (ssid != null && psk != null) {
                    wifiList.add("SSID: $ssid\nPassword: $psk")
                    ssid = null
                    psk = null
                }
            }
        } else {
            wifiList.add(getString(R.string.no_wifi_passwords_found))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, wifiList)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            val selected = wifiList[position]
            val ssid = selected.substringAfter("SSID: ").substringBefore("\n")
            val password = selected.substringAfter("Password: ")

            showPopupMenu(view, ssid, password)
        }
    }

    private fun showPopupMenu(view: View, ssid: String, password: String) {
        val popupMenu = PopupMenu(this, view)
        val inflater: MenuInflater = popupMenu.menuInflater
        inflater.inflate(R.menu.menu_wifi_options, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_copy_ssid -> {
                    copyToClipboard(getString(R.string.copy_ssid), ssid)
                    true
                }
                R.id.action_copy_password -> {
                    copyToClipboard(getString(R.string.copy_password), password)
                    true
                }
                R.id.action_share -> {
                    shareWifiDetails(ssid, password)
                    true
                }
                R.id.action_add_to_db -> {
                    addToLocalDatabase(ssid, "", password, "")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun addToLocalDatabase(essid: String, bssid: String, password: String, wps: String) {
        wifiDatabaseHelper.addNetwork(essid, bssid, password, wps, "", "")
        runOnUiThread {
            Toast.makeText(this, getString(R.string.network_added), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareWifiDetails(ssid: String, password: String) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "SSID: $ssid\nPassword: $password")
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
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
}
