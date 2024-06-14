import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class WifiUtil(private val context: Context) {

    fun executeWpaCommand(command: String): String {
        val assetManager = context.assets
        val wpaCliFile = assetManager.openFd("wpa_cli").fileDescriptor
        val wpaSupplicantFile = assetManager.openFd("wpa_supplicant").fileDescriptor

        val runtime = Runtime.getRuntime()
        val process = runtime.exec("$wpaCliFile $command")

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()
        reader.forEachLine { line ->
            output.append(line).append("\n")
        }

        reader.close()
        process.waitFor()
        return output.toString()
    }

    fun connectUsingWpsPin(ssid: String, pin: String): String {
        val commands = listOf(
            "set_network 0 ssid \"$ssid\"",
            "set_network 0 key_mgmt NONE",
            "wps_pin any $pin",
            "enable_network 0"
        )

        val results = commands.map { command ->
            executeWpaCommand(command)
        }

        return results.joinToString("\n")
    }
}
