package com.lgtv.rs232.cmp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun timeNow(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
