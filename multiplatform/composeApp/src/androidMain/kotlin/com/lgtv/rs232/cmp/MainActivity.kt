package com.lgtv.rs232.cmp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.lgtv.rs232.cmp.serial.AndroidSerialLink

class MainActivity : ComponentActivity() {

    private lateinit var serialLink: AndroidSerialLink

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            when (intent.action) {
                AndroidSerialLink.ACTION_USB_PERMISSION -> serialLink.onPermissionResult(
                    device,
                    intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                )
                UsbManager.ACTION_USB_DEVICE_DETACHED -> serialLink.onDeviceDetached(device)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        serialLink = AndroidSerialLink(this)

        val filter = IntentFilter(AndroidSerialLink.ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbReceiver, filter)
        }

        setContent {
            App(serialLink)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serialLink.disconnect()
        unregisterReceiver(usbReceiver)
    }
}
