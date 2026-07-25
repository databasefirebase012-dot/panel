package com.example.service.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.ThemePreferences

@Composable
fun AimFovOverlay() {
    val fovEnabled by ThemePreferences.fovEnabledFlow.collectAsState()
    val fovRadius by ThemePreferences.fovRadiusFlow.collectAsState()
    val fovColor by ThemePreferences.fovColorFlow.collectAsState()

    if (!fovEnabled) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerPoint = center
        // Draw crosshair overlay circle
        drawCircle(
            color = fovColor,
            radius = fovRadius,
            center = centerPoint,
            style = Stroke(width = 3f)
        )
        // Center dot
        drawCircle(
            color = fovColor,
            radius = 4f,
            center = centerPoint
        )
    }
}
