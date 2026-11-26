package com.reminimalism.particleslivewallpaper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reminimalism.particleslivewallpaper.ui.theme.ParticlesLiveWallpaperTheme
import androidx.core.net.toUri

class SettingsActivity : ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParticlesLiveWallpaperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsActivityView(
                        modifier = Modifier.padding(innerPadding),
                        context = this
                    )
                }
            }
        }
    }
}

fun getAppPreferences(context: Context): SharedPreferences
{
    //return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // This gets what was previously returned by:
    // PreferenceManager.getDefaultSharedPreferences(context)
    return context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}

const val PERSONALIZATION_COLORS = "colors"

const val CUSTOM_COLORS_ENABLED = "custom_colors_enabled"

const val CUSTOM_DAY_PARTICLES = "custom_day_particles"
const val CUSTOM_DAY_BG = "custom_day_bg"
const val CUSTOM_DAY_PARTICLES_SHUFFLE = "custom_day_particles_shuffle"
const val CUSTOM_DAY_BRIGHTNESS = "custom_day_brightness"

const val CUSTOM_DAWN_PARTICLES = "custom_dawn_particles"
const val CUSTOM_DAWN_BG = "custom_dawn_bg"
const val CUSTOM_DAWN_PARTICLES_SHUFFLE = "custom_dawn_particles_shuffle"
const val CUSTOM_DAWN_BRIGHTNESS = "custom_dawn_brightness"

const val CUSTOM_NIGHT_PARTICLES = "custom_night_particles"
const val CUSTOM_NIGHT_BG = "custom_night_bg"
const val CUSTOM_NIGHT_PARTICLES_SHUFFLE = "custom_night_particles_shuffle"
const val CUSTOM_NIGHT_BRIGHTNESS = "custom_night_brightness"

const val PERSONALIZATION_SUPPORT_DARK_TEXT = "dark_text_support_enabled"

const val MOTION_ACCELERATION = "acceleration"
const val MOTION_ROTATION_SPEED = "rotation_speed"

const val PERFORMANCE_PARTICLE_DENSITY = "particles_density"
const val PERFORMANCE_SPACE_SIZE = "space_size"

const val CUSTOM_COLOR_OPTION_WHITE = "white"
const val CUSTOM_COLOR_OPTION_RED = "red"
const val CUSTOM_COLOR_OPTION_ORANGE = "orange"
const val CUSTOM_COLOR_OPTION_YELLOW = "yellow"
const val CUSTOM_COLOR_OPTION_LIME = "lime"
const val CUSTOM_COLOR_OPTION_GREEN = "green"
const val CUSTOM_COLOR_OPTION_MINT = "mint"
const val CUSTOM_COLOR_OPTION_CYAN = "cyan"
const val CUSTOM_COLOR_OPTION_SKY_BLUE = "sky_blue"
const val CUSTOM_COLOR_OPTION_BLUE = "blue"
const val CUSTOM_COLOR_OPTION_LAVENDER = "lavender"
const val CUSTOM_COLOR_OPTION_PINK = "pink"
const val CUSTOM_COLOR_OPTION_SALMON_PINK = "salmon_pink"
const val CUSTOM_COLOR_OPTION_WATERMELON = "watermelon"

val CUSTOM_COLOR_OPTIONS = mapOf(
    Pair(CUSTOM_COLOR_OPTION_WHITE, "White"),
    Pair(CUSTOM_COLOR_OPTION_RED, "Red"),
    Pair(CUSTOM_COLOR_OPTION_ORANGE, "Orange"),
    Pair(CUSTOM_COLOR_OPTION_YELLOW, "Yellow"),
    Pair(CUSTOM_COLOR_OPTION_LIME, "Lime"),
    Pair(CUSTOM_COLOR_OPTION_GREEN, "Green"),
    Pair(CUSTOM_COLOR_OPTION_MINT, "Mint"),
    Pair(CUSTOM_COLOR_OPTION_CYAN, "Cyan"),
    Pair(CUSTOM_COLOR_OPTION_SKY_BLUE, "Sky Blue"),
    Pair(CUSTOM_COLOR_OPTION_BLUE, "Blue"),
    Pair(CUSTOM_COLOR_OPTION_LAVENDER, "Lavender"),
    Pair(CUSTOM_COLOR_OPTION_PINK, "Pink"),
    Pair(CUSTOM_COLOR_OPTION_SALMON_PINK, "Salmon Pink"),
    Pair(CUSTOM_COLOR_OPTION_WATERMELON, "Watermelon")
)

const val PARTICLE_COLOR_OPTION_DAY_NIGHT_SKY_BLUE = "day_night_sky_blue"
const val PARTICLE_COLOR_OPTION_DAY_NIGHT_GREEN = "day_night_green"
const val PARTICLE_COLOR_OPTION_DAY_NIGHT_PINK = "day_night_pink"
const val PARTICLE_COLOR_OPTION_NIGHT = "night"
const val PARTICLE_COLOR_OPTION_GRAYSCALE = "grayscale"

val PARTICLE_COLOR_OPTIONS = mapOf(
    Pair(PARTICLE_COLOR_OPTION_DAY_NIGHT_SKY_BLUE, "Day Night: Sky Blue"),
    Pair(PARTICLE_COLOR_OPTION_DAY_NIGHT_GREEN, "Day Night: Green"),
    Pair(PARTICLE_COLOR_OPTION_DAY_NIGHT_PINK, "Day Night: Pink"),
    Pair(PARTICLE_COLOR_OPTION_NIGHT, "Night"),
    Pair(PARTICLE_COLOR_OPTION_GRAYSCALE, "Grayscale")
)

