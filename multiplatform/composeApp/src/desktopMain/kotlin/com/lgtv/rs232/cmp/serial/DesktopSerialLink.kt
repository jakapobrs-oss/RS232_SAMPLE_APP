package com.lgtv.rs232.cmp.serial

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent

/** ฝั่ง Windows/desktop: คุย COM port ผ่านไลบรารี jSerialComm (LG ใช้ 8N1 ไม่มี flow control) */
class DesktopSerialLink : SerialLink {

    override var listener: SerialListener? = null
    private var port: SerialPort? = null

    override fun listPorts(): List<PortInfo> =
        SerialPort.getCommPorts().map {
            PortInfo(it.systemPortName, "${it.systemPortName} — ${it.descriptivePortName}")
        }

    override fun connect(portId: String?, baudRate: Int) {
        closeQuietly()

        val all = SerialPort.getCommPorts()
        val target = all.firstOrNull { it.systemPortName == portId } ?: all.firstOrNull()
        if (target == null) {
            listener?.onConnected(false, "No serial port found — plug in the USB-RS232 adapter and press Refresh.")
            return
        }

        target.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY)
        target.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED)

        if (!target.openPort()) {
            listener?.onConnected(
                false,
                "Failed to open ${target.systemPortName} — port in use by another program, or unplug/replug the adapter."
            )
            return
        }

        target.addDataListener(object : SerialPortDataListener {
            override fun getListeningEvents(): Int =
                SerialPort.LISTENING_EVENT_DATA_AVAILABLE or SerialPort.LISTENING_EVENT_PORT_DISCONNECTED

            override fun serialEvent(event: SerialPortEvent) {
                when (event.eventType) {
                    SerialPort.LISTENING_EVENT_DATA_AVAILABLE -> {
                        val available = target.bytesAvailable()
                        if (available > 0) {
                            val buffer = ByteArray(available)
                            val read = target.readBytes(buffer, buffer.size)
                            if (read > 0) {
                                listener?.onDataReceived(String(buffer, 0, read, Charsets.US_ASCII))
                            }
                        }
                    }
                    SerialPort.LISTENING_EVENT_PORT_DISCONNECTED -> {
                        closeQuietly()
                        listener?.onConnected(false, "Connection lost: adapter unplugged.")
                        listener?.onDisconnected()
                    }
                }
            }
        })

        port = target
        listener?.onConnected(true, "Connected to ${target.systemPortName} @ $baudRate bps")
    }

    override fun disconnect() {
        closeQuietly()
        listener?.onDisconnected()
    }

    override fun send(text: String) {
        val p = port ?: return
        val bytes = text.toByteArray(Charsets.US_ASCII)
        p.writeBytes(bytes, bytes.size)
    }

    private fun closeQuietly() {
        port?.let {
            try { it.removeDataListener() } catch (_: Exception) {}
            try { it.closePort() } catch (_: Exception) {}
        }
        port = null
    }
}
