package com.lgtv.rs232.cmp.serial

/** ข้อมูลพอร์ตที่เลือกได้ — Windows: COM port, Android: USB device ที่เสียบผ่าน OTG */
data class PortInfo(val id: String, val label: String)

interface SerialListener {
    fun onConnected(success: Boolean, message: String)
    fun onDisconnected()
    fun onDataReceived(data: String)
}

/**
 * ชั้นกลางของการคุย serial — interface เดียวกับ AndroidSerial bridge ของแอปเดิม
 * แต่ละแพลตฟอร์ม implement เอง (Windows: jSerialComm, Android: usb-serial-for-android)
 */
interface SerialLink {
    var listener: SerialListener?
    fun listPorts(): List<PortInfo>
    fun connect(portId: String?, baudRate: Int)
    fun disconnect()
    fun send(text: String)
}
