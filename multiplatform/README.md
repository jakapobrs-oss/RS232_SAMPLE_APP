# LG TV Controller — Compose Multiplatform (ไม่มีเว็บ 100%)

โปรเจกต์นี้คือเวอร์ชัน **"แอปแท้"** ของตัวควบคุม LG TV ผ่านสาย RS232 — เขียนด้วย **Kotlin + Compose Multiplatform** ทั้งหมด ไม่มี HTML / JavaScript / WebView / เบราว์เซอร์เข้ามาเกี่ยวข้องเลยแม้แต่บรรทัดเดียว

โค้ดชุดเดียว build ออกมาได้ **2 แพลตฟอร์ม:**

| แพลตฟอร์ม | ได้อะไร | ต่อสายยังไง |
|---|---|---|
| 🖥️ **Windows** | โปรแกรม .exe เปิดเป็นหน้าต่างปกติ | notebook → USB-to-RS232 → TV |
| 📱 **Android** | ไฟล์ APK ติดตั้งบนมือถือ/แท็บเล็ต | มือถือ → OTG → USB-to-RS232 → TV |

ฟีเจอร์เหมือนแอปเดิมครบ: เลือกพอร์ต/baud rate, Power On/Off, Volume/Mute, เปลี่ยน Input, รีโมตเสมือน, Command Terminal และ **Mock Mode** (ลอง UI โดยไม่ต้องมีทีวี)

---

## 1. สิ่งที่ต้องมีก่อน

- **Android Studio** — เครื่องนี้ติดตั้งแล้วที่ `D:\Android_Studio` ✅
  (ตัว Studio มี Java (JDK 21) แถมมาในตัวที่ `D:\Android_Studio\jbr` — ไม่ต้องลง Java เพิ่ม)
- **อินเทอร์เน็ต เฉพาะการ build ครั้งแรก** — Gradle จะโหลดไลบรารีมาเก็บไว้ในเครื่อง (หลายร้อย MB ใช้เวลา 5–20 นาทีแล้วแต่เน็ต) หลังจากนั้น build/รันได้แบบออฟไลน์
- รันบน Windows: **ไม่ต้องมีมือถือ** / รันบน Android: ต้องมีมือถือจริง (Android 7+ ที่รองรับ OTG)

> โฟลเดอร์นี้เป็นโปรเจกต์อิสระ แยกจากแอป Android เดิมที่อยู่โฟลเดอร์นอก ไม่กระทบกัน

---

## 2. วิธีรันบน Windows (ทดสอบกับ TV ได้เลย ไม่ต้องมีมือถือ)

### วิธีที่ง่ายที่สุด — ดับเบิลคลิกไฟล์เดียวจบ

1. เสียบสาย USB-to-RS232 เข้า notebook (อีกปลายเข้าช่อง RS-232C ของ TV)
2. ดับเบิลคลิก **`run-desktop.bat`** (อยู่ในโฟลเดอร์นี้)
   - สคริปต์จะหา Java ให้เอง (ใช้ของ Android Studio อัตโนมัติ) แล้วสั่ง build + เปิดโปรแกรม
   - **ครั้งแรกจะช้า** เพราะโหลดไลบรารี — รอจนหน้าต่างโปรแกรมเด้งขึ้นมา ครั้งถัดไปเร็ว (~10–30 วินาที)
3. ในโปรแกรม: ช่อง **Port** เลือก `COM3 — USB-SERIAL CH340` → กด **Connect Port**
4. ดูช่อง Command Terminal — โปรแกรมจะถามสถานะทีวีอัตโนมัติ (`ka 01 FF`) ถ้าทีวีตอบ `a 01 OK01x` (เปิดอยู่) หรือ `OK00x` (standby) แปลว่าทุกอย่างทำงาน 🎉
5. ลองกดปุ่มต่างๆ ได้เลย: Power, Volume, เปลี่ยน Input, รีโมต หรือพิมพ์คำสั่งเองในช่องล่าง

### วิธีผ่าน Android Studio

