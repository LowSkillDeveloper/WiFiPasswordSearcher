package com.example.Unofficial3WiFiLocator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.example.Unofficial3WiFiLocator.databinding.ActivitySettingsBinding
import android.preference.PreferenceManager

class SettingsActivity : Activity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val strSettingsRows = resources.getStringArray(R.array.strings_settings_rows)
        val adapterSettingsListView = ArrayAdapter(this, android.R.layout.simple_list_item_1, strSettingsRows)
        binding.SettingsListView.adapter = adapterSettingsListView
        binding.SettingsListView.onItemClickListener = generalListOnClick
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

    private val generalListOnClick = AdapterView.OnItemClickListener { _, _, _, id ->
        when (id.toInt()) {
            0 -> {
                val serverSettingsIntent = Intent(this@SettingsActivity, ServerSettingsActivity::class.java)
                startActivity(serverSettingsIntent)
            }
            1 -> {
                val userActivity = Intent(this@SettingsActivity, UserInfoActivity::class.java)
                userActivity.putExtra("showInfo", "wpspin")
                startActivity(userActivity)
            }
            2 -> {
                val aboutInfoIntent = Intent(this@SettingsActivity, AboutActivity::class.java)
                startActivity(aboutInfoIntent)
            }
            3 -> {
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LowSkillDeveloper/3WiFiLocator-Unofficial"))
                startActivity(githubIntent)
            }
            4 -> {
                val version = AppVersion(applicationContext)
                if (!version.isActualyVersion(applicationContext, true)) {
                    version.ShowUpdateDialog(this@SettingsActivity)
                }
            }
            5 -> {
                val mSettings = Settings(applicationContext)
                mSettings.Reload()
                mSettings.Editor!!.remove(Settings.APP_SERVER_LOGIN)
                mSettings.Editor!!.remove(Settings.APP_SERVER_PASSWORD)
                mSettings.Editor!!.remove(Settings.API_READ_KEY)
                mSettings.Editor!!.remove(Settings.API_WRITE_KEY)
                mSettings.Editor!!.remove(Settings.API_KEYS_VALID)
                mSettings.Editor!!.commit()
                val startPage = Intent(applicationContext, StartActivity::class.java)
                startPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(startPage)
            }
        }
    }
}