package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ThemePreferences
import com.example.service.FloatingPanelService
import com.example.shizuku.ShizukuCommandRunner
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_REQ_CODE) {
            val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Shizuku Permission Denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ThemePreferences.init(applicationContext)

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContent {
            MyApplicationTheme {
                MainScreen(
                    onRequestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    },
                    onRequestShizukuPermission = {
                        if (ShizukuCommandRunner.isShizukuAvailable()) {
                            try {
                                Shizuku.requestPermission(SHIZUKU_REQ_CODE)
                            } catch (e: Throwable) {
                                Toast.makeText(this, "Failed to request Shizuku: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Shizuku Service is not running!", Toast.LENGTH_LONG).show()
                        }
                    },
                    onStartFloatingPanel = {
                        FloatingPanelService.startService(applicationContext)
                        moveTaskToBack(true)
                    },
                    onStopFloatingPanel = {
                        FloatingPanelService.stopService(applicationContext)
                    }
                )
            }
        }
    }


    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    companion object {
        private const val SHIZUKU_REQ_CODE = 1001
    }
}

@Composable
fun MainScreen(
    onRequestOverlayPermission: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onStartFloatingPanel: () -> Unit,
    onStopFloatingPanel: () -> Unit
) {
    val context = LocalContext.current
    val themeConfig by ThemePreferences.themeFlow.collectAsState()
    val selectedGame by ThemePreferences.selectedGameFlow.collectAsState()
    val isServiceRunning by FloatingPanelService.isRunningFlow.collectAsState()

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isShizukuAvailable by remember { mutableStateOf(ShizukuCommandRunner.isShizukuAvailable()) }
    var hasShizukuPermission by remember { mutableStateOf(ShizukuCommandRunner.hasPermission()) }


    // Periodic check for permission status updates
    LaunchedEffect(Unit) {
        while (true) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
            isShizukuAvailable = ShizukuCommandRunner.isShizukuAvailable()
            hasShizukuPermission = ShizukuCommandRunner.hasPermission()
            delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = themeConfig.backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // App Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(themeConfig.primaryColor.copy(alpha = 0.15f))
                    .border(2.dp, themeConfig.primaryColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Logo",
                    tint = themeConfig.primaryColor,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "ZEPHYR PANEL",
                color = themeConfig.primaryColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 1.sp
            )

            Text(
                text = "Silent Shizuku-Powered Gaming Floating Tool",
                color = themeConfig.highlightColor.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Permissions Card Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, themeConfig.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SYSTEM PERMISSIONS",
                        color = themeConfig.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    // 1. Overlay Permission Item
                    PermissionRow(
                        title = "Display Over Other Apps",
                        subtitle = "Required to show floating panel",
                        isGranted = hasOverlayPermission,
                        themeConfig = themeConfig,
                        onRequest = onRequestOverlayPermission
                    )

                    HorizontalDivider(color = themeConfig.primaryColor.copy(alpha = 0.15f))

                    // 2. Shizuku Permission Item
                    PermissionRow(
                        title = "Shizuku ADB Permission",
                        subtitle = if (!isShizukuAvailable) "Shizuku Service Not Running!" else "Required for silent shell execution",
                        isGranted = hasShizukuPermission,
                        themeConfig = themeConfig,
                        onRequest = onRequestShizukuPermission
                    )
                }
            }

            // Game Target Selector Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = themeConfig.cardBgColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, themeConfig.primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TARGET GAME SELECTOR",
                        color = themeConfig.primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    val games = listOf(
                        "FREE FIRE ORI" to ThemePreferences.GAME_FREE_FIRE_ORI,
                        "FREE FIRE MAX" to ThemePreferences.GAME_FREE_FIRE_MAX
                    )

                    games.forEach { (label, pkg) ->
                        val isSelected = selectedGame == pkg
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) themeConfig.primaryColor.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    ThemePreferences.saveSelectedGame(context, pkg)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { ThemePreferences.saveSelectedGame(context, pkg) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = themeConfig.primaryColor,
                                    unselectedColor = themeConfig.highlightColor.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = label,
                                    color = themeConfig.highlightColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = pkg,
                                    color = themeConfig.highlightColor.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // START / STOP BUTTON
            val canStart = hasOverlayPermission && hasShizukuPermission
            Button(
                onClick = {
                    if (isServiceRunning) {
                        onStopFloatingPanel()
                    } else {
                        if (canStart) {
                            onStartFloatingPanel()
                        } else {
                            Toast.makeText(
                                context,
                                "Please grant all permissions first!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isServiceRunning) Color(0xFFFF0055)
                    else if (canStart) themeConfig.primaryColor
                    else themeConfig.primaryColor.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isServiceRunning) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isServiceRunning) "STOP ZEPHYR PANEL" else "START ZEPHYR PANEL",
                    color = if (isServiceRunning) Color.White else Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }


            Spacer(modifier = Modifier.weight(1f))

            // Branding Watermark
            Text(
                text = "Developer: XRANS OFFICIAL",
                color = themeConfig.highlightColor.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    themeConfig: com.example.data.ZephyrThemeConfig,
    onRequest: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = themeConfig.highlightColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = if (isGranted) themeConfig.primaryColor else themeConfig.highlightColor.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = themeConfig.primaryColor,
                modifier = Modifier.size(28.dp)
            )
        } else {
            OutlinedButton(
                onClick = onRequest,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeConfig.primaryColor),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(themeConfig.primaryColor)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}
