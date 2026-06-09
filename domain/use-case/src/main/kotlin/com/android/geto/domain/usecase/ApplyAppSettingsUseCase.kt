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
package com.android.geto.domain.usecase

import com.android.geto.domain.common.dispatcher.Dispatcher
import com.android.geto.domain.common.dispatcher.GetoDispatchers.Default
import com.android.geto.domain.framework.SecureSettingsWrapper
import com.android.geto.domain.model.AppSettingsApplyResult
import com.android.geto.domain.model.AppSettingsResult
import com.android.geto.domain.model.AppSettingsResult.DisabledAppSettings
import com.android.geto.domain.model.AppSettingsResult.EmptyAppSettings
import com.android.geto.domain.model.AppSettingsResult.Failure
import com.android.geto.domain.model.AppSettingsResult.InvalidValues
import com.android.geto.domain.model.AppSettingsResult.NoPermission
import com.android.geto.domain.model.AppSettingsResult.Success
import com.android.geto.domain.repository.AppSettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ApplyAppSettingsUseCase @Inject constructor(
    @param:Dispatcher(Default) private val defaultDispatcher: CoroutineDispatcher,
    private val appSettingsRepository: AppSettingsRepository,
    private val secureSettingsWrapper: SecureSettingsWrapper,
) {
    suspend operator fun invoke(componentName: String): AppSettingsApplyResult {
        return withContext(defaultDispatcher) {
            val appSettings =
                appSettingsRepository.getAppSettingsByComponentName(componentName = componentName)

            if (appSettings.isEmpty()) {
                return@withContext AppSettingsApplyResult(EmptyAppSettings, emptyMap())
            }

            val enabledSettings = appSettings.filter { it.enabled }

            if (enabledSettings.isEmpty()) {
                return@withContext AppSettingsApplyResult(DisabledAppSettings, emptyMap())
            }

            try {
                // Apply every enabled rule individually — map{} does NOT short-circuit.
                // One failing key must NOT block the others from being applied.
                val ruleResults: Map<String, Boolean> = enabledSettings.associate { appSetting ->
                    appSetting.key to secureSettingsWrapper.canWriteSecureSettings(
                        settingType = appSetting.settingType,
                        key = appSetting.key,
                        value = appSetting.valueOnLaunch,
                    )
                }
                val overall: AppSettingsResult = if (ruleResults.values.all { it }) Success else Failure
                AppSettingsApplyResult(overall, ruleResults)
            } catch (_: SecurityException) {
                AppSettingsApplyResult(NoPermission, emptyMap())
            } catch (_: IllegalArgumentException) {
                AppSettingsApplyResult(InvalidValues, emptyMap())
            }
        }
    }
}