@Composable
fun SettingsActivityView(modifier: Modifier = Modifier, context: Context? = null)
{
    val preferences = if (context == null) null else getAppPreferences(context)

    Box(modifier = modifier.fillMaxSize())
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
        )
        {
            // Activity Header
            Text(
                text = "Settings",
                fontSize = 24.sp,
                modifier = Modifier
                    .padding(16.dp)
            )

            // Settings Scroll View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(0.dp, 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            )
            {
                val versionName = context?.let { context ->
                    context.packageManager.getPackageInfo(
                        context.packageName, 0
                    ).versionName
                } ?: "UNKNOWN"

                SettingsHeaderView("Personalization")

                val isCustomEnabled = remember {
                    mutableStateOf(
                        preferences?.getBoolean(
                            CUSTOM_COLORS_ENABLED,
                            false
                        ) ?: false
                    )
                }

                SettingsToggleView(
                    preferences, CUSTOM_COLORS_ENABLED,
                    "Custom Colors",
                    defaultValue = false,
                    onToggle = {
                        isCustomEnabled.value = it
                    }
                )

                if (isCustomEnabled.value)
                {
                    // Day

                    SettingsOptionView(
                        preferences, CUSTOM_DAY_PARTICLES,
                        "Day Particles Base Color",
                        CUSTOM_COLOR_OPTION_WHITE,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsOptionView(
                        preferences, CUSTOM_DAY_BG,
                        "Day Background Color",
                        CUSTOM_COLOR_OPTION_SKY_BLUE,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_DAY_PARTICLES_SHUFFLE,
                        "Day Particles Color Variety",
                        "Particle color deviation from the base color.",
                        75
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_DAY_BRIGHTNESS,
                        "Day Brightness",
                        "",
                        90
                    )

                    // Dawn

                    SettingsOptionView(
                        preferences, CUSTOM_DAWN_PARTICLES,
                        "Dawn Particles Base Color",
                        CUSTOM_COLOR_OPTION_WHITE,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsOptionView(
                        preferences, CUSTOM_DAWN_BG,
                        "Dawn Background Color",
                        CUSTOM_COLOR_OPTION_ORANGE,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_DAWN_PARTICLES_SHUFFLE,
                        "Dawn Particles Color Variety",
                        "Particle color deviation from the base color.",
                        50
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_DAWN_BRIGHTNESS,
                        "Dawn Brightness", "",
                        75
                    )

                    // Night

                    SettingsOptionView(
                        preferences, CUSTOM_NIGHT_PARTICLES,
                        "Night Particles Base Color",
                        CUSTOM_COLOR_OPTION_SALMON_PINK,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsOptionView(
                        preferences, CUSTOM_NIGHT_BG,
                        "Night Background Color",
                        CUSTOM_COLOR_OPTION_BLUE,
                        CUSTOM_COLOR_OPTIONS
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_NIGHT_PARTICLES_SHUFFLE,
                        "Night Particles Color Variety",
                        "Particle color deviation from the base color.",
                        50
                    )

                    SettingsIntSliderView(
                        preferences, CUSTOM_NIGHT_BRIGHTNESS,
                        "Night Brightness", "",
                        20
                    )
                }
                else
                {
                    SettingsOptionView(
                        preferences, PERSONALIZATION_COLORS,
                        "Colors",
                        PARTICLE_COLOR_OPTION_DAY_NIGHT_SKY_BLUE,
                        PARTICLE_COLOR_OPTIONS
                    )
                }

                SettingsToggleView(
                    preferences, PERSONALIZATION_SUPPORT_DARK_TEXT,
                    "Launcher Dark Text",
                    "Reports whether dark text is supported to launcher"
                            + " when the background is bright (Android 12+).",
                    "Doesn't report whether dark text is supported to launcher",
                    true
                )

                SettingsHeaderView("Motion")

                SettingsIntSliderView(
                    preferences, MOTION_ACCELERATION,
                    "Acceleration", "",
                    50
                )

                SettingsIntSliderView(
                    preferences, MOTION_ROTATION_SPEED,
                    "Rotation Speed", "",
                    20
                )

                SettingsHeaderView("Performance")

                SettingsIntSliderView(
                    preferences, PERFORMANCE_PARTICLE_DENSITY,
                    "Particle Density",
                    "Affects battery usage and frame rate."
                            + " Needs restart to fully apply.",
                    25
                )

                SettingsIntSliderView(
                    preferences, PERFORMANCE_SPACE_SIZE,
                    "Space Size",
                    "Adds depth to the space shown."
                            + " Affects battery usage and frame rate."
                            + " Needs restart to fully apply.",
                    0
                )

                SettingsHeaderView("More")

                SettingsItemView(
                    "App Version",
                    versionName
                )

                SettingsItemView(
                    "Privacy Policy",
                    onClick = {
                        context?.startActivity(Intent(
                            Intent.ACTION_VIEW,
                            "https://www.reminimalism.com/particleslivewallpaper/privacypolicy".toUri()
                        ))
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsActivityPreview()
{
    ParticlesLiveWallpaperTheme {
        SettingsActivityView()
    }
}