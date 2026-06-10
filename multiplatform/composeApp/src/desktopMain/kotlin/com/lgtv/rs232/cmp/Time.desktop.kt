package com.lgtv.rs232.cmp

import java.text.SimpleDateFormat
import java.util.Date

actual fun timeNow(): String = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
