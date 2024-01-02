package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.example.WiFiPasswordSearcher.databinding.ActivitySettingsBinding

class SettingsActivity : Activity() {
    /**
     * Called when the activity is first created.
     */
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val strSettingsRows = resources.getStringArray(R.array.strings_settings_rows)
        val adapterSettingsListView = ArrayAdapter(this, android.R.layout.simple_list_item_1, strSettingsRows)
        binding.SettingsListView.adapter = adapterSettingsListView
        binding.SettingsListView.onItemClickListener = generalListOnClick
    }

    private val generalListOnClick = OnItemClickListener { parent, Item, position, id ->
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
                val version = AppVersion(applicationContext)
                if (!version.isActualyVersion(applicationContext, true)) {
                    version.ShowUpdateDialog(this@SettingsActivity)
                }
            }
            4 -> {
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