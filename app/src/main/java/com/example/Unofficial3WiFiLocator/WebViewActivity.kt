package com.example.Unofficial3WiFiLocator

import android.app.DownloadManager
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.os.Environment
import android.preference.PreferenceManager
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import android.webkit.WebResourceResponse
import android.widget.Toast
import java.io.IOException

class WebViewActivity : AppCompatActivity() {

    private lateinit var mSettings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        setAppTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        mSettings = Settings(applicationContext)
        mSettings.Reload()

        val serverURI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, "")
        val useCustomHost = mSettings.AppSettings!!.getBoolean("USE_CUSTOM_HOST", false)

        val webView = findViewById<WebView>(R.id.webview)


        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.setDescription("Downloading file...")
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
        }

        webView.setInitialScale(100)
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                if (url != null && useCustomHost && url.startsWith("http://134.0.119.34")) {
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.setRequestProperty("Host", "3wifi.stascorp.com")
                        val inputStream = connection.inputStream
                        val mimeType = "text/html"
                        val encoding = "UTF-8"
                        return WebResourceResponse(mimeType, encoding, inputStream)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                return super.shouldInterceptRequest(view, url)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
        }
        if (serverURI != null) {
            webView.loadUrl(serverURI)
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
}