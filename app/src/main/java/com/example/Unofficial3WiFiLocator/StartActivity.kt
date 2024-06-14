package com.example.Unofficial3WiFiLocator

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.Unofficial3WiFiLocator.databinding.ActivityStartBinding
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class StartActivity : Activity() {

    private lateinit var binding: ActivityStartBinding
    private var userInteracted = false
    private lateinit var mSettings: Settings
    private lateinit var user: UserManager

    private fun loadServerList() {
        Thread {
            try {
                val url = URL(resources.getString(R.string.SERVERS_LIST_URL))
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val inputStream = connection.inputStream
                val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                val serverListJson = bufferedReader.readText()
                val serverList = JSONArray(serverListJson)
                val servers = getSavedServers()
                servers.add(0, resources.getString(R.string.change_server))
                for (i in 0 until serverList.length()) {
                    val server = serverList.getString(i)
                    if (!servers.contains(server)) {
                        servers.add(server)
                    }
                }
                servers.add(resources.getString(R.string.specify_own_server))
                runOnUiThread {
                    updateSpinner(servers)
                    showClearButton()
                    FirstRunIfNeeded(servers)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val defaultServerUrl = resources.getString(R.string.SERVER_URI_DEFAULT)
                val backupServers = arrayListOf("http://134.0.119.34", defaultServerUrl, resources.getString(R.string.specify_own_server))
                runOnUiThread {
                    updateSpinner(backupServers)
                }
            }
        }.start()
    }

    private fun updateSpinner(servers: ArrayList<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, servers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServer.adapter = adapter
    }

    private fun setupSpinner() {
        binding.spinnerServer.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (userInteracted) {
                    val selectedItem = parent.getItemAtPosition(position).toString()

                    when (selectedItem) {
                        resources.getString(R.string.specify_own_server) -> {
                            showServerInputDialog()
                            loadServerList()
                            updateCurrentServerTextView()
                        }
                        resources.getString(R.string.change_server) -> {
                            updateCurrentServerTextView()
                        }
                        else -> {
                            mSettings.Editor!!.putString(Settings.APP_SERVER_URI, selectedItem)
                            mSettings.Editor!!.commit()
                            updateCurrentServerTextView()
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerServer.setOnTouchListener { _, _ ->
            userInteracted = true
            false
        }
    }


    private fun initializeSpinnerWithCurrentServer(servers: ArrayList<String>) {
        val currentServer = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, "")
        val serverIndex = servers.indexOf(currentServer)
        binding.spinnerServer.setSelection(if (serverIndex != -1) serverIndex else 0)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, servers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerServer.adapter = adapter
        binding.spinnerServer.setSelection(serverIndex)
    }

    private fun showServerInputDialog() {
        val alertDialog = AlertDialog.Builder(this@StartActivity)
        alertDialog.setTitle(resources.getString(R.string.enter_server_address))

        val input = EditText(this@StartActivity)
        alertDialog.setView(input)
        alertDialog.setPositiveButton(resources.getString(R.string.ok)) { dialog, whichButton ->
            var customServerUrl = input.text.toString()

            if (customServerUrl.endsWith("/")) {
                customServerUrl = customServerUrl.removeSuffix("/")
            }

            mSettings.Editor!!.putString(Settings.APP_SERVER_URI, customServerUrl)
            mSettings.Editor!!.commit()
            saveServerList(customServerUrl)
            updateSpinnerWithSavedServers()
        }
        alertDialog.setNegativeButton(resources.getString(R.string.cancel), null)
        alertDialog.show()
    }


    private fun saveServerList(newServer: String) {
        val servers = getSavedServers()
        if (!servers.contains(newServer)) {
            servers.add(newServer)
            val serversJson = JSONArray(servers).toString()
            mSettings.Editor!!.putString("savedServers", serversJson)
            mSettings.Editor!!.commit()
        }
    }

    private fun getSavedServers(): ArrayList<String> {
        val serversJson = mSettings.AppSettings!!.getString("savedServers", "[]")
        val serversArray = JSONArray(serversJson)
        val servers = ArrayList<String>()
        for (i in 0 until serversArray.length()) {
            servers.add(serversArray.getString(i))
        }
        return servers
    }

    private fun updateSpinnerWithSavedServers() {
        val servers = getSavedServers()
        servers.add(0, resources.getString(R.string.change_server))
        servers.add(resources.getString(R.string.specify_own_server))
        updateSpinner(servers)
    }

    override fun onResume() {
        super.onResume()
        updateCurrentServerTextView()
    }

    private var clickCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClearServerList.setOnClickListener {
            clearServerList()
            showUpdateButton()
        }
        binding.btnUpdateServerList.setOnClickListener {
            loadServerList()
            showClearButton()
        }

        binding.btnOfflineMode.setOnClickListener {
            switchToOfflineMode()
        }
        mSettings = Settings(applicationContext)
        user = UserManager(applicationContext)
        updateSpinnerWithSavedServers()
        loadServerList()
        setupSpinner()
        loadAndShowMessage()
        onConfigurationChanged(resources.configuration)

        val currentServerUri = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
        binding.txtCurrentServer.text = resources.getString(R.string.current_server) + " $currentServerUri"

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
        binding.btnToggleLoginMethod.setOnClickListener {
            if (binding.edtApiKey.visibility == View.GONE) {
                binding.edtLogin.visibility = View.GONE
                binding.edtPassword.visibility = View.GONE
                binding.edtApiKey.visibility = View.VISIBLE
            } else {
                binding.edtLogin.visibility = View.VISIBLE
                binding.edtPassword.visibility = View.VISIBLE
                binding.edtApiKey.visibility = View.GONE
            }
        }
        binding.edtLogin.setText(savedLogin)
        binding.edtPassword.setText(savedPassword)
        val savedApiKey = mSettings.AppSettings!!.getString(Settings.API_READ_KEY, "")
        if (savedApiKey != "offline") {
            binding.edtApiKey.setText(savedApiKey)
        } else {
            binding.edtApiKey.setText("")
        }
        binding.btnGetApiKeys.setOnClickListener {
            val dProcess = ProgressDialog(this@StartActivity)
            dProcess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            dProcess.setMessage(resources.getString(R.string.status_signing_in))
            dProcess.setCanceledOnTouchOutside(false)
            dProcess.show()

            if (binding.edtApiKey.visibility == View.VISIBLE) {
                val apiKey = binding.edtApiKey.text.toString()
                mSettings.Editor!!.putString(Settings.API_READ_KEY, apiKey).apply()
                mSettings.Editor!!.putBoolean(Settings.API_KEYS_VALID, true).apply()

                runOnUiThread {
                    dProcess.dismiss()
                    binding.llStartMenu.visibility = View.VISIBLE
                    binding.edtApiKey.isEnabled = false
                    binding.btnGetApiKeys.visibility = View.GONE
                }
            } else {
                Thread(Runnable {
                    var res = false
                    try {
                        res = getApiKeys(binding.edtLogin.text.toString(), binding.edtPassword.text.toString())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    runOnUiThread {
                        dProcess.dismiss()
                        if (res) {
                            binding.llStartMenu.visibility = View.VISIBLE
                            binding.edtLogin.isEnabled = false
                            binding.edtPassword.isEnabled = false
                            binding.btnGetApiKeys.visibility = View.GONE
                        }
                    }
                }).start()
            }
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
        binding.btnClearServerList.setOnClickListener {
            clearServerList()
        }
        checkAndRequestPermissions()

        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setOnClickListener {
            performImageShake()
            clickCount++
            if (clickCount == 1) {
                Toast.makeText(this, resources.getString(R.string.easter_egg), Toast.LENGTH_SHORT).show()
            } else if (clickCount >= 3) {
                openRickRoll()
            }
        }
    }

    fun performImageShake() {
        val imageView = findViewById<ImageView>(R.id.imageView)
        val shake = ObjectAnimator.ofFloat(imageView, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        shake.duration = 500
        shake.start()
    }

    fun openRickRoll() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        startActivity(intent)
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), MY_PERMISSIONS_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION || permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION) {
                        Toast.makeText(this, resources.getString(R.string.message_nolocation_permissions), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    data class MessageInfo(
        val id: Int,
        val messages: Map<String, Message>
    )

    data class Message(
        val title: String,
        val message: String,
        val button_text: String? = null,
        val download_url: String? = null
    )

    fun loadAndShowMessage() = CoroutineScope(Dispatchers.IO).launch {
        if (!isNetworkAvailable()) {
            return@launch
        }

        val url = URL("https://raw.githubusercontent.com/LowSkillDeveloper/3WiFiLocator-Unofficial/master-v2/msg21.json")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.apply {
                requestMethod = "GET"
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream.use { stream ->
                        val jsonStr = stream.reader().use { reader -> reader.readText() }
                        val messageInfo = Gson().fromJson<MessageInfo>(jsonStr, MessageInfo::class.java)
                        withContext(Dispatchers.Main) {
                            handleJsonResponse(messageInfo)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun handleJsonResponse(messageInfo: MessageInfo) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val lastShownMessageId = sharedPreferences.getInt("lastShownMessageId", 0)

        if (messageInfo.id != 0 && messageInfo.id != lastShownMessageId) {
            val currentLanguage = Locale.getDefault().language
            val message = messageInfo.messages[currentLanguage] ?: messageInfo.messages["en"]

            message?.let {
                showAlertDialog(it.title, it.message, it.button_text, it.download_url)
                sharedPreferences.edit().putInt("lastShownMessageId", messageInfo.id).apply()
            }
        }
    }


    fun showAlertDialog(title: String, message: String, buttonText: String?, downloadUrl: String?) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton("OK", null)
        buttonText?.let { text ->
            alertDialog.setNegativeButton(text) { dialog, which ->
                downloadUrl?.let {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(downloadUrl)
                    startActivity(intent)
                }
            }
        }
        alertDialog.show()
    }


    private fun FirstRunIfNeeded(servers: ArrayList<String>) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (isFirstRun && servers.contains("http://134.0.119.34")) {
            mSettings.Editor!!.putBoolean("USE_CUSTOM_HOST", true).apply()
            sharedPreferences.edit().putBoolean("isFirstRun", false).apply()
        }
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

    private fun updateCurrentServerTextView() {
        val currentServerUri = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
        binding.txtCurrentServer.text = resources.getString(R.string.current_server) + " $currentServerUri"
    }
    private fun switchToOfflineMode() {
        mSettings.Editor!!.putString(Settings.API_READ_KEY, "offline")
        mSettings.Editor!!.commit()
        val offlineActivityIntent = Intent(this@StartActivity, MyActivity::class.java)
        startActivity(offlineActivityIntent)
    }

    private fun showUpdateButton() {
        binding.btnClearServerList.visibility = View.GONE
        binding.btnUpdateServerList.visibility = View.VISIBLE
    }

    private fun showClearButton() {
        binding.btnUpdateServerList.visibility = View.GONE
        binding.btnClearServerList.visibility = View.VISIBLE
    }

    private fun clearServerList() {
        mSettings.Editor!!.putString("savedServers", "[]")
        mSettings.Editor!!.commit()
        val defaultServerUrl = resources.getString(R.string.SERVER_URI_DEFAULT)
        val backupServers = arrayListOf(defaultServerUrl, resources.getString(R.string.specify_own_server))
        updateSpinner(backupServers)
        showUpdateButton()
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
            val useCustomHost = mSettings.AppSettings!!.getBoolean("USE_CUSTOM_HOST", false)
            if (useCustomHost && serverURI.startsWith("http://134.0.119.34")) {
                connection.setRequestProperty("Host", "3wifi.stascorp.com")
            }
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
        private const val MY_PERMISSIONS_REQUEST = 100
    }
}