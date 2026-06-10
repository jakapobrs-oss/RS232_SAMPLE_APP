package com.lgtv.rs232.cmp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lgtv.rs232.cmp.serial.LgProtocol
import com.lgtv.rs232.cmp.serial.PortInfo
import com.lgtv.rs232.cmp.serial.SerialLink
import com.lgtv.rs232.cmp.serial.SerialListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LogKind { INFO, TX, RX, ERROR }

data class LogEntry(val time: String, val tag: String, val message: String, val kind: LogKind)

/**
 * สมอง/state ของหน้าจอ — พฤติกรรมเดียวกับ app.js ของเว็บเดิมทั้งหมด
 * (auto power query หลังต่อสำเร็จ, buffer คำตอบจนเจอ 'x', mock mode ฯลฯ)
 */
class ControllerState(
    private val link: SerialLink,
    private val scope: CoroutineScope
) : SerialListener {

    var connected by mutableStateOf(false); private set
    var mockMode by mutableStateOf(false); private set
    var baudRate by mutableStateOf(9600)
    var setId by mutableStateOf(1)
    var volume by mutableStateOf(0); private set
    var muted by mutableStateOf(false); private set
    var ports by mutableStateOf(listOf<PortInfo>()); private set
    var selectedPortId by mutableStateOf<String?>(null)
    val logs = mutableStateListOf<LogEntry>()

    private var receiveBuffer = ""

    val controlsEnabled: Boolean get() = connected || mockMode

    init {
        link.listener = this
        log("INFO", "System initialized. Waiting for connection...", LogKind.INFO)
        refreshPorts()
    }

    fun refreshPorts() {
        ports = link.listPorts()
        if (ports.none { it.id == selectedPortId }) selectedPortId = ports.firstOrNull()?.id
    }

    fun connect() {
        if (mockMode) toggleMock()
        log("INFO", "Requesting connection at $baudRate bps...", LogKind.INFO)
        link.connect(selectedPortId, baudRate)
    }

    fun disconnect() {
        if (!connected) return
        link.disconnect()
    }

    fun toggleMock() {
        if (connected && !mockMode) {
            log("ERROR", "Please disconnect real port before enabling mock mode.", LogKind.ERROR)
            return
        }
        mockMode = !mockMode
        log(
            "INFO",
            if (mockMode) "Mock Mode enabled — commands are simulated locally."
            else "Mock Mode disabled.",
            LogKind.INFO
        )
    }

    fun clearLogs() {
        logs.clear()
        log("INFO", "Log cleared.", LogKind.INFO)
    }

    // ---- callbacks จาก SerialLink (อาจถูกเรียกจาก background thread) ----

    override fun onConnected(success: Boolean, message: String) = onUi {
        if (success) {
            connected = true
            log("INFO", message, LogKind.INFO)
            scope.launch {
                delay(500) // ถามสถานะ power หลังต่อสำเร็จ (เหมือนเว็บเดิม)
                sendCommand("ka", "FF")
            }
        } else {
            connected = false
            log("ERROR", message, LogKind.ERROR)
        }
    }

    override fun onDisconnected() = onUi {
        connected = false
        log("INFO", "Disconnected.", LogKind.INFO)
    }

    override fun onDataReceived(data: String) = onUi {
        if (!connected) return@onUi
        receiveBuffer += data
        var idx = receiveBuffer.indexOf(LgProtocol.TERMINATOR)
        while (idx != -1) {
            val message = receiveBuffer.substring(0, idx + 1).trim()
            receiveBuffer = receiveBuffer.substring(idx + 1)
            if (message.isNotEmpty()) handleResponse(message)
            idx = receiveBuffer.indexOf(LgProtocol.TERMINATOR)
        }
    }

    /** ย้ายงานเข้า UI dispatcher เพราะ Compose state ควรแก้จาก thread เดียว */
    private fun onUi(block: () -> Unit) {
        scope.launch { block() }
    }

    // ---- ส่งคำสั่ง ----

    fun sendCommand(cmd: String, dataHex: String) {
        val fullCmd = LgProtocol.command(cmd, setId, dataHex)
        val display = fullCmd.trim()

        if (mockMode) {
            log("TX", display, LogKind.TX)
            simulateResponse(cmd, dataHex)
            return
        }
        if (!connected) {
            log("ERROR", "Port not connected.", LogKind.ERROR)
            return
        }
        link.send(fullCmd)
        log("TX", display, LogKind.TX)
    }

    fun sendRaw(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return

        if (mockMode) {
            log("TX", text, LogKind.TX)
            scope.launch {
                delay(300)
                handleResponse("${text.getOrElse(1) { 'k' }} ${LgProtocol.toHex(setId)} OKFFx")
            }
            return
        }
        if (!connected) {
            log("ERROR", "Port not connected.", LogKind.ERROR)
            return
        }
        link.send(text + "\r")
        log("TX", text, LogKind.TX)
    }

    fun applyVolume(value: Int) {
        volume = value.coerceIn(0, 100)
        sendCommand("kf", LgProtocol.toHex(volume))
    }

    fun toggleMute() {
        muted = !muted
        // โปรโตคอล LG: ke 00 = mute, ke 01 = เสียงปกติ
        sendCommand("ke", if (muted) "00" else "01")
    }

    // ---- รับคำตอบ ----

    private fun handleResponse(message: String) {
        log("RX", message, LogKind.RX)
        val res = LgProtocol.parse(message) ?: return

        if (!res.ok) {
            log("ERROR", "TV returned NG (error) for command '${res.cmd2}'", LogKind.ERROR)
            return
        }
        when (res.cmd2) {
            'f' -> volume = res.dataHex.toIntOrNull(16)?.coerceIn(0, 100) ?: volume
            'a' -> log(
                "INFO",
                if (res.dataHex == "01") "TV Power is ON" else "TV Power is OFF",
                LogKind.INFO
            )
        }
    }

    private fun simulateResponse(cmd: String, dataHex: String) {
        scope.launch {
            delay(400)
            val resData = if (dataHex.equals("FF", ignoreCase = true)) {
                when (cmd) {
                    "ka" -> "01" // Power On
                    "kf" -> "32" // Volume 50
                    "ke" -> "01" // Mute off
                    "xb" -> "90" // HDMI 1
                    else -> "01"
                }
            } else dataHex
            handleResponse("${cmd.last()} ${LgProtocol.toHex(setId)} OK${resData}x")
        }
    }

    private fun log(tag: String, message: String, kind: LogKind) {
        logs.add(LogEntry(timeNow(), tag, message, kind))
        if (logs.size > 500) logs.removeAt(0) // กัน log โตเรื่อยๆ จนกินแรม
    }
}
