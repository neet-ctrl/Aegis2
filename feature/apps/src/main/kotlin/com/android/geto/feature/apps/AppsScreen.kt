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
package com.android.geto.feature.apps

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.android.geto.designsystem.icon.GetoIcons
import com.android.geto.domain.model.LauncherAppsActivityInfo
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

@Composable
internal fun AppsRoute(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
    onClickApp: (
        componentName: String,
        activityLabel: String,
    ) -> Unit,
) {
    val appListUiState by viewModel.appsUiState.collectAsStateWithLifecycle()

    AppsScreen(
        modifier = modifier,
        appsUiState = appListUiState,
        onClickApp = onClickApp,
        onSearch = viewModel::search,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@VisibleForTesting
@Composable
internal fun AppsScreen(
    modifier: Modifier = Modifier,
    appsUiState: AppsUiState,
    onClickApp: (
        componentName: String,
        activityLabel: String,
    ) -> Unit,
    onSearch: (String) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (appsUiState) {
            AppsUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading apps…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is AppsUiState.Success -> {
                Success(
                    modifier = modifier,
                    appsUiState = appsUiState,
                    onClickApp = onClickApp,
                    onSearch = onSearch,
                )
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
private fun Success(
    modifier: Modifier = Modifier,
    appsUiState: AppsUiState.Success,
    onClickApp: (
        componentName: String,
        activityLabel: String,
    ) -> Unit,
    onSearch: (String) -> Unit,
) {
    var searchBarQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(key1 = searchBarQuery) {
        snapshotFlow { searchBarQuery }.debounce(500).distinctUntilChanged().onEach {
            onSearch(it)
        }.collect()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SearchBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchBarQuery,
                    onQueryChange = { searchBarQuery = it },
                    onSearch = onSearch,
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = GetoIcons.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            shape = RoundedCornerShape(16.dp),
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {}

        if (appsUiState.launcherAppsActivityInfos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No apps found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(
                    items = appsUiState.launcherAppsActivityInfos,
                    key = { it.componentName },
                ) { launcherAppsActivityInfo ->
                    AppItem(
                        launcherAppsActivityInfo = launcherAppsActivityInfo,
                        onClickApp = onClickApp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    modifier: Modifier = Modifier,
    launcherAppsActivityInfo: LauncherAppsActivityInfo,
    onClickApp: (
        componentName: String,
        activityLabel: String,
    ) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                onClickApp(
                    launcherAppsActivityInfo.componentName,
                    launcherAppsActivityInfo.activityLabel,
                )
            }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                modifier = Modifier.size(40.dp),
                model = launcherAppsActivityInfo.activityIcon,
                contentDescription = null,
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = launcherAppsActivityInfo.activityLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = launcherAppsActivityInfo.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.padding(end = 4.dp),
        ) {
            Icon(
                imageVector = GetoIcons.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(6.dp)
                    .size(14.dp),
            )
        }
    }
}
