package com.example.Unofficial3WiFiLocator

import android.content.Context
import android.os.Build
import eu.chainfire.libsuperuser.Shell
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class CliHelper(private val context: Context) {
    companion object {
        var CLI_NAME = ""
        var CLI_PATH = ""
        var CTRL_DIR = ""
        var INFO = StringBuilder()
    }

    init {
        try {
            val result = Shell.SU.run("cat /data/misc/wifi/wpa_supplicant.conf")
            if (result != null && result.isNotEmpty()) {
                CTRL_DIR = result.joinToString("\n")
                CTRL_DIR = CTRL_DIR.substring(CTRL_DIR.indexOf("=/") + 1, CTRL_DIR.indexOf("\n"))
                INFO.append("\nctrl sockets path:\n").append(CTRL_DIR).append("\n\n")
                CTRL_DIR = " -p $CTRL_DIR"
            } else {
                throw IOException("Failed to execute command")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CLI_PATH = if (Build.VERSION.SDK_INT >= 17) {
            context.applicationInfo.dataDir + "/files/"
        } else {
            "/data/data/" + context.packageName + "/files/"
        }

        INFO.append(Build.BRAND).append(" ").append(Build.MODEL).append("\nversion sdk: ")
            .append(Build.VERSION.SDK_INT).append("\nandroid version: ").append(Build.VERSION.RELEASE).append("\n")

        CLI_NAME = when (Build.VERSION.SDK_INT) {
            15 -> "wpa_cli404"
            16, 17 -> "wpa_cli41"
            18 -> "wpa_cli43"
            in 21..22 -> "wpa_cli50"
            else -> "wpa_cli60"
        }

        if (File("/system/bin/wpa_cli").exists()) {
            INFO.append("\nwpa_cli\n")
            CLI_PATH = ""
            CLI_NAME = "wpa_cli"
        } else {
            INFO.append("\nwpa_cli \n").append(CLI_NAME).append("\n\n")
        }

        val cliFile = File(CLI_PATH + CLI_NAME)
        if (!cliFile.exists()) {
            try {
                copyCli()
                cliFile.setExecutable(true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @Throws(IOException::class)
    private fun copyCli() {
        val inputStream: InputStream = context.assets.open(CLI_NAME)
        val outputStream = FileOutputStream(CLI_PATH + CLI_NAME)
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }
}
