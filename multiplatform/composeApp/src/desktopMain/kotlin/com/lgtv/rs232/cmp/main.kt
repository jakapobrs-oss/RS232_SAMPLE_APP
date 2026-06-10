package com.lgtv.rs232.cmp

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.lgtv.rs232.cmp.serial.DesktopSerialLink

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LG TV Controller (RS232)",
        state = rememberWindowState(width = 1150.dp, height = 820.dp)
    ) {
        val link = remember { DesktopSerialLink() }
        DisposableEffect(Unit) {
            onDispose { link.disconnect() } // ปิดพอร์ตตอนปิดหน้าต่าง ไม่งั้นพอร์ตค้าง
        }
        App(link)
    }
}