1. เปิด Android Studio → **File → Open** → เลือกโฟลเดอร์ `multiplatform` นี้ (อย่าเลือกโฟลเดอร์แม่) → Trust Project
2. รอ Gradle sync เสร็จ (แถบล่างขวาหยุดวิ่ง — ครั้งแรกนานหน่อย)
3. เปิดแท็บ **Terminal** ใน Studio แล้วพิมพ์:
   ```
   .\gradlew :composeApp:run
   ```
4. หน้าต่างโปรแกรม Windows จะเด้งขึ้นมา

### วิธีผ่าน PowerShell ล้วนๆ (ไม่เปิด Studio)

```powershell
cd D:\Project\Test_rs232_Android\multiplatform
$env:JAVA_HOME = "D:\Android_Studio\jbr"
.\gradlew :composeApp:run
```

---

## 3. วิธี build เป็นไฟล์ .exe เอาไปแจก/ติดตั้งเครื่องอื่น

```powershell
cd D:\Project\Test_rs232_Android\multiplatform
$env:JAVA_HOME = "D:\Android_Studio\jbr"

# แบบ A: โฟลเดอร์โปรแกรมพร้อมใช้ (portable — ก๊อปทั้งโฟลเดอร์ไปเครื่องไหนก็รันได้ ไม่ต้องลง Java)
.\gradlew :composeApp:createDistributable
# ผลลัพธ์: composeApp\build\compose\binaries\main\app\LgTvController\LgTvController.exe

# แบบ B: ตัวติดตั้ง .msi (ดับเบิลคลิกติดตั้ง มี shortcut ใน Start Menu)
.\gradlew :composeApp:packageMsi
# ผลลัพธ์: composeApp\build\compose\binaries\main\msi\LgTvController-1.0.0.msi
```

> ถ้าเจอ error ประมาณ `jpackage not found` ให้ลง JDK 17 ตัวเต็ม (เช่น Eclipse Temurin จาก adoptium.net) แล้วชี้ `$env:JAVA_HOME` ไปที่ตัวนั้นแทน — เฉพาะตอน package เท่านั้น ตอน run ธรรมดาใช้ Java ของ Studio ได้

---

## 4. วิธีรันบน Android (เมื่อมีมือถือ)

**เตรียมมือถือครั้งเดียว:** Settings → About phone → กด **Build number** 7 ครั้ง → กลับมาที่ Developer options → เปิด **USB debugging**

**รัน:**
1. เสียบมือถือกับ notebook ด้วยสาย USB → กด Allow บนจอมือถือ
2. เปิดโฟลเดอร์ `multiplatform` ใน Android Studio → รอ sync
3. ดู dropdown ด้านบน: เลือก run configuration **composeApp** + ชื่อมือถือ → กด **Run ▶**
   หรือใช้คำสั่ง: `.\gradlew :composeApp:installDebug`
4. ตอนทดสอบกับทีวีจริง: ถอดสายจาก notebook → เสียบ **OTG + USB-RS232 → TV** เข้ามือถือแทน
   (จะ deploy แอปต่อโดยไม่ใช้สาย ให้เปิด **Wireless debugging** ใน Developer options แล้ว pair กับ Studio ผ่าน Wi-Fi วงเดียวกัน)
5. พอเสียบอะแดปเตอร์ แอปจะถามสิทธิ์ USB → กด OK → Connect

---

## 5. หน้าตาโปรเจกต์ — แก้อะไรที่ไฟล์ไหน

