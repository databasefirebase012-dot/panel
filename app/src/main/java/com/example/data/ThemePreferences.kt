package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ZephyrThemeConfig(
    val primaryColor: Color = Color(0xFF39FF14), // Green Light
    val backgroundColor: Color = Color(0xFF000000), // Pure Black
    val highlightColor: Color = Color(0xFFFFFFFF), // White Light
    val cardBgColor: Color = Color(0xFF101410),
    val panelOpacity: Float = 0.95f
)

object ThemePreferences {
    private const val PREF_NAME = "zephyr_panel_prefs"
    private const val KEY_PRIMARY_COLOR = "primary_color"
    private const val KEY_BG_COLOR = "bg_color"
    private const val KEY_HIGHLIGHT_COLOR = "highlight_color"
    private const val KEY_SELECTED_GAME = "selected_game"
    private const val KEY_FOV_RADIUS = "fov_radius"
    private const val KEY_FOV_COLOR = "fov_color"
    private const val KEY_FOV_ENABLED = "fov_enabled"
    private const val KEY_FOV_OFFSET_X = "fov_offset_x"
    private const val KEY_FOV_OFFSET_Y = "fov_offset_y"

    const val GAME_FREE_FIRE_ORI = "com.dts.freefireth"
    const val GAME_FREE_FIRE_MAX = "com.dts.freefiremax"

    private val _themeFlow = MutableStateFlow(ZephyrThemeConfig())
    val themeFlow: StateFlow<ZephyrThemeConfig> = _themeFlow

    private val _selectedGameFlow = MutableStateFlow(GAME_FREE_FIRE_ORI)
    val selectedGameFlow: StateFlow<String> = _selectedGameFlow

    private val _fovRadiusFlow = MutableStateFlow(100f)
    val fovRadiusFlow: StateFlow<Float> = _fovRadiusFlow

    private val _fovColorFlow = MutableStateFlow(Color(0xFF39FF14))
    val fovColorFlow: StateFlow<Color> = _fovColorFlow

    private val _fovEnabledFlow = MutableStateFlow(false)
    val fovEnabledFlow: StateFlow<Boolean> = _fovEnabledFlow

    private val _fovOffsetXFlow = MutableStateFlow(0f)
    val fovOffsetXFlow: StateFlow<Float> = _fovOffsetXFlow

    private val _fovOffsetYFlow = MutableStateFlow(0f)
    val fovOffsetYFlow: StateFlow<Float> = _fovOffsetYFlow

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val primary = prefs.getInt(KEY_PRIMARY_COLOR, 0xFF39FF14.toInt())
        val bg = prefs.getInt(KEY_BG_COLOR, 0xFF000000.toInt())
        val highlight = prefs.getInt(KEY_HIGHLIGHT_COLOR, 0xFFFFFFFF.toInt())
        val game = prefs.getString(KEY_SELECTED_GAME, GAME_FREE_FIRE_ORI) ?: GAME_FREE_FIRE_ORI
        val fovRadius = prefs.getFloat(KEY_FOV_RADIUS, 100f)
        val fovColor = prefs.getInt(KEY_FOV_COLOR, 0xFF39FF14.toInt())
        val fovEnabled = prefs.getBoolean(KEY_FOV_ENABLED, false)
        val fovOffsetX = prefs.getFloat(KEY_FOV_OFFSET_X, 0f)
        val fovOffsetY = prefs.getFloat(KEY_FOV_OFFSET_Y, 0f)

        _themeFlow.value = ZephyrThemeConfig(
            primaryColor = Color(primary),
            backgroundColor = Color(bg),
            highlightColor = Color(highlight)
        )
        _selectedGameFlow.value = game
        _fovRadiusFlow.value = fovRadius
        _fovColorFlow.value = Color(fovColor)
        _fovEnabledFlow.value = fovEnabled
        _fovOffsetXFlow.value = fovOffsetX
        _fovOffsetYFlow.value = fovOffsetY
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveTheme(context: Context, primary: Color, bg: Color, highlight: Color) {
        getPrefs(context).edit().apply {
            putInt(KEY_PRIMARY_COLOR, primary.toArgb())
            putInt(KEY_BG_COLOR, bg.toArgb())
            putInt(KEY_HIGHLIGHT_COLOR, highlight.toArgb())
            apply()
        }
        _themeFlow.value = ZephyrThemeConfig(
            primaryColor = primary,
            backgroundColor = bg,
            highlightColor = highlight
        )
    }

    fun saveSelectedGame(context: Context, gamePackage: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_GAME, gamePackage).apply()
        _selectedGameFlow.value = gamePackage
    }

    fun saveFovConfig(context: Context, enabled: Boolean, radius: Float, color: Color) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_FOV_ENABLED, enabled)
            putFloat(KEY_FOV_RADIUS, radius)
            putInt(KEY_FOV_COLOR, color.toArgb())
            apply()
        }
        _fovEnabledFlow.value = enabled
        _fovRadiusFlow.value = radius
        _fovColorFlow.value = color
    }

    fun setFovEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FOV_ENABLED, enabled).apply()
        _fovEnabledFlow.value = enabled
    }

    fun adjustFovOffset(context: Context, deltaX: Float, deltaY: Float) {
        val newX = _fovOffsetXFlow.value + deltaX
        val newY = _fovOffsetYFlow.value + deltaY
        getPrefs(context).edit().apply {
            putFloat(KEY_FOV_OFFSET_X, newX)
            putFloat(KEY_FOV_OFFSET_Y, newY)
            apply()
        }
        _fovOffsetXFlow.value = newX
        _fovOffsetYFlow.value = newY
    }

    fun resetFovOffset(context: Context) {
        getPrefs(context).edit().apply {
            putFloat(KEY_FOV_OFFSET_X, 0f)
            putFloat(KEY_FOV_OFFSET_Y, 0f)
            apply()
        }
        _fovOffsetXFlow.value = 0f
        _fovOffsetYFlow.value = 0f
    }
}
