package com.example.service.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemePreferences
import com.example.shizuku.ShizukuCommandRunner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ZephyrFloatingPanel(
    onCloseService: () -> Unit,
    onUpdatePosition: (Float, Float) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeConfig by ThemePreferences.themeFlow.collectAsState()
    val selectedGame by ThemePreferences.selectedGameFlow.collectAsState()

    var isMinimized by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Main features, 1: Settings/Theme

    // Failsafe timer state
    var isFailsafeActive by remember { mutableStateOf(false) }
    var failsafeSeconds by remember { mutableIntStateOf(10) }
    var failsafeJob by remember { mutableStateOf<Job?>(null) }

    // Helper for running commands silently
    val runSilentCommand: (String) -> Unit = { cmd ->
        coroutineScope.launch {
            ShizukuCommandRunner.executeCommandSilent(cmd)
        }
    }

    // Helper for starting Failsafe timer
    val triggerFailsafe: () -> Unit = {
        failsafeJob?.cancel()
        isFailsafeActive = true
        failsafeSeconds = 10
        failsafeJob = coroutineScope.launch {
            while (failsafeSeconds > 0) {
                delay(1000)
                failsafeSeconds--
            }
            // Timer expired without user keeping changes: reset wm size & density
            ShizukuCommandRunner.executeCommandsSilent(
                listOf("wm size reset", "wm density reset")
            )
            isFailsafeActive = false
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            if (isMinimized) {
                // Minimized Floating Widget Icon
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(themeConfig.backgroundColor)
                        .border(2.dp, themeConfig.primaryColor, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onUpdatePosition(dragAmount.x, dragAmount.y)
                            }
                        }
                        .clickable { isMinimized = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = themeConfig.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            } else {
                // Expanded Panel
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = themeConfig.backgroundColor.copy(alpha = themeConfig.panelOpacity)
                    ),
                    modifier = Modifier
                        .width(320.dp)
                        .border(1.5.dp, themeConfig.primaryColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Header with Drag handle and controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        onUpdatePosition(dragAmount.x, dragAmount.y)
                                    }
                                }
                                .padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag",
                                tint = themeConfig.highlightColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZEPHYR PANEL",
                                color = themeConfig.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { activeTab = if (activeTab == 0) 1 else 0 },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeTab == 0) Icons.Default.Settings else Icons.Default.Tune,
                                    contentDescription = "Settings",
                                    tint = themeConfig.highlightColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { isMinimized = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Minimize",
                                    tint = themeConfig.highlightColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onCloseService,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = themeConfig.primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = themeConfig.primaryColor.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Body Content
                        Box(
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 10.dp)
                        ) {
                            if (activeTab == 0) {
                                MainFeaturesSection(
                                    context = context,
                                    themeConfig = themeConfig,
                                    selectedGame = selectedGame,
                                    runSilentCommand = runSilentCommand,
                                    triggerFailsafe = triggerFailsafe
                                )
                            } else {
                                SettingsThemeSection(
                                    context = context,
                                    themeConfig = themeConfig
                                )
                            }
                        }

                        HorizontalDivider(
                            color = themeConfig.primaryColor.copy(alpha = 0.3f),
                            thickness = 1.dp
                        )

                        // Watermark Footer
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Developer: XRANS OFFICIAL",
                                color = themeConfig.highlightColor.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Failsafe Keep Changes Dialog / Overlay
            if (isFailsafeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(2.dp, themeConfig.primaryColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Keep Changes? ($failsafeSeconds s)",
                            color = themeConfig.highlightColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    failsafeJob?.cancel()
                                    isFailsafeActive = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.primaryColor)
                            ) {
                                Text("Keep", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    failsafeJob?.cancel()
                                    isFailsafeActive = false
                                    coroutineScope.launch {
                                        ShizukuCommandRunner.executeCommandsSilent(
                                            listOf("wm size reset", "wm density reset")
                                        )
                                    }
                                }
                            ) {
                                Text("Revert", color = themeConfig.highlightColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainFeaturesSection(
    context: Context,
    themeConfig: com.example.data.ZephyrThemeConfig,
    selectedGame: String,
    runSilentCommand: (String) -> Unit,
    triggerFailsafe: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val fovEnabled by ThemePreferences.fovEnabledFlow.collectAsState()
    val fovRadius by ThemePreferences.fovRadiusFlow.collectAsState()
    val fovColor by ThemePreferences.fovColorFlow.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // 1. AIM FOV
        SectionTitle("1. Aim FOV", themeConfig)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable Overlay Circle", color = themeConfig.highlightColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(
                checked = fovEnabled,
                onCheckedChange = {
                    ThemePreferences.saveFovConfig(context, it, fovRadius, fovColor)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeConfig.primaryColor,
                    checkedTrackColor = themeConfig.primaryColor.copy(alpha = 0.4f)
                )
            )
        }

        if (fovEnabled) {
            Text("Radius: ${fovRadius.toInt()} px", color = themeConfig.highlightColor, fontSize = 12.sp)
            Slider(
                value = fovRadius,
                onValueChange = {
                    ThemePreferences.saveFovConfig(context, true, it, fovColor)
                },
                valueRange = 20f..300f,
                colors = SliderDefaults.colors(
                    thumbColor = themeConfig.primaryColor,
                    activeTrackColor = themeConfig.primaryColor
                )
            )

            // RGB Color selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Color:", color = themeConfig.highlightColor, fontSize = 12.sp)
                val colorList = listOf(
                    Color(0xFF39FF14), // Green Light
                    Color(0xFFFF0055), // Neon Red
                    Color(0xFF00E5FF), // Cyan
                    Color(0xFFFFFF00), // Yellow
                    Color(0xFFFFFFFF)  // White
                )
                colorList.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (fovColor == color) 2.dp else 0.dp,
                                color = themeConfig.highlightColor,
                                shape = CircleShape
                            )
                            .clickable {
                                ThemePreferences.saveFovConfig(context, true, fovRadius, color)
                            }
                    )
                }
            }
        }

        HorizontalDivider(color = themeConfig.primaryColor.copy(alpha = 0.2f))

        // 2. DOWNSCALE
        SectionTitle("2. Downscale (Game Overlay)", themeConfig)
        Text("Game: $selectedGame", color = themeConfig.highlightColor.copy(alpha = 0.7f), fontSize = 11.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("90%" to "0.9", "80%" to "0.8", "70%" to "0.7").forEach { (label, factor) ->
                OutlinedButton(
                    onClick = {
                        val cmd = "device_config put game_overlay $selectedGame mode=2,downscaleFactor=$factor"
                        runSilentCommand(cmd)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    Text(label, color = themeConfig.primaryColor, fontSize = 11.sp)
                }
            }
        }
        OutlinedButton(
            onClick = {
                val cmd = "device_config delete game_overlay $selectedGame"
                runSilentCommand(cmd)
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(4.dp)
        ) {
            Text("Reset Downscale", color = themeConfig.highlightColor, fontSize = 11.sp)
        }

        HorizontalDivider(color = themeConfig.primaryColor.copy(alpha = 0.2f))

        // 3. AIM RESOLUTION
        SectionTitle("3. Aim Resolution (WM Size & Density)", themeConfig)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    runSilentCommand("wm size 1280x720")
                    triggerFailsafe()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("PC Ratio", color = themeConfig.primaryColor, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    runSilentCommand("wm size 1080x1080")
                    triggerFailsafe()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Stretch", color = themeConfig.primaryColor, fontSize = 11.sp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    runSilentCommand("wm density 480")
                    triggerFailsafe()
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("High DPI", color = themeConfig.primaryColor, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        ShizukuCommandRunner.executeCommandsSilent(
                            listOf("wm size reset", "wm density reset")
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                Text("Reset WM", color = themeConfig.highlightColor, fontSize = 11.sp)
            }
        }

        HorizontalDivider(color = themeConfig.primaryColor.copy(alpha = 0.2f))

        // 4. BOOST RAM
        SectionTitle("4. Boost RAM", themeConfig)
        Button(
            onClick = {
                coroutineScope.launch {
                    ShizukuCommandRunner.executeCommandsSilent(
                        listOf(
                            "am kill-all",
                            "pm trim-caches 999999999"
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeConfig.primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clean RAM & Cache", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        HorizontalDivider(color = themeConfig.primaryColor.copy(alpha = 0.2f))

        // 5. AIM LEGIT
        SectionTitle("5. Aim Legit (Touch Responsiveness)", themeConfig)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    runSilentCommand("settings put system pointer_speed 7")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f)
            ) {
                Text("Boost Pointer", color = themeConfig.primaryColor, fontSize = 11.sp)
            }
            Button(
                onClick = {
                    runSilentCommand("settings put system pointer_speed 0")
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset Pointer", color = themeConfig.highlightColor, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SettingsThemeSection(
    context: Context,
    themeConfig: com.example.data.ZephyrThemeConfig
) {
    var primaryHex by remember { mutableStateOf("#39FF14") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle("UI Theme Settings", themeConfig)

        Text("Select Preset Theme:", color = themeConfig.highlightColor, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Preset 1: Green Cyber
            Button(
                onClick = {
                    ThemePreferences.saveTheme(context, Color(0xFF39FF14), Color(0xFF000000), Color(0xFFFFFFFF))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Green", color = Color.Black, fontSize = 10.sp)
            }
            // Preset 2: Cyan Cyber
            Button(
                onClick = {
                    ThemePreferences.saveTheme(context, Color(0xFF00E5FF), Color(0xFF050B14), Color(0xFFFFFFFF))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cyan", color = Color.Black, fontSize = 10.sp)
            }
            // Preset 3: Red Neon
            Button(
                onClick = {
                    ThemePreferences.saveTheme(context, Color(0xFFFF0055), Color(0xFF100205), Color(0xFFFFFFFF))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                modifier = Modifier.weight(1f)
            ) {
                Text("Red", color = Color.White, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Custom Accent Color:", color = themeConfig.highlightColor, fontSize = 12.sp)
        OutlinedTextField(
            value = primaryHex,
            onValueChange = { primaryHex = it },
            label = { Text("Color Hex (e.g. #39FF14)", color = themeConfig.highlightColor) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeConfig.primaryColor,
                unfocusedBorderColor = themeConfig.highlightColor.copy(alpha = 0.5f),
                focusedTextColor = themeConfig.highlightColor,
                unfocusedTextColor = themeConfig.highlightColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                try {
                    val parsed = Color(android.graphics.Color.parseColor(primaryHex))
                    ThemePreferences.saveTheme(context, parsed, themeConfig.backgroundColor, themeConfig.highlightColor)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = themeConfig.primaryColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Color", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String, themeConfig: com.example.data.ZephyrThemeConfig) {
    Text(
        text = title,
        color = themeConfig.primaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}
