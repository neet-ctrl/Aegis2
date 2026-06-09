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
package com.android.geto.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.android.geto.R
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.feature.apps.navigation.AppsRouteData
import com.android.geto.feature.home.navigation.ActivityRouteData
import com.android.geto.feature.home.navigation.AutomationsRouteData
import com.android.geto.feature.home.navigation.DashboardRouteData
import com.android.geto.feature.home.navigation.HomeDestination
import com.android.geto.feature.settings.navigation.SettingsRouteData
import kotlin.reflect.KClass

enum class TopLevelDestination(
    override val label: Int,
    override val icon: ImageVector,
    override val selectedIcon: ImageVector,
    override val contentDescription: Int,
    override val route: KClass<*>,
) : HomeDestination {
    DASHBOARD(
        label = R.string.dashboard,
        icon = GetoIcons.DashboardOutlined,
        selectedIcon = GetoIcons.Dashboard,
        contentDescription = R.string.dashboard,
        route = DashboardRouteData::class,
    ),
    APPS(
        label = R.string.apps,
        icon = GetoIcons.AppsOutlined,
        selectedIcon = GetoIcons.Apps,
        contentDescription = R.string.apps,
        route = AppsRouteData::class,
    ),
    AUTOMATIONS(
        label = R.string.automations,
        icon = GetoIcons.AutomationsOutlined,
        selectedIcon = GetoIcons.Automations,
        contentDescription = R.string.automations,
        route = AutomationsRouteData::class,
    ),
    ACTIVITY(
        label = R.string.activity,
        icon = GetoIcons.ActivityOutlined,
        selectedIcon = GetoIcons.Activity,
        contentDescription = R.string.activity,
        route = ActivityRouteData::class,
    ),
    SETTINGS(
        label = R.string.settings,
        icon = GetoIcons.SettingsOutlined,
        selectedIcon = GetoIcons.Settings,
        contentDescription = R.string.settings,
        route = SettingsRouteData::class,
    ),
}
