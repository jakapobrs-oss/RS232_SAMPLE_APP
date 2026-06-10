package com.lgtv.rs232

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException

class MainActivity : AppCompatActivity(), SerialInputOutputManager.Listener {

    private lateinit var webView: WebView
    private lateinit var usbManager: UsbManager
    
    private var usbSerialPort: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    
    private var currentBaudRate: Int = 9600

    companion object {
        private const val ACTION_USB_PERMISSION = "com.lgtv.rs232.USB_PERMISSION"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        
        // Add JS Interface
        webView.addJavascriptInterface(AndroidBridge(), "AndroidSerial")
        
        // Load the web app
        webView.loadUrl("file:///android_asset/www/index.html")

        // Register USB Permission receiver
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectSerial()
        unregisterReceiver(usbReceiver)
    }

    // --- JavaScript Interface ---
    inner class AndroidBridge {
        @JavascriptInterface
        fun connect(baudRate: Int) {
            currentBaudRate = baudRate
            runOnUiThread {
                connectToDevice()
            }
        }

        @JavascriptInterface
        fun disconnect() {
            runOnUiThread {
                disconnectSerial()
            }
        }

        @JavascriptInterface
        fun send(data: String) {
            if (usbSerialPort != null && usbSerialPort!!.isOpen) {
                try {
                    usbSerialPort!!.write(data.toByteArray(Charsets.US_ASCII), 1000)
                    Log.d("Serial", "TX: $data")
                } catch (e: Exception) {
                    Log.e("Serial", "Write error", e)
                }
            }
        }
    }

    // --- USB Serial Logic ---
    private fun connectToDevice() {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Toast.makeText(this, "No USB Serial devices found", Toast.LENGTH_SHORT).show()
            notifyWebJS("window.onSerialConnected(false, 'No USB Serial devices found.');")
            return
        }

        val driver = availableDrivers[0]
        val device = driver.device

        if (!usbManager.hasPermission(device)) {
            // Android 14+ (targetSdk 34) บังคับว่า mutable PendingIntent ต้องเป็น explicit intent
            // (ระบุ package) ไม่งั้น throw ตั้งแต่ตอนสร้าง — เป็น pattern ตาม README ของ usb-serial-for-android
            val permissionIntent = PendingIntent.getBroadcast(
                this, 0, Intent(ACTION_USB_PERMISSION).setPackage(packageName),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            usbManager.requestPermission(device, permissionIntent)
            return
        }

        openDevice(driver)
    }

    private fun openDevice(driver: com.hoho.android.usbserial.driver.UsbSerialDriver) {
        val connection = usbManager.openDevice(driver.device)
        if (connection == null) {
            notifyWebJS("window.onSerialConnected(false, 'Failed to open USB device.');")
            return
        }

        usbSerialPort = driver.ports[0] // Most devices have just one port (port 0)
        try {
            usbSerialPort?.open(connection)
            usbSerialPort?.setParameters(
                currentBaudRate, 
                8, 
                UsbSerialPort.STOPBITS_1, 
                UsbSerialPort.PARITY_NONE
            )

            usbIoManager = SerialInputOutputManager(usbSerialPort, this)
            usbIoManager?.start()

            notifyWebJS("window.onSerialConnected(true, 'Connected successfully!');")
        } catch (e: Exception) {
            notifyWebJS("window.onSerialConnected(false, 'Error: ${e.message}');")
            disconnectSerial()
        }
    }

    private fun disconnectSerial() {
        usbIoManager?.stop()
        usbIoManager = null
        try {
            usbSerialPort?.close()
        } catch (e: IOException) {
            // Ignore
        }
        usbSerialPort = null
        notifyWebJS("window.onSerialDisconnected();")
    }

    // --- Serial Listener ---
    override fun onNewData(data: ByteArray?) {
        data?.let {
            val str = String(it, Charsets.US_ASCII)
            Log.d("Serial", "RX: $str")
            // Pass to JS
            runOnUiThread {
                val safeStr = str.replace("\n", "\\n").replace("\r", "\\r").replace("'", "\\'")
                notifyWebJS("window.onSerialDataReceived('$safeStr');")
            }
        }
    }

    override fun onRunError(e: Exception?) {
        runOnUiThread {
            disconnectSerial()
            notifyWebJS("window.onSerialConnected(false, 'Connection lost: ${e?.message}');")
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            device?.let {
                                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                                val driver = availableDrivers.find { it.device.deviceId == device.deviceId }
                                if (driver != null) {
                                    openDevice(driver)
                                }
                            }
                        } else {
                            notifyWebJS("window.onSerialConnected(false, 'Permission denied for device.');")
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null && usbSerialPort?.device?.deviceId == device.deviceId) {
                        disconnectSerial()
                    }
                }
            }
        }
    }

    private fun notifyWebJS(script: String) {
        webView.evaluateJavascript(script, null)
    }
}
