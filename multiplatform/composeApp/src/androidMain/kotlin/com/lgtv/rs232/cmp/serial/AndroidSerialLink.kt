package com.lgtv.rs232.cmp.serial

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager

/** ฝั่ง Android: คุยผ่าน USB OTG ด้วย usb-serial-for-android (logic เดียวกับ MainActivity ของแอปเดิม) */
class AndroidSerialLink(private val context: Context) : SerialLink, SerialInputOutputManager.Listener {

    companion object {
        const val ACTION_USB_PERMISSION = "com.lgtv.rs232.cmp.USB_PERMISSION"
    }

    override var listener: SerialListener? = null

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private var requestedBaudRate = 9600

    override fun listPorts(): List<PortInfo> =
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).map { driver ->
            val d = driver.device
            PortInfo(
                id = d.deviceName,
                label = "${driver.javaClass.simpleName.removeSuffix("SerialDriver")} (${d.vendorId}:${d.productId})"
            )
        }

    override fun connect(portId: String?, baudRate: Int) {
        requestedBaudRate = baudRate

        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) {
            listener?.onConnected(false, "No USB serial device found — check OTG cable + RS232 adapter.")
            return
        }
        val driver = drivers.firstOrNull { it.device.deviceName == portId } ?: drivers[0]

        if (!usbManager.hasPermission(driver.device)) {
            // Android 14+ (targetSdk 34): mutable PendingIntent ต้องเป็น explicit intent (ระบุ package)
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(driver.device, permissionIntent)
            return // ผลจะวิ่งกลับมาทาง onPermissionResult() ผ่าน BroadcastReceiver ของ MainActivity
        }
        open(driver)
    }

    /** MainActivity เรียกเมื่อผู้ใช้ตอบ dialog ขอสิทธิ์ USB */
    fun onPermissionResult(device: UsbDevice?, granted: Boolean) {
        if (!granted) {
            listener?.onConnected(false, "Permission denied for USB device.")
            return
        }
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            .firstOrNull { it.device.deviceId == device?.deviceId }
        if (driver != null) {
            open(driver)
        } else {
            listener?.onConnected(false, "USB device not found after permission grant.")
        }
    }

    /** MainActivity เรียกเมื่อสาย USB ถูกถอด */
    fun onDeviceDetached(device: UsbDevice?) {
        if (device != null && serialPort?.device?.deviceId == device.deviceId) {
            disconnect()
        }
    }

    private fun open(driver: UsbSerialDriver) {
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            listener?.onConnected(false, "Failed to open USB device.")
            return
        }
        try {
            val port = driver.ports[0] // อะแดปเตอร์ทั่วไปมีพอร์ตเดียว
            port.open(connection)
            port.setParameters(requestedBaudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            serialPort = port
            ioManager = SerialInputOutputManager(port, this).also { it.start() }
            listener?.onConnected(true, "Connected @ $requestedBaudRate bps")
        } catch (e: Exception) {
            closeQuietly()
            listener?.onConnected(false, "Error: ${e.message}")
        }
    }

    override fun disconnect() {
        closeQuietly()
        listener?.onDisconnected()
    }

    override fun send(text: String) {
        val port = serialPort ?: return
        if (!port.isOpen) return
        try {
            port.write(text.toByteArray(Charsets.US_ASCII), 1000)
        } catch (_: Exception) {
            // เขียนไม่สำเร็จ — ปล่อยให้ onRunError จัดการถ้าหลุดจริง
        }
    }

    private fun closeQuietly() {
        try { ioManager?.stop() } catch (_: Exception) {}
        ioManager = null
        try { serialPort?.close() } catch (_: Exception) {}
        serialPort = null
    }

    // ---- SerialInputOutputManager.Listener (มาจาก background thread) ----

    override fun onNewData(data: ByteArray?) {
        if (data != null) listener?.onDataReceived(String(data, Charsets.US_ASCII))
    }

    override fun onRunError(e: Exception?) {
        closeQuietly()
        listener?.onConnected(false, "Connection lost: ${e?.message}")
        listener?.onDisconnected()
    }
}