```
multiplatform/
├── run-desktop.bat                ← ดับเบิลคลิกรันบน Windows
├── settings.gradle.kts            ← รายชื่อ module + ที่อยู่ repository
├── build.gradle.kts               ← ประกาศ plugin (ระดับ root)
├── gradle/libs.versions.toml      ← เวอร์ชันไลบรารีทั้งหมด รวมที่เดียว
└── composeApp/
    ├── build.gradle.kts           ← ตั้งค่า build ทั้ง Android + Windows
    └── src/
        ├── commonMain/kotlin/com/lgtv/rs232/cmp/
        │   ├── App.kt                  ← 🎨 หน้าจอ UI ทั้งหมด (ใช้ร่วม 2 แพลตฟอร์ม)
        │   ├── ControllerState.kt      ← 🧠 logic การทำงาน (เทียบเท่า app.js ของแอปเดิม)
        │   └── serial/
        │       ├── LgProtocol.kt       ← 📜 โปรโตคอล LG (รูปแบบคำสั่ง/คำตอบ)
        │       └── SerialLink.kt       ← interface กลางของการคุย serial
        ├── desktopMain/kotlin/.../
        │   ├── main.kt                 ← จุดเริ่มโปรแกรม Windows
        │   └── serial/DesktopSerialLink.kt  ← คุย COM port (ไลบรารี jSerialComm)
        └── androidMain/kotlin/.../
            ├── MainActivity.kt         ← จุดเริ่มแอป Android
            └── serial/AndroidSerialLink.kt   ← คุย USB OTG (usb-serial-for-android)
```

**อยากเพิ่มปุ่ม/คำสั่งใหม่?** แก้ที่ `App.kt` (ปุ่ม) + `ControllerState.kt` (พฤติกรรม) ที่เดียว ได้ผลทั้ง Windows และ Android
**อยากแก้เรื่องพอร์ต/การเชื่อมต่อ?** ฝั่ง Windows แก้ `DesktopSerialLink.kt` ฝั่งมือถือแก้ `AndroidSerialLink.kt`

---

## 6. แก้ปัญหาที่เจอบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| `JAVA_HOME is not set` หรือหา Java ไม่เจอ | ใช้ `run-desktop.bat` (หา Java ให้เอง) หรือรัน `$env:JAVA_HOME = "D:\Android_Studio\jbr"` ก่อน |
| build ครั้งแรกค้างนาน | ปกติ — กำลังโหลดไลบรารี รอจนจบ (ดู progress ใน terminal) ครั้งต่อไปเร็ว |
| ขึ้น warning เวอร์ชัน Kotlin/AGP ตอน build | เป็นแค่คำเตือนความเข้ากัน ไม่ใช่ error — build ต่อได้ปกติ |
| กด Connect แล้ว `Failed to open COM3` | มีโปรแกรมอื่นเปิดพอร์ตค้าง (เช่น หน้าเว็บเทสเก่า, PuTTY, สคริปต์) — ปิดให้หมด หรือถอดสาย USB เสียบใหม่ |
| เปิดพอร์ตไม่ได้ ขึ้น "device not functioning" | อาการชิป CH340 ค้าง — ถอดสาย USB รอ 3 วิ เสียบใหม่ (เปลี่ยนช่อง USB ได้ยิ่งดี) |
| เชื่อมต่อได้แต่ทีวีไม่ตอบ | เช็ค: ทีวีเสียบปลั๊ก (standby ก็ตอบได้), ลองเปลี่ยน TV Set ID เป็น `0`, เสียบช่อง **RS-232C IN (Control/Service)**, บางรุ่นต้องใช้หัวแปลง null modem |
| ไม่มีทีวี/สายให้ลอง | กดปุ่ม **Enable Mock Mode** — UI ทำงานครบโดยจำลองคำตอบทีวี |
| Android: ไม่เจอ device | เช็คสาย OTG, มือถือบางรุ่นต้องเปิด "OTG" ใน Settings ก่อน |

---

## 7. โน้ตเรื่องโปรโตคอล LG RS232 (อ้างอิงเร็ว)

- ตั้งค่าพอร์ต: **9600 bps, 8 data bits, no parity, 1 stop bit, ไม่มี flow control**
- รูปแบบคำสั่ง: `[cmd] [SetID] [Data]` + Enter เช่น `ka 01 FF`
- คำสั่งที่ใช้บ่อย: `ka` power (01=on, 00=off, FF=ถามสถานะ) · `kf` volume (00–64 hex) · `ke` mute · `xb` input (90=HDMI1) · `mc` ยิงปุ่มรีโมต
- ทีวีตอบ: `a 01 OK01x` = สำเร็จ / `... NG...x` = ทีวีไม่รับคำสั่งนั้น
