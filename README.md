# RS232_SAMPLE_APP — RS232 TV Controller (Android / WebView)

แอป **Android** (sample) สำหรับสั่งงานทีวีผ่านสาย RS232 (USB-to-RS232 + OTG)
**แบรนด์อ้างอิงตัวแรกคือ LG** (โปรโตคอลในหน้าเว็บ + `tools/Test-LgTv.ps1`) — แบรนด์อื่นทยอยเพิ่มทีหลัง
สถาปัตยกรรมแบบ **WebView**: เปลือกเป็น Kotlin native ที่เปิด WebView โหลดหน้าเว็บ UI
ที่ฝังมาในแอป แล้วเปิด **USB-serial** ให้หน้าเว็บยิงคำสั่งออกพอร์ตได้

> คู่กับ repo **RS232_SAMPLE_WEB** (ตัว web command เดี่ยวๆ รันบนเบราว์เซอร์ได้)

## โครงสร้าง

```
app/src/main/
├── java/com/rs232sample/app/MainActivity.kt  ← เปลือก native: WebView + bridge USB-serial
└── assets/www/                           ← 🔁 หน้าเว็บ UI (สำเนาจาก RS232_SAMPLE_WEB)
    ├── index.html  app.js  styles.css
    ├── serial-web-shim.js                ← no-op บน Android (native bridge ชนะ)
    └── lib/fontawesome/
tools/                                    ← สคริปต์เทสโปรโตคอลเฟส 1 (ดู tools/README.md)
```

### สะพานเชื่อม web ↔ native (contract เดียวกับฝั่งเว็บ)

- JS เรียก native: `AndroidSerial.connect(baud)` / `.send(data)` / `.disconnect()`
- native เรียกกลับ JS: `window.onSerialConnected(ok,msg)` / `onSerialDataReceived(str)` / `onSerialDisconnected()`

หน้าเว็บชุดเดียวกันนี้รันบนเบราว์เซอร์ desktop ก็ได้ (ผ่าน `serial-web-shim.js` + Web Serial API) —
บน Android ตัว native bridge ถูก inject ก่อน หน้าเว็บจึงใช้ของจริงอัตโนมัติ

## ⚠️ เรื่อง www/ ซ้ำกับ RS232_SAMPLE_WEB

`assets/www/` ในนี้เป็น **สำเนา** ของ RS232_SAMPLE_WEB (จงใจ เพื่อให้แอป build/รัน offline ได้จบในตัว)
**แก้ปุ่ม/คำสั่ง/หน้าตาเว็บที่ฝั่งไหน อย่าลืม sync อีกฝั่งด้วย** ไม่งั้นสองที่จะเพี้ยนกัน (drift)

## วิธี build / รัน

1. เปิดโฟลเดอร์นี้ใน **Android Studio** (จะ gen `local.properties` ชี้ Android SDK ให้เอง)
2. รอ Gradle sync เสร็จ
3. เสียบมือถือ/กล่อง Android (เปิด USB debugging) → กด **Run ▶** หรือ `.\gradlew installDebug`
4. ต่อทีวี: **OTG → USB-to-RS232 → ช่อง RS-232C ของ TV** → เปิดแอป กด Connect (แอปจะขอสิทธิ์ USB)

> ทดสอบฮาร์ดแวร์/โปรโตคอลก่อนโดยไม่ต้องมีแอป: ใช้สคริปต์ใน `tools/` (เช่น `tools/Test-LgTv.ps1`)

## หมายเหตุ

- เวอร์ชัน **Compose Multiplatform** (แอป native ล้วน Android+Windows ไม่มีเว็บ) ถูกแยกเก็บไว้ที่ repo เดิม
  `Test_rs232_Android` (เป็น archive) ไม่ได้รวมมาใน repo นี้
- `local.properties` ถูก gitignore (เป็น path SDK เฉพาะเครื่อง — Android Studio สร้างให้เอง)
