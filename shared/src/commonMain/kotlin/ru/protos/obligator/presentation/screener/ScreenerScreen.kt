package ru.protos.obligator.presentation.screener

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_compare
import obligator.shared.generated.resources.action_filters
import obligator.shared.generated.resources.action_refresh
import obligator.shared.generated.resources.action_sort
import obligator.shared.generated.resources.disclaimer_text
import obligator.shared.generated.resources.error_bonds_not_found
import obligator.shared.generated.resources.error_offline_no_data
import obligator.shared.generated.resources.error_offline_saved_data
import obligator.shared.generated.resources.found_count_prefix
import obligator.shared.generated.resources.label_current_yield_short
import obligator.shared.generated.resources.label_price_short
import obligator.shared.generated.resources.label_term_short
import obligator.shared.generated.resources.label_yield_short
import obligator.shared.generated.resources.loaded_at_prefix
import obligator.shared.generated.resources.price_date_prefix
import obligator.shared.generated.resources.screener_title
import obligator.shared.generated.resources.search_placeholder
import obligator.shared.generated.resources.sort_by_cur_yield
import obligator.shared.generated.resources.sort_by_duration
import obligator.shared.generated.resources.sort_by_name
import obligator.shared.generated.resources.sort_by_price
import obligator.shared.generated.resources.sort_by_term
import obligator.shared.generated.resources.sort_by_ytm
import obligator.shared.generated.resources.sort_short_cur_yield
import obligator.shared.generated.resources.sort_short_duration
import obligator.shared.generated.resources.sort_short_name
import obligator.shared.generated.resources.sort_short_price
import obligator.shared.generated.resources.sort_short_term
import obligator.shared.generated.resources.sort_short_ytm
import obligator.shared.generated.resources.tab_favorites
import obligator.shared.generated.resources.unit_years
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.calc.calculateCurrentYield
import ru.protos.obligator.analytics.calc.calculateYearsToMaturity
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.SortField
import ru.protos.obligator.analytics.model.SortOrder
import ru.protos.obligator.presentation.components.BondLogoIcon
import ru.protos.obligator.presentation.util.formatDate
import ru.protos.obligator.presentation.util.formatDateTime
import ru.protos.obligator.presentation.util.formatDouble
import ru.protos.obligator.presentation.util.formatPercent

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ScreenerScreen(
    viewModel: ScreenerViewModel,
    listState: LazyListState = rememberLazyListState(),
    onBondClick: (String) -> Unit,
    onCompareClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortShortLabel = when (state.sortField) {
        SortField.NAME -> stringResource(Res.string.sort_short_name)
        SortField.YIELD_TO_MATURITY -> stringResource(Res.string.sort_short_ytm)
        SortField.CURRENT_YIELD -> stringResource(Res.string.sort_short_cur_yield)
        SortField.TERM -> stringResource(Res.string.sort_short_term)
        SortField.DURATION -> stringResource(Res.string.sort_short_duration)
        SortField.PRICE -> stringResource(Res.string.sort_short_price)
    }
    val sortArrow = if (state.sortOrder == SortOrder.ASC) "▲" else "▼"
    val activeFiltersCount = countActiveFilters(state.filter)
    val filterButtonLabel = if (activeFiltersCount > 0) {
        "${stringResource(Res.string.action_filters)} ($activeFiltersCount)"
    } else {
        stringResource(Res.string.action_filters)
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BondLogoIcon(
                    modifier = Modifier.padding(end = 8.dp).size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.screener_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.updateInfo != null) {
                        Text(
                            text = "${stringResource(Res.string.price_date_prefix)}${formatDate(state.updateInfo?.priceDate)}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${stringResource(Res.string.loaded_at_prefix)}${formatDateTime(state.updateInfo?.loadedAt)}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.action_refresh))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                backgroundColor = Color(0xFFFFEBEE)
            ) {
                val msg = if (state.displayedBonds.isNotEmpty()) {
                    stringResource(Res.string.error_offline_saved_data)
                } else {
                    stringResource(Res.string.error_offline_no_data)
                }
                Text(
                    text = msg,
                    color = Color(0xFFC62828),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text(stringResource(Res.string.search_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(Res.string.action_sort),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$sortShortLabel $sortArrow",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.NAME && state.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                        viewModel.onSortChanged(SortField.NAME, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_name) + if (state.sortField == SortField.NAME) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.YIELD_TO_MATURITY && state.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.onSortChanged(SortField.YIELD_TO_MATURITY, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_ytm) + if (state.sortField == SortField.YIELD_TO_MATURITY) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.CURRENT_YIELD && state.sortOrder == SortOrder.DESC) SortOrder.ASC else SortOrder.DESC
                        viewModel.onSortChanged(SortField.CURRENT_YIELD, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_cur_yield) + if (state.sortField == SortField.CURRENT_YIELD) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.TERM && state.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                        viewModel.onSortChanged(SortField.TERM, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_term) + if (state.sortField == SortField.TERM) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.DURATION && state.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                        viewModel.onSortChanged(SortField.DURATION, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_duration) + if (state.sortField == SortField.DURATION) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                    DropdownMenuItem(onClick = {
                        val newOrder = if (state.sortField == SortField.PRICE && state.sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC
                        viewModel.onSortChanged(SortField.PRICE, newOrder)
                        showSortMenu = false
                    }) {
                        Text(stringResource(Res.string.sort_by_price) + if (state.sortField == SortField.PRICE) (if (state.sortOrder == SortOrder.ASC) " ▲" else " ▼") else "")
                    }
                }
            }

            OutlinedButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = stringResource(Res.string.action_filters),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = filterButtonLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${stringResource(Res.string.found_count_prefix)}${state.displayedBonds.size}", fontSize = 14.sp)
            if (state.selectedCompareSecids.size >= 2) {
                Button(onClick = onCompareClick) {
                    Text("${stringResource(Res.string.action_compare)} (${state.selectedCompareSecids.size})")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val pullRefreshState = rememberPullRefreshState(
            refreshing = state.isLoading,
            onRefresh = { viewModel.refresh() }
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().pullRefresh(pullRefreshState)
        ) {
            if (state.isLoading && state.displayedBonds.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.displayedBonds.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val emptyMsg = if (state.errorMessage != null) stringResource(Res.string.error_offline_no_data) else stringResource(Res.string.error_bonds_not_found)
                    Text(text = emptyMsg, color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.displayedBonds, key = { it.secid }) { bond ->
                        BondItemCard(
                            bond = bond,
                            isFavorite = state.favoriteSecids.contains(bond.secid),
                            isCompared = state.selectedCompareSecids.contains(bond.secid),
                            canSelectMore = state.selectedCompareSecids.size < 3 || state.selectedCompareSecids.contains(bond.secid),
                            priceDate = state.updateInfo?.priceDate,
                            onClick = { onBondClick(bond.secid) },
                            onFavoriteClick = { viewModel.toggleFavorite(bond) },
                            onCompareToggle = { viewModel.toggleCompare(bond.secid) }
                        )
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.disclaimer_text),
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showFilterDialog) {
        FilterBottomSheet(
            currentFilter = state.filter,
            onApply = {
                viewModel.onFilterChanged(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

private fun countActiveFilters(filter: BondFilter): Int {
    var count = 0
    if (!filter.issuerGroups.isNullOrEmpty()) count++
    if (filter.minYieldToMat != null || filter.maxYieldToMat != null) count++
    if (filter.minYearsToMat != null || filter.maxYearsToMat != null) count++
    if (filter.couponType != null) count++
    if (!filter.listLevels.isNullOrEmpty()) count++
    return count
}

@Composable
fun BondItemCard(
    bond: Bond,
    isFavorite: Boolean,
    isCompared: Boolean,
    canSelectMore: Boolean,
    priceDate: String?,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCompareToggle: () -> Unit
) {
    val curYield = calculateCurrentYield(bond.couponPercent, bond.price)
    val years = if (priceDate != null) calculateYearsToMaturity(bond.matDate, priceDate) else null

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isCompared,
                onCheckedChange = { onCompareToggle() },
                enabled = canSelectMore
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bond.shortName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "${stringResource(Res.string.label_yield_short)}${formatPercent(bond.yieldToMaturity)}", fontSize = 13.sp)
                    Text(text = "${stringResource(Res.string.label_current_yield_short)}${formatPercent(curYield)}", fontSize = 13.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "${stringResource(Res.string.label_price_short)}${formatPercent(bond.price)}", fontSize = 13.sp)
                    Text(text = "${stringResource(Res.string.label_term_short)}${formatDouble(years)} ${stringResource(Res.string.unit_years)}", fontSize = 13.sp)
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(Res.string.tab_favorites),
                    tint = if (isFavorite) Color.Red else Color.Gray
                )
            }
        }
    }
}
