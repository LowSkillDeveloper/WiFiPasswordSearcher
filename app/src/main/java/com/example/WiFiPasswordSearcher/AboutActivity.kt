package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import com.example.WiFiPasswordSearcher.databinding.ActivityAboutBinding
import java.nio.charset.Charset

class AboutActivity : Activity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val filename = "about.html"
        var html: String
        html = try {
            val `in` = assets.open(filename)
            val size = `in`.available()
            val data = ByteArray(size)
            val read = `in`.read(data)
            `in`.close()
            if (read > 0) String(data, Charset.forName("UTF-8")) else ""
        } catch (e: Exception) {
            ""
        }
        val backColor = themeColor
        val textColor = resources.getColor(if (isColorDark(backColor)) android.R.color.secondary_text_dark else android.R.color.secondary_text_light)
        html = html.replace("#000;", colorToCSS(backColor))
        html = html.replace("#fff;", colorToCSS(textColor))
        binding.aboutWeb.loadData(html, "text/html", "UTF-8")
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    private fun colorToCSS(color: Int): String {
        val str = String.format("%06x", color and 0xFFFFFF)
        return "#$str;"
    }

    private val themeColor: Int
        get() {
            var color = 0
            val v = TypedValue()
            theme.resolveAttribute(android.R.attr.colorBackground, v, true)
            if (v.type >= TypedValue.TYPE_FIRST_COLOR_INT && v.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                color = v.data
            }
            return color
        }
}