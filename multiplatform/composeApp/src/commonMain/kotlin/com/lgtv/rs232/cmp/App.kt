@file:OptIn(ExperimentalMaterial3Api::class)

package com.lgtv.rs232.cmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lgtv.rs232.cmp.serial.SerialLink
import kotlin.math.roundToInt

private val inputSources = listOf(
    "HDMI 1" to "90", "HDMI 2" to "91", "HDMI 3" to "92",
    "HDMI 4" to "93", "AV 1" to "20", "RGB PC" to "60"
)

// (label, IR keycode สำหรับคำสั่ง mc)
private val remoteActions = listOf(
    "Back" to "28", "Exit" to "5B", "Menu" to "43", "Home" to "7C"
)

@Composable
fun App(link: SerialLink) {
    val scope = rememberCoroutineScope()
    val state = remember { ControllerState(link, scope) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4F8CFF),
            background = Color(0xFF0F141E),
            surface = Color(0xFF18202E),
            surfaceVariant = Color(0xFF222B3C)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                HeaderBar(state)
                Spacer(Modifier.height(12.dp))
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    if (maxWidth < 760.dp) {
                        // จอแคบ (มือถือแนวตั้ง) — เรียงคอลัมน์เดียว
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ConnectionPanel(state)
                            PowerVolumePanel(state)
                            InputPanel(state)
                            RemotePanel(state)
                            TerminalPanel(state, Modifier.height(420.dp))
                        }
                    } else {
                        // จอกว้าง (Windows / แท็บเล็ต) — สองคอลัมน์เหมือนเว็บเดิม
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ConnectionPanel(state)
                                PowerVolumePanel(state)
                                InputPanel(state)
                            }
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RemotePanel(state)
                                TerminalPanel(state, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar(state: ControllerState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("LG TV Controller", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        val (statusText, dotColor) = when {
            state.connected -> "Connected" to Color(0xFF2ECC71)
            state.mockMode -> "Mocking" to Color(0xFFFFC857)
            else -> "Disconnected" to Color(0xFFE74C3C)
        }
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(9.dp).background(dotColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(statusText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun Panel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "—",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ConnectionPanel(state: ControllerState) {
    Panel("🔌 Connection Setup") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DropdownField(
                label = "Baud Rate",
                options = listOf(9600, 19200, 115200),
                selected = state.baudRate,
                optionLabel = { "$it bps" },
                enabled = !state.connected,
                modifier = Modifier.weight(1f)
            ) { state.baudRate = it }

            var setIdText by remember { mutableStateOf(state.setId.toString()) }
            OutlinedTextField(
                value = setIdText,
                onValueChange = { input ->
                    setIdText = input.filter { it.isDigit() }.take(2)
                    state.setId = setIdText.toIntOrNull() ?: 0
                },
                label = { Text("TV Set ID") },
                singleLine = true,
                enabled = !state.connected,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            DropdownField(
                label = "Port",
                options = state.ports,
                selected = state.ports.firstOrNull { it.id == state.selectedPortId },
                optionLabel = { it.label },
                enabled = !state.connected,
                modifier = Modifier.weight(1f)
            ) { state.selectedPortId = it.id }

            OutlinedButton(onClick = { state.refreshPorts() }, enabled = !state.connected) {
                Text("↻ Refresh")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { state.connect() },
                enabled = !state.controlsEnabled,
                modifier = Modifier.weight(1f)
            ) { Text("Connect Port") }

            OutlinedButton(
                onClick = { state.disconnect() },
                enabled = state.connected,
                modifier = Modifier.weight(1f)
            ) { Text("Disconnect") }
        }

        Button(
            onClick = { state.toggleMock() },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.mockMode) Color(0xFFFFC857) else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (state.mockMode) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.mockMode) "Mock Mode Active — click to disable" else "Enable Mock Mode (Test UI without TV)")
        }
    }
}

@Composable
private fun PowerVolumePanel(state: ControllerState) {
    Panel("⚡ Power & Volume") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { state.sendCommand("ka", "01") },
                enabled = state.controlsEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71), contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) { Text("Power On") }

            Button(
                onClick = { state.sendCommand("ka", "00") },
                enabled = state.controlsEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C), contentColor = Color.White),
                modifier = Modifier.weight(1f)
            ) { Text("Power Off") }
        }

        var sliderPos by remember { mutableStateOf(state.volume.toFloat()) }
        LaunchedEffect(state.volume) { sliderPos = state.volume.toFloat() }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Vol", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = sliderPos,
                onValueChange = { sliderPos = it },
                onValueChangeFinished = { state.applyVolume(sliderPos.roundToInt()) },
                valueRange = 0f..100f,
                enabled = state.controlsEnabled,
                modifier = Modifier.weight(1f)
            )
            Text(
                sliderPos.roundToInt().toString(),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(36.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { state.sendCommand("mc", "03") }, // IR: Volume down
                enabled = state.controlsEnabled,
                modifier = Modifier.weight(1f)
            ) { Text("− Vol") }

            OutlinedButton(
                onClick = { state.toggleMute() },
                enabled = state.controlsEnabled,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.muted) "Unmute" else "Mute") }

            OutlinedButton(
                onClick = { state.sendCommand("mc", "02") }, // IR: Volume up
                enabled = state.controlsEnabled,
                modifier = Modifier.weight(1f)
            ) { Text("+ Vol") }
        }
    }
}

@Composable
private fun InputPanel(state: ControllerState) {
    Panel("📺 Input Source") {
        inputSources.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (label, hex) ->
                    OutlinedButton(
                        onClick = { state.sendCommand("xb", hex) },
                        enabled = state.controlsEnabled,
                        modifier = Modifier.weight(1f)
                    ) { Text(label) }
                }
            }
        }
    }
}

@Composable
private fun RemotePanel(state: ControllerState) {
    Panel("🎮 Virtual Remote") {
        // ปุ่มทิศทาง 3x3 — ช่องว่างที่มุมใช้ null
        val dpad = listOf(
            null, "▲" to "40", null,
            "◀" to "07", "OK" to "44", "▶" to "06",
            null, "▼" to "41", null
        )
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            dpad.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item ->
                        if (item == null) {
                            Spacer(Modifier.size(64.dp))
                        } else {
                            val (label, hex) = item
                            FilledTonalButton(
                                onClick = { state.sendCommand("mc", hex) },
                                enabled = state.controlsEnabled,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(64.dp)
                            ) { Text(label) }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            remoteActions.forEach { (label, hex) ->
                OutlinedButton(
                    onClick = { state.sendCommand("mc", hex) },
                    enabled = state.controlsEnabled,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(label, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun TerminalPanel(state: ControllerState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "> Command Terminal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = { state.clearLogs() }) { Text("Clear") }
            }

            val listState = rememberLazyListState()
            LaunchedEffect(state.logs.size) {
                if (state.logs.isNotEmpty()) listState.scrollToItem(state.logs.size - 1)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F17), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                items(state.logs) { entry ->
                    val color = when (entry.kind) {
                        LogKind.INFO -> Color(0xFF9AA7B8)
                        LogKind.TX -> Color(0xFFFFC857)
                        LogKind.RX -> Color(0xFF7CE38B)
                        LogKind.ERROR -> Color(0xFFFF6B6B)
                    }
                    Text(
                        "[${entry.time}] ${entry.tag}: ${entry.message}",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }

            var rawText by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("e.g. ka 01 FF") },
                    singleLine = true,
                    enabled = state.controlsEnabled,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        state.sendRaw(rawText)
                        rawText = ""
                    },
                    enabled = state.controlsEnabled
                ) { Text("Send") }
            }
        }
    }
}
