/*
 *
 *   Copyright 2023 Einstein Blanco
 *
 *   Licensed under the GNU General Public License v3.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.gnu.org/licenses/gpl-3.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.android.geto.designsystem.theme

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.android.geto.domain.model.Theme

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF001849),
    secondary = Color(0xFF006874),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF97F0FF),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = Color(0xFF6750A4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADDFF),
    onTertiaryContainer = Color(0xFF21005D),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFAFF),
    onBackground = Color(0xFF1A1C23),
    surface = Color(0xFFFAFAFF),
    onSurface = Color(0xFF1A1C23),
    surfaceVariant = Color(0xFFE2E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313038),
    inverseOnSurface = Color(0xFFF3EFF7),
    inversePrimary = Color(0xFFADC6FF),
    surfaceDim = Color(0xFFDBD9E0),
    surfaceBright = Color(0xFFFAFAFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5FF),
    surfaceContainer = Color(0xFFEFEFF7),
    surfaceContainerHigh = Color(0xFFE9E9F1),
    surfaceContainerHighest = Color(0xFFE3E3EB),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF002E6E),
    primaryContainer = Color(0xFF00449F),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF003740),
    secondaryContainer = Color(0xFF00505C),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = Color(0xFFCFBCFF),
    onTertiary = Color(0xFF370065),
    tertiaryContainer = Color(0xFF4F378B),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E8),
    surface = Color(0xFF121316),
    onSurface = Color(0xFFE3E2E8),
    surfaceVariant = Color(0xFF292B36),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E2E8),
    inverseOnSurface = Color(0xFF313038),
    inversePrimary = Color(0xFF1565C0),
    surfaceDim = Color(0xFF121316),
    surfaceBright = Color(0xFF38393F),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF1A1C22),
    surfaceContainer = Color(0xFF1E2028),
    surfaceContainerHigh = Color(0xFF282A33),
    surfaceContainerHighest = Color(0xFF33353F),
)

private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF4DA3FF),
    onPrimary = Color(0xFF002E6E),
    primaryContainer = Color(0xFF003585),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF4DD0E1),
    onSecondary = Color(0xFF003740),
    secondaryContainer = Color(0xFF004150),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = Color(0xFFCFBCFF),
    onTertiary = Color(0xFF370065),
    tertiaryContainer = Color(0xFF3A0074),
    onTertiaryContainer = Color(0xFFEADDFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF6E0009),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE3E2E8),
    surface = Color(0xFF050507),
    onSurface = Color(0xFFE3E2E8),
    surfaceVariant = Color(0xFF18191F),
    onSurfaceVariant = Color(0xFFC6C5D0),
    outline = Color(0xFF75747E),
    outlineVariant = Color(0xFF38383F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E2E8),
    inverseOnSurface = Color(0xFF313038),
    inversePrimary = Color(0xFF1565C0),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF232427),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0B10),
    surfaceContainer = Color(0xFF0F1016),
    surfaceContainerHigh = Color(0xFF14151C),
    surfaceContainerHighest = Color(0xFF1A1B22),
)

@Composable
fun GetoTheme(
    theme: Theme,
    dynamicTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        theme == Theme.AMOLED -> AmoledColorScheme
        supportsDynamicTheming() && dynamicTheme -> getDynamicColorScheme(theme)
        else -> getStaticColorScheme(theme)
    }

    CompositionLocalProvider {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}

@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
fun supportsDynamicTheming() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
private fun getStaticColorScheme(theme: Theme): ColorScheme {
    return when (theme) {
        Theme.FOLLOW_SYSTEM -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
        Theme.LIGHT -> LightColorScheme
        Theme.DARK -> DarkColorScheme
        Theme.AMOLED -> AmoledColorScheme
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun getDynamicColorScheme(theme: Theme): ColorScheme {
    val context = LocalContext.current

    return when (theme) {
        Theme.FOLLOW_SYSTEM -> {
            if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        Theme.LIGHT -> dynamicLightColorScheme(context)
        Theme.DARK -> dynamicDarkColorScheme(context)
        Theme.AMOLED -> AmoledColorScheme
    }
}
