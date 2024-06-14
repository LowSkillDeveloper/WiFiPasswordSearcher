package com.example.Unofficial3WiFiLocator

import android.content.Context
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class WpaCliHelper {

    private val a = "/wpa_cli_n"
    private val b = "libssl.so.1.1"
    private val c = "libssl.so.3"
    private val d = "libcrypto.so.1.1"
    private val e = "libcrypto.so.3"
    private val f = "libnl-3.so"
    private val g = "libnl-genl-3.so"
    private val h = "wpa_supplicant.conf"
    private val i = "/wpa_supplicant"
    private val j = "pixiedust"
    private val k = "iw"
    private val l = "libnl-route-3.so"

    private fun isFilesExist(context: Context): Boolean {
        val file1 = File("${context.filesDir.path}$a")
        val file2 = File("${context.filesDir.path}$i")
        return file1.exists() && file2.exists()
    }

    private fun copyFiles(context: Context) {
        val is64Bit = System.getProperty("os.arch").contains("64")
        val wpaCliFileName = if (is64Bit) "wpa_cli" else "wpa_cli-32"
        copyAssetFile(context, wpaCliFileName, a)
        setExecutable(context, a)

        val wpaSupplicantFileName = if (is64Bit) "wpa_supplicant" else "wpa_supplicant-32"
        copyAssetFile(context, wpaSupplicantFileName, i)
        setExecutable(context, i)

        copyAssetFile(context, h, h)

        val filesToCopy = if (is64Bit) {
            listOf(k, g, f, j, l, c, e)
        } else {
            listOf("${k}-32", "${d}-32", "${b}-32", "${g}-32", "${f}-32", "${j}-32", "${l}-32")
        }
        filesToCopy.forEach { file ->
            copyAssetFile(context, file, file)
            if (file.contains(j) || file.contains(k)) {
                setExecutable(context, file)
            }
        }
    }

    private fun copyAssetFile(context: Context, assetFileName: String, destFileName: String) {
        val assetManager = context.assets
        val inputStream = assetManager.open(assetFileName)
        val outputStream = FileOutputStream("${context.filesDir.path}/$destFileName")
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    private fun setExecutable(context: Context, fileName: String) {
        val file = File("${context.filesDir.path}/$fileName")
        file.setExecutable(true)
    }

    fun init(context: Context) {
        if (!isFilesExist(context)) {
            try {
                copyFiles(context)
                Log.e("WPACLI-SUPP", "wpa_cli created")
            } catch (e: IOException) {
                Log.d("WPACLI-SUPP", e.toString())
            }
        }
    }
}
