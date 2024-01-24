package com.example.Unofficial3WiFiLocator

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.CheckBox
import com.example.Unofficial3WiFiLocator.databinding.ActivityServerSettingsBinding
import android.preference.PreferenceManager
import android.widget.Switch

class ServerSettingsActivity : Activity() {
    private lateinit var binding: ActivityServerSettingsBinding
    private lateinit var mSettings: Settings
    private lateinit var switchPrimaryButton: Switch
    private lateinit var switchDarkMode: Switch
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityServerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mSettings = Settings(applicationContext)
        mSettings.Reload()
        val serverLogin = mSettings.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
        val serverPassword = mSettings.AppSettings!!.getString(Settings.APP_SERVER_PASSWORD, "")

        switchPrimaryButton = findViewById<Switch>(R.id.switch_primary_button)
        val primaryButtonIsLocalDb = mSettings.AppSettings!!.getBoolean("PRIMARY_BUTTON_IS_LOCAL_DB", false)
        switchPrimaryButton.isChecked = primaryButtonIsLocalDb

        val doubleScan = mSettings.AppSettings!!.getBoolean("DOUBLE_SCAN", false)
        val switchDoubleScan = findViewById<Switch>(R.id.switch_double_scan)
        switchDoubleScan.isChecked = doubleScan

        val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
        binding.txtSettings3wifiServer.setText(serverURI)
        val switchAutoSearchLocalDb = findViewById<Switch>(R.id.switch_auto_search_local_db)
        val switchSaveToDb = findViewById<Switch>(R.id.switch_save_to_db)
        val saveToDb = mSettings.AppSettings!!.getBoolean("SAVE_TO_DB", true)
        val autoSearchLocalDb = mSettings.AppSettings!!.getBoolean("AUTO_SEARCH_LOCAL_DB", true)
        switchSaveToDb.isChecked = saveToDb

        val fetchESS = mSettings.AppSettings!!.getBoolean(Settings.APP_FETCH_ESS, false)
        val checkUpdates = mSettings.AppSettings!!.getBoolean(Settings.APP_CHECK_UPDATES, true)
        binding.txtSettings3wifiLogin.setText(serverLogin)
        binding.txtSettings3wifiPassword.setText(serverPassword)
        binding.txtSettings3wifiServer.setText(serverURI)
        binding.swFetchEss.isChecked = fetchESS
        binding.swCheckUpd.isChecked = checkUpdates
        binding.switchSaveToDb.isChecked = saveToDb
        binding.switchAutoSearchLocalDb.isChecked = autoSearchLocalDb
        binding.btnSettitgs3wifiCancel.setOnClickListener(btnCloseOnClick)
        binding.btnSettitgs3wifiSave.setOnClickListener(btnSaveOnClick)

        switchDarkMode = findViewById<Switch>(R.id.switch_dark_mode)
        switchDarkMode.isChecked = getDarkModePreference()
    }

    private fun restartApp() {
        val i = Intent(this, StartActivity::class.java)
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun getDarkModePreference(): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPref.getBoolean("DARK_MODE", false)
    }

    private fun setDarkModePreference(isDarkMode: Boolean) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        with(sharedPref.edit()) {
            putBoolean("DARK_MODE", isDarkMode)
            apply()
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

    private val btnCloseOnClick = View.OnClickListener { finish() }
    private val btnSaveOnClick = View.OnClickListener {
        val login = binding.txtSettings3wifiLogin.text.toString()
        val password = binding.txtSettings3wifiPassword.text.toString()
        val uri = binding.txtSettings3wifiServer.text.toString()
        val fetchESS = binding.swFetchEss.isChecked
        val checkUpdates = binding.swCheckUpd.isChecked
        val autoSearchLocalDb = binding.switchAutoSearchLocalDb.isChecked
        val switchSaveToDb = findViewById<Switch>(R.id.switch_save_to_db)
        mSettings.Editor!!.putBoolean("SAVE_TO_DB", switchSaveToDb.isChecked).apply()
        mSettings.Editor!!.putBoolean("AUTO_SEARCH_LOCAL_DB", autoSearchLocalDb)
        val switchDoubleScan = findViewById<Switch>(R.id.switch_double_scan)
        mSettings.Editor!!.putBoolean("DOUBLE_SCAN", switchDoubleScan.isChecked).apply()
        mSettings.Editor!!.putBoolean("PRIMARY_BUTTON_IS_LOCAL_DB", switchPrimaryButton.isChecked).apply()
        val isDarkMode = switchDarkMode.isChecked
        setDarkModePreference(isDarkMode)

        mSettings.Editor!!.putString(Settings.APP_SERVER_LOGIN, login)
        mSettings.Editor!!.putString(Settings.APP_SERVER_PASSWORD, password)



        mSettings.Editor!!.putString(Settings.APP_SERVER_URI, binding.txtSettings3wifiServer.text.toString())
        mSettings.Editor!!.commit()


        mSettings.Editor!!.putBoolean(Settings.APP_FETCH_ESS, fetchESS)
        mSettings.Editor!!.putBoolean(Settings.APP_CHECK_UPDATES, checkUpdates)
        mSettings.Editor!!.commit()
        restartApp()
        finish()
    }

    fun cbUnmaskClick(view: View) {
        val eType = binding.txtSettings3wifiPassword.inputType
        if ((view as CheckBox).isChecked) {
            binding.txtSettings3wifiPassword.inputType = eType and InputType.TYPE_TEXT_VARIATION_PASSWORD.inv()
        } else {
            binding.txtSettings3wifiPassword.inputType = eType or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}