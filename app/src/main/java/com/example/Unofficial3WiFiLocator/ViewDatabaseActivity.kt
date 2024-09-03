package com.example.Unofficial3WiFiLocator

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CursorAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter

class ViewDatabaseActivity : Activity() {
    companion object {
        private const val REQUEST_CODE_IMPORT_DB = 1
        private const val REQUEST_CODE_IMPORT_ROUTERSCAN = 2
        private const val REQUEST_CODE_IMPORT_CSV = 3
        private const val REQUEST_CODE_RESTORE_DB = 4
    }
    private lateinit var searchField: EditText
    private lateinit var listView: ListView
    private lateinit var wifiDatabaseHelper: WiFiDatabaseHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_database)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            performSearch()
        }


        listView = findViewById(R.id.list_view_database)
        wifiDatabaseHelper = WiFiDatabaseHelper(this)
        searchField = findViewById(R.id.search_field)

        val menuButton: ImageButton = findViewById(R.id.menu_button)
        menuButton.setOnClickListener { view ->
            showPopupMenu(view)
        }

        displayDatabaseInfo()

        listView.setOnItemClickListener { _, _, position, _ ->
            val adapter = listView.adapter
            if (adapter is WiFiCursorAdapter) {
                val cursor = adapter.cursor
                if (cursor.moveToPosition(position)) {
                    val network = APData().apply {
                        id = cursor.getInt(cursor.getColumnIndex("ID"))
                        essid = cursor.getString(cursor.getColumnIndexOrThrow("WiFiName"))
                        bssid = cursor.getString(cursor.getColumnIndexOrThrow("MACAddress"))
                        keys = arrayListOf(cursor.getString(cursor.getColumnIndexOrThrow("WiFiPassword")))
                        wps = arrayListOf(cursor.getString(cursor.getColumnIndexOrThrow("WPSCode")))
                        adminLogin = cursor.getString(cursor.getColumnIndexOrThrow("AdminLogin"))
                        adminPass = cursor.getString(cursor.getColumnIndexOrThrow("AdminPass"))
                    }
                    showNetworkOptionsDialog(network)
                }
            } else if (adapter is WiFiNetworkAdapter) {
                val network = adapter.getItem(position)
                network?.let { showNetworkOptionsDialog(it) }
            }
        }

        searchField = findViewById<EditText>(R.id.search_field)

    }

    private fun performSearch() {
        val searchQuery = searchField.text.toString()
        val filteredList = wifiDatabaseHelper.getAllNetworks().filter {
            it.essid?.contains(searchQuery, ignoreCase = true) == true ||
                    it.bssid?.contains(searchQuery, ignoreCase = true) == true ||
                    it.keys?.any { key -> key.contains(searchQuery, ignoreCase = true) } == true ||
                    it.wps?.any { pin -> pin.contains(searchQuery, ignoreCase = true) } == true ||
                    it.adminLogin?.contains(searchQuery, ignoreCase = true) == true ||
                    it.adminPass?.contains(searchQuery, ignoreCase = true) == true
        }
        updateListView(filteredList)
    }

    private class WiFiCursorAdapter(context: Context, cursor: Cursor) : CursorAdapter(context, cursor, 0) {
        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            return LayoutInflater.from(context).inflate(R.layout.wifi_network_item, parent, false)
        }

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val ssidTextView = view.findViewById<TextView>(R.id.ssid_text_view)
            val bssidTextView = view.findViewById<TextView>(R.id.bssid_text_view)
            val passwordTextView = view.findViewById<TextView>(R.id.password_text_view)
            val wpsTextView = view.findViewById<TextView>(R.id.wps_text_view)
            val adminLoginTextView = view.findViewById<TextView>(R.id.admin_login_text_view)
            val adminPassTextView = view.findViewById<TextView>(R.id.admin_pass_text_view)

            ssidTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("WiFiName"))
            bssidTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("MACAddress"))
            passwordTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("WiFiPassword"))
            wpsTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("WPSCode"))
            adminLoginTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("AdminLogin"))
            adminPassTextView.text = cursor.getString(cursor.getColumnIndexOrThrow("AdminPass"))
        }
    }

    private fun filterData(searchQuery: String) {
        val filteredList = wifiDatabaseHelper.getAllNetworks().filter {
            it.essid?.contains(searchQuery, ignoreCase = true) == true ||
                    it.bssid?.contains(searchQuery, ignoreCase = true) == true ||
                    it.keys?.any { key -> key.contains(searchQuery, ignoreCase = true) } == true ||
                    it.wps?.any { pin -> pin.contains(searchQuery, ignoreCase = true) } == true ||
                    it.adminLogin?.contains(searchQuery, ignoreCase = true) == true ||
                    it.adminPass?.contains(searchQuery, ignoreCase = true) == true
        }
        updateListView(filteredList)
    }

    private fun updateListView(networks: List<APData>) {
        val adapter = WiFiNetworkAdapter(this, networks)
        listView.adapter = adapter
    }

    private fun showNetworkOptionsDialog(network: APData) {
        val options = arrayListOf<String>()

        network.essid?.let {
            if (it.isNotEmpty()) options.add(getString(R.string.copy_essid))
        }
        network.bssid?.let {
            if (it.isNotEmpty()) options.add(getString(R.string.copy_bssid))
        }
        network.keys?.let {
            if (it.any { key -> key.isNotEmpty() }) options.add(getString(R.string.copy_password))
        }
        network.wps?.let {
            if (it.any { pin -> pin.isNotEmpty() }) {
                options.add(getString(R.string.copy_wps_pin))
                options.add(getString(R.string.connect_using_wps))
            }
        }
        network.adminLogin?.let {
            if (it.isNotEmpty()) options.add(getString(R.string.copy_router_login))
        }
        network.adminPass?.let {
            if (it.isNotEmpty()) options.add(getString(R.string.copy_router_password))
        }

        options.add(getString(R.string.generate_wps_pin))
        options.add(getString(R.string.edit_network))
        options.add(getString(R.string.delete_network))

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(network.essid ?: getString(R.string.unknown_network))
        dialogBuilder.setItems(options.toTypedArray()) { _, which ->
            val selectedOption = options[which]
            handleOtherOptions(selectedOption, network)
        }
        dialogBuilder.show()
    }

    private fun handleOtherOptions(selectedOption: String, network: APData) {
        when (selectedOption) {
            getString(R.string.copy_essid) -> network.essid?.let { copyToClipboard("ESSID", it) }
            getString(R.string.copy_bssid) -> network.bssid?.let { copyToClipboard("BSSID", it) }
            getString(R.string.copy_password) -> network.keys?.joinToString(", ")?.let { copyToClipboard("Password", it) }
            getString(R.string.copy_wps_pin) -> network.wps?.joinToString(", ")?.let { copyToClipboard("WPS PIN", it) }
            getString(R.string.connect_using_wps) -> connectUsingWPS(network)
            getString(R.string.copy_router_login) -> network.adminLogin?.let { copyToClipboard("Router Login", it) }
            getString(R.string.copy_router_password) -> network.adminPass?.let { copyToClipboard("Router Password", it) }
            getString(R.string.generate_wps_pin) -> {
                val intent = Intent(this, WPSActivity::class.java)
                intent.putExtra("variable", network.essid)
                intent.putExtra("variable1", network.bssid)
                startActivity(intent)
            }
            getString(R.string.edit_network) -> showEditNetworkDialog(network)
            getString(R.string.delete_network) -> showDeleteConfirmationDialog(network)
        }
    }


    private fun showEditNetworkDialog(network: APData) {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_edit_network, null)

        val ssidInput = view.findViewById<EditText>(R.id.ssid_input).apply { setText(network.essid) }
        val bssidInput = view.findViewById<EditText>(R.id.bssid_input).apply { setText(network.bssid) }
        val passwordInput = view.findViewById<EditText>(R.id.password_input).apply { setText(network.keys?.joinToString(", ")) }
        val wpsInput = view.findViewById<EditText>(R.id.wps_input).apply { setText(network.wps?.joinToString(", ")) }
        val adminLoginInput = view.findViewById<EditText>(R.id.admin_login_input).apply { setText(network.adminLogin) }
        val adminPassInput = view.findViewById<EditText>(R.id.admin_pass_input).apply { setText(network.adminPass) }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(view)
        dialogBuilder.setTitle(getString(R.string.edit_network))
        dialogBuilder.setPositiveButton(getString(R.string.save)) { dialog, _ ->
            network.essid = ssidInput.text.toString()
            network.bssid = bssidInput.text.toString()
            network.keys = arrayListOf(passwordInput.text.toString())
            network.wps = arrayListOf(wpsInput.text.toString())
            network.adminLogin = adminLoginInput.text.toString()
            network.adminPass = adminPassInput.text.toString()

            wifiDatabaseHelper.updateNetwork(network)
            displayDatabaseInfo()
        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel), null)
        dialogBuilder.create().show()
    }


    private fun showDeleteConfirmationDialog(network: APData) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_deletion_message, network.essid ?: ""))
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                deleteNetwork(network.bssid ?: "")
                Toast.makeText(this, R.string.network_deleted, Toast.LENGTH_SHORT).show()
                displayDatabaseInfo()
            }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    private fun deleteNetwork(bssid: String) {
        val db = wifiDatabaseHelper.writableDatabase
        val selection = "${WiFiDatabaseHelper.COLUMN_MAC_ADDRESS} LIKE ?"
        val selectionArgs = arrayOf(bssid)
        db.delete(WiFiDatabaseHelper.TABLE_NAME, selection, selectionArgs)
        db.close()
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
                    exportDatabaseAsync()
                    true
                }
                R.id.import_database -> {
                    selectImportFile()
                    true
                }
                R.id.import_routerscan -> {
                    selectImportFileFromRouterScan()
                    true
                }
                R.id.clear_database -> {
                    clearDatabase()
                    true
                }
                R.id.add_network_manually -> {
                    showAddNetworkDialog()
                    true
                }
                R.id.optimize_database -> {
                    wifiDatabaseHelper.vacuumDatabase()
                    Toast.makeText(this, getString(R.string.database_optimized), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.remove_duplicates -> {
                    wifiDatabaseHelper.removeDuplicates()
                    displayDatabaseInfo()
                    Toast.makeText(this, getString(R.string.duplicates_removed), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.export_to_csv -> {
                    exportDatabaseToCSV()
                    true
                }
                R.id.import_from_csv -> {
                    selectImportFileCSV()
                    true
                }
                R.id.backup_database -> {
                    backupDatabase()
                    true
                }
                R.id.restore_database -> {
                    selectRestoreFile()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun backupDatabase() {
        val backupDBPath = getDatabasePath(WiFiDatabaseHelper.DATABASE_NAME).absolutePath
        val backupFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wifi_database_backup.db")

        try {
            File(backupDBPath).copyTo(backupFile, overwrite = true)
            Toast.makeText(this, getString(R.string.backup_successful, backupFile.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.backup_failed, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun selectRestoreFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_sqlite_file)), REQUEST_CODE_RESTORE_DB)
    }

    private fun restoreDatabase(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.restore_database_title))
            .setMessage(getString(R.string.restore_database_confirmation))
            .setPositiveButton(getString(R.string.dialog_yes)) { dialog, _ ->
                try {
                    val backupDBPath = getDatabasePath(WiFiDatabaseHelper.DATABASE_NAME).absolutePath
                    
                    wifiDatabaseHelper.close()

                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        File(backupDBPath).outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    wifiDatabaseHelper = WiFiDatabaseHelper(this)

                    Toast.makeText(this, getString(R.string.restore_successful), Toast.LENGTH_LONG).show()
                    displayDatabaseInfo()
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.restore_failed, e.message), Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }


    private fun selectImportFileCSV() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, (getString(R.string.select_csv_file))), REQUEST_CODE_IMPORT_CSV)
    }

    private fun importDatabaseFromCSV(uri: Uri) {
        showImportTypeDialog { importType ->
            val progressDialog = ProgressDialog(this).apply {
                setMessage(getString(R.string.importing_data))
                setCancelable(false)
                show()
            }

            val handler = Handler(Looper.getMainLooper())
            val updateMessageRunnable = Runnable {
                if (progressDialog.isShowing) {
                    progressDialog.setMessage(getString(R.string.importing_large_amount))
                }
            }
            handler.postDelayed(updateMessageRunnable, 30000) // 30 секунд

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val networksToInsert = mutableListOf<Array<String>>()
                    inputStream?.bufferedReader()?.use { reader ->
                        val csvReader = CSVReader(reader)
                        var nextLine: Array<String>?
                        csvReader.readNext()
                        while (csvReader.readNext().also { nextLine = it } != null) {
                            nextLine?.let { line ->
                                val essid = line[0]
                                val bssid = line[1]
                                val password = line[2]
                                val wpsCode = line[3]
                                val adminLogin = line[4]
                                val adminPass = line[5]

                                if (importType == "update" && wifiDatabaseHelper.networkExists(bssid, password, wpsCode, adminLogin, adminPass)) {
                                    return@let
                                }

                                networksToInsert.add(arrayOf(essid, bssid, password, wpsCode, adminLogin, adminPass))

                                if (networksToInsert.size >= 100) {
                                    wifiDatabaseHelper.addNetworksInTransaction(networksToInsert)
                                    networksToInsert.clear()
                                }
                            }
                        }
                        csvReader.close()
                    }

                    if (networksToInsert.isNotEmpty()) {
                        wifiDatabaseHelper.addNetworksInTransaction(networksToInsert)
                    }

                    withContext(Dispatchers.Main) {
                        displayDatabaseInfo()
                        progressDialog.dismiss()
                        toast(getString(R.string.import_complete))
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        toast(getString(R.string.import_error, e.message))
                    }
                } finally {
                    handler.removeCallbacks(updateMessageRunnable)
                }
            }
        }
    }

    private fun importDataFromRouterScanAsync(uri: Uri) {
        showImportTypeDialog { importType ->
            val progressDialog = ProgressDialog(this).apply {
                setMessage(getString(R.string.importing_data))
                setCancelable(false)
                show()
            }

            val handler = Handler(Looper.getMainLooper())
            val updateMessageRunnable = Runnable {
                if (progressDialog.isShowing) {
                    progressDialog.setMessage(getString(R.string.importing_large_amount))
                }
            }
            handler.postDelayed(updateMessageRunnable, 30000) // 30 секунд

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val networksToAdd = mutableListOf<Array<String>>()

                    inputStream?.bufferedReader()?.useLines { lines ->
                        lines.forEach { line ->
                            parseRouterScanLine(line, importType)?.let { networkArray ->
                                networksToAdd.add(networkArray)
                            }
                        }
                    }

                    wifiDatabaseHelper.addNetworksInTransactionRS(networksToAdd)

                    withContext(Dispatchers.Main) {
                        displayDatabaseInfo()
                        progressDialog.dismiss()
                        Toast.makeText(applicationContext, getString(R.string.import_complete), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(applicationContext, getString(R.string.import_error, e.message), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    handler.removeCallbacks(updateMessageRunnable)
                }
            }
        }
    }

    private fun selectImportFileFromRouterScan() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/plain"
        startActivityForResult(intent, REQUEST_CODE_IMPORT_ROUTERSCAN)
    }


    private fun parseRouterScanLine(line: String, importType: String): Array<String>? {
        val parts = line.split("\t")
        if (parts.size >= 14) {
            val adminCredentials = parts[4].split(":")
            val adminLogin = if (adminCredentials.size > 1) adminCredentials[0] else ""
            val adminPass = if (adminCredentials.size > 1) adminCredentials[1] else ""
            val bssid = parts[8]
            val essid = parts[9]
            val keys = parts[11]
            val wps = parts[12]
            if (importType == "update" && wifiDatabaseHelper.networkExists(bssid, keys, wps, adminLogin, adminPass)) {
                return null
            }
            if (essid.isNotEmpty() || bssid.isNotEmpty()) {
                return arrayOf(essid, bssid, keys, wps, adminLogin, adminPass)
            }
        }
        return null
    }

    private fun showImportTypeDialog(importFunction: (String) -> Unit) {
        val options = arrayOf(getString(R.string.replace_database), getString(R.string.update_existing_database))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.select_import_type))
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    wifiDatabaseHelper.clearAllNetworks()
                    importFunction.invoke("replace")
                }
                1 -> importFunction.invoke("update")
            }
        }
        builder.show()
    }

    private fun showAddNetworkDialog() {
        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_network, null)
        val ssidInput = view.findViewById<EditText>(R.id.ssid_input)
        val bssidInput = view.findViewById<EditText>(R.id.bssid_input)
        val passwordInput = view.findViewById<EditText>(R.id.password_input)
        val wpsInput = view.findViewById<EditText>(R.id.wps_input)
        val adminLoginInput = view.findViewById<EditText>(R.id.admin_login_input)
        val adminPassInput = view.findViewById<EditText>(R.id.admin_pass_input)

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(view)
        dialogBuilder.setTitle(getString(R.string.add_network_manually))
        dialogBuilder.setPositiveButton(getString(R.string.save)) { dialog, _ ->
            val ssid = ssidInput.text.toString()
            val bssid = bssidInput.text.toString()
            val password = passwordInput.text.toString()
            val wps = wpsInput.text.toString()
            val adminLogin = adminLoginInput.text.toString()
            val adminPass = adminPassInput.text.toString()

            if (ssid.isNotEmpty() || bssid.isNotEmpty()) {
                wifiDatabaseHelper.addNetwork(ssid, bssid, password, wps, adminLogin, adminPass)
                displayDatabaseInfo()
            } else {
                Toast.makeText(this, getString(R.string.error_empty_ssid_bssid), Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }
        dialogBuilder.create().show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_IMPORT_DB -> {
                    data?.data?.also { uri ->
                        try {
                            val inputStream = contentResolver.openInputStream(uri)
                            inputStream?.let { stream ->
                                val fileContents = stream.bufferedReader().use(BufferedReader::readText)
                                importDatabaseAsync(fileContents)
                            } ?: throw Exception(getString(R.string.failed_to_open_file))
                        } catch (e: Exception) {
                            Toast.makeText(this, getString(R.string.error_importing_file) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    }
                }
                REQUEST_CODE_IMPORT_ROUTERSCAN -> {
                    data?.data?.also { uri ->
                        importDataFromRouterScanAsync(uri)
                    }
                }
                REQUEST_CODE_IMPORT_CSV -> {
                    data?.data?.also { uri ->
                        importDatabaseFromCSV(uri)
                    }
                }
                REQUEST_CODE_RESTORE_DB -> {
                    data?.data?.also { uri ->
                        restoreDatabase(uri)
                    }
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

    private fun importDatabaseAsync(jsonContents: String) {
        showImportTypeDialog { importType ->
            val progressDialog = ProgressDialog(this).apply {
                setMessage(getString(R.string.importing_data))
                setCancelable(false)
                show()
            }

            val handler = Handler(Looper.getMainLooper())
            val updateMessageRunnable = Runnable {
                if (progressDialog.isShowing) {
                    progressDialog.setMessage(getString(R.string.importing_large_amount))
                }
            }
            handler.postDelayed(updateMessageRunnable, 30000) // 30 секунд

            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val jsonArray = JSONArray(jsonContents)
                    val networksToInsert = mutableListOf<Array<String>>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val essid = jsonObject.optString("essid")
                        val bssid = jsonObject.optString("bssid")
                        val password = jsonObject.optString("password")
                        val wpsCode = jsonObject.optString("wpsCode").takeIf { it.isNotEmpty() } ?: jsonObject.optString("wps")
                        val adminLogin = jsonObject.optString("adminLogin")
                        val adminPass = jsonObject.optString("adminPass")

                        if (importType == "update" && wifiDatabaseHelper.networkExists(bssid, password, wpsCode, adminLogin, adminPass)) {
                            continue
                        }

                        networksToInsert.add(arrayOf(essid, bssid, password, wpsCode, adminLogin, adminPass))

                        if (networksToInsert.size >= 100) {
                            wifiDatabaseHelper.addNetworksInTransaction(networksToInsert)
                            networksToInsert.clear()
                        }
                    }

                    if (networksToInsert.isNotEmpty()) {
                        wifiDatabaseHelper.addNetworksInTransaction(networksToInsert)
                    }

                    withContext(Dispatchers.Main) {
                        displayDatabaseInfo()
                        progressDialog.dismiss()
                        Toast.makeText(applicationContext, getString(R.string.import_complete), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(applicationContext, getString(R.string.import_error, e.message), Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    handler.removeCallbacks(updateMessageRunnable)
                }
            }
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

    private fun exportDatabaseAsync() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.exporting_data))
            setCancelable(false)
            show()
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
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

                val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!folder.exists()) {
                    folder.mkdirs()
                }
                var file = File(folder, "wifi_database_export.json")
                var count = 1
                while (file.exists()) {
                    file = File(folder, "wifi_database_export(${count++}).json")
                }

                val fileWriter = FileWriter(file)
                fileWriter.write(jsonArray.toString(4))
                fileWriter.close()

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    toast(getString(R.string.database_exported_successfully_to) + " ${file.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    toast(getString(R.string.error_exporting_database) + ": ${e.message}")
                }
            }
        }
    }

    private fun exportDatabaseToCSV() {
        val progressDialog = ProgressDialog(this).apply {
            setMessage(getString(R.string.exporting_data))
            setCancelable(false)
            show()
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val wifiList = wifiDatabaseHelper.getAllNetworks()
                val csvFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wifi_database_export.csv")
                csvFile.bufferedWriter().use { writer ->
                    val csvWriter = CSVWriter(writer)
                    csvWriter.writeNext(arrayOf("essid", "bssid", "password", "wps", "adminLogin", "adminPass"))
                    wifiList.forEach { network ->
                        csvWriter.writeNext(arrayOf(
                            network.essid,
                            network.bssid,
                            network.keys?.joinToString(", "),
                            network.wps?.joinToString(", "),
                            network.adminLogin,
                            network.adminPass
                        ))
                    }
                    csvWriter.close()
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    toast(getString(R.string.database_exported_successfully_to) + " ${csvFile.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    toast(getString(R.string.error_exporting_database) + ": ${e.message}")
                }
            }
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
        val cursor = wifiDatabaseHelper.getNetworksCursor()
        val adapter = WiFiCursorAdapter(this, cursor)
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

    override fun onDestroy() {
        super.onDestroy()
        (listView.adapter as? WiFiCursorAdapter)?.cursor?.close()
    }

}