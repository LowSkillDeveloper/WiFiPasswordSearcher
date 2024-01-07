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
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivityServerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mSettings = Settings(applicationContext)
        mSettings.Reload()
        val serverLogin = mSettings.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
        val serverPassword = mSettings.AppSettings!!.getString(Settings.APP_SERVER_PASSWORD, "")


        val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
        binding.txtSettings3wifiServer.setText(serverURI)


        val fetchESS = mSettings.AppSettings!!.getBoolean(Settings.APP_FETCH_ESS, false)
        val checkUpdates = mSettings.AppSettings!!.getBoolean(Settings.APP_CHECK_UPDATES, true)
        binding.txtSettings3wifiLogin.setText(serverLogin)
        binding.txtSettings3wifiPassword.setText(serverPassword)
        binding.txtSettings3wifiServer.setText(serverURI)
        binding.swFetchEss.isChecked = fetchESS
        binding.swCheckUpd.isChecked = checkUpdates
        binding.btnSettitgs3wifiCancel.setOnClickListener(btnCloseOnClick)
        binding.btnSettitgs3wifiSave.setOnClickListener(btnSaveOnClick)

        val switchDarkMode = findViewById<Switch>(R.id.switch_dark_mode)
        switchDarkMode.isChecked = getDarkModePreference()
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            setDarkModePreference(isChecked)
            restartApp()
        }
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
        // Save
        mSettings.Editor!!.putString(Settings.APP_SERVER_LOGIN, login)
        mSettings.Editor!!.putString(Settings.APP_SERVER_PASSWORD, password)



        mSettings.Editor!!.putString(Settings.APP_SERVER_URI, binding.txtSettings3wifiServer.text.toString())
        mSettings.Editor!!.commit()


        mSettings.Editor!!.putBoolean(Settings.APP_FETCH_ESS, fetchESS)
        mSettings.Editor!!.putBoolean(Settings.APP_CHECK_UPDATES, checkUpdates)
        mSettings.Editor!!.commit()
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