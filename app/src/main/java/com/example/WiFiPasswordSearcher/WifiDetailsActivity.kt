package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.SoundPool
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.WiFiPasswordSearcher.databinding.ActivityWifiDetailsBinding
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.*

class WifiDetailsActivity : Activity() {
    private lateinit var binding: ActivityWifiDetailsBinding
    private lateinit var wifiInfo: ScanResult
    private lateinit var detectorThread: Thread
    private lateinit var mSoundPool: SoundPool
    private lateinit var wifiMgr: WifiManager
    private lateinit var networkBSSID: String
    private lateinit var networkESSID: String
    private var scanThreadActive = false
    private var useWifiDetector = false
    private var lastSignal = 0
    private var lastFreq = -1
    private var lastBSSID: String? = null
    private var lastESSID: String? = null
    private lateinit var mSettings: Settings
    private lateinit var graphSeries: LineGraphSeries<DataPoint>
    private var iGraphPointCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWifiDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onConfigurationChanged(resources.configuration)
        useWifiDetector = false
        val startWifiInfo = intent.getSerializableExtra("WifiInfo") as HashMap<String, String>
        networkBSSID = startWifiInfo["BSSID"].toString()
        networkESSID = startWifiInfo["SSID"].toString()
        mSoundPool = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        // Graph init
        iGraphPointCount = 0
        graphSeries = LineGraphSeries()
        graphSeries.title = getString(R.string.label_signal_graph)
        binding.signalGraph.gridLabelRenderer.numVerticalLabels = 2
        binding.signalGraph.gridLabelRenderer.isHorizontalLabelsVisible = false
        binding.signalGraph.viewport.isXAxisBoundsManual = true
        binding.signalGraph.viewport.isScrollable = false
        binding.signalGraph.viewport.setMinY(0.0)
        binding.signalGraph.viewport.setMaxY(100.0)
        binding.signalGraph.viewport.yAxisBoundsStatus = Viewport.AxisBoundsStatus.INITIAL
        binding.signalGraph.viewport.setScalableY(true)
        binding.signalGraph.title = getString(R.string.label_signal_graph)
        binding.signalGraph.addSeries(graphSeries)
        binding.chkbUseDetector.setOnCheckedChangeListener { buttonView, isChecked ->
            useWifiDetector = isChecked
            mSettings.Editor!!.putBoolean(Settings.WIFI_SIGNAL, useWifiDetector)
            mSettings.Editor!!.commit()
            if (useWifiDetector) {
                detectorThread = Thread(Runnable { detectorWorker() })
                detectorThread.start()
            }
        }
        mSettings = Settings(applicationContext)
        mSettings.Reload()
        val wifiSignal = mSettings.AppSettings!!.getBoolean(Settings.WIFI_SIGNAL, false)
        binding.chkbUseDetector.isChecked = wifiSignal
        setBSSID(startWifiInfo["BSSID"])
        setESSID(startWifiInfo["SSID"])
        setFreq(startWifiInfo["Freq"])
        setSignal(startWifiInfo["Signal"])
        wifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val scanThread = Thread(Runnable { scanWorker() })
        scanThreadActive = true
        scanThread.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val lr = findViewById<LinearLayout>(R.id.rootLayout)
        val li = findViewById<LinearLayout>(R.id.layoutInfo)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            lr.orientation = LinearLayout.VERTICAL
            val layoutParams = li.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            li.layoutParams = layoutParams
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lr.orientation = LinearLayout.HORIZONTAL
            val layoutParams = li.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            li.layoutParams = layoutParams
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        useWifiDetector = false
        scanThreadActive = false
        binding.signalGraph.removeAllSeries()
    }

    private fun update() {
        setBSSID(wifiInfo.BSSID)
        setESSID(wifiInfo.SSID)
        setFreq(wifiInfo.frequency.toString())
        setSignal(wifiInfo.level.toString())
    }

    private fun detectorWorker() {
        val pickSoundId = mSoundPool.load(applicationContext, R.raw.pick, 1)
        while (useWifiDetector) {
            if (lastSignal > 0) mSoundPool.play(pickSoundId, 1f, 1f, 100, 0, 1f)
            val sleepTime = 2100 - 2000 / 100 * lastSignal
            try {
                Thread.sleep(sleepTime.toLong(), 0)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun scanWorker() // THREADED!
    {
        while (scanThreadActive) {
            var found = false
            wifiMgr.startScan()
            val results: List<ScanResult> = wifiMgr.scanResults
            var match: Boolean
            for (result in results) {
                match = if (networkBSSID.isNotEmpty() && networkESSID.isNotEmpty()) {
                    result.BSSID == networkBSSID && result.SSID == networkESSID
                } else if (networkBSSID.isEmpty() && networkESSID.isNotEmpty()) {
                    result.SSID == networkESSID
                } else if (networkBSSID.isNotEmpty() && networkESSID.isEmpty()) {
                    result.BSSID == networkBSSID
                } else {
                    true
                }
                if (match) {
                    if (networkBSSID.isEmpty()) networkBSSID = result.BSSID
                    if (networkESSID.isEmpty()) networkESSID = result.SSID
                    wifiInfo = result
                    update()
                    found = true
                    break
                }
            }
            if (!found) setSignal("-100")
            try {
                Thread.sleep(1000.toLong(), 0)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun setBSSID(BSSID: String?) {
        var BSSID = BSSID
        BSSID = BSSID!!.toUpperCase()
        if (BSSID == lastBSSID) return
        val text = "BSSID: " + if (BSSID.isEmpty()) "Unknown" else BSSID
        runOnUiThread { binding.txtDetailsBSSID.text = text }
        lastBSSID = BSSID
    }

    private fun setESSID(ESSID: String?) {
        if (ESSID == lastESSID) return
        val text = if (ESSID!!.isEmpty()) "<unknown>" else ESSID
        runOnUiThread { binding.txtDetailsESSID.text = text }
        lastESSID = ESSID
    }

    private fun setFreq(Freq: String?) {
        var sDiap = ""
        var channel = 0
        val iFreq = Freq!!.toInt()
        if (iFreq == lastFreq) return
        if (iFreq > 0) {
            when (iFreq) {
                in 2401..2483 -> {
                    sDiap = "2.4 GHz"
                    channel = (iFreq - 2412) / 5 + 1
                }
                in 5150..5250 -> {
                    sDiap = "UNII 1"
                    channel = (5000 + iFreq) / 5
                }
                in 5250..5350 -> {
                    sDiap = "UNII 2"
                    channel = (5000 + iFreq) / 5
                }
                in 5470..5725 -> {
                    sDiap = "UNII 2 Extended"
                    channel = iFreq / 5 + 1
                }
                in 5725..5825 -> {
                    sDiap = "UNII 3"
                    channel = (5000 + iFreq) / 5
                }
            }
        }
        val sText = "${getString(R.string.label_freq)} ${if (iFreq <= 0) "Unknown" else Freq} MHz ($sDiap)"
        runOnUiThread { binding.txtDetailsFreq.text = sText }
        setChannel(channel)
        lastFreq = iFreq
    }

    private fun setSignal(Signal: String?) {
        var iSignal = Signal!!.toInt()
        iSignal = (100 + iSignal) * 2
        iSignal = iSignal.coerceAtLeast(0).coerceAtMost(100)
        val fSignal = iSignal
        lastSignal = iSignal
        runOnUiThread {
            graphSeries.appendData(
                    DataPoint(iGraphPointCount.toDouble(), fSignal.toDouble()),
                    true,
                    25
            )
            iGraphPointCount++
            binding.txtDetailsSignal.text = "${getString(R.string.label_signal)} $fSignal%"
        }
    }

    private fun setChannel(Channel: Int) {
        runOnUiThread { binding.txtDetailsChannel.text = "${getString(R.string.label_channel)} ${if (Channel <= 0) "N/A" else Channel}" }
    }
}