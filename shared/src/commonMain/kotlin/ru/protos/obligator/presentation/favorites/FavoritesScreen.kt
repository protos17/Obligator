package ru.protos.obligator.presentation.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_compare
import obligator.shared.generated.resources.action_delete
import obligator.shared.generated.resources.favorites_empty
import obligator.shared.generated.resources.label_current_yield_short
import obligator.shared.generated.resources.label_yield_short
import obligator.shared.generated.resources.tab_favorites
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.calc.calculateCurrentYield
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.presentation.util.formatPercent

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    listState: LazyListState = rememberLazyListState(),
    onBondClick: (String) -> Unit,
    onCompareClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "${stringResource(Res.string.tab_favorites)} (${state.favorites.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (state.selectedCompareSecids.size >= 2) {
                Button(onClick = onCompareClick) {
                    Text("${stringResource(Res.string.action_compare)} (${state.selectedCompareSecids.size})")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(Res.string.favorites_empty), color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.favorites, key = { it.secid }) { item ->
                    val bond = state.bondsMap[item.secid]
                    val isCompared = state.selectedCompareSecids.contains(item.secid)
                    val canSelectMore = state.selectedCompareSecids.size < 3 || isCompared

                    FavoriteItemCard(
                        secid = item.secid,
                        shortName = item.shortName,
                        bond = bond,
                        isCompared = isCompared,
                        canSelectMore = canSelectMore,
                        onClick = { onBondClick(item.secid) },
                        onRemove = { viewModel.removeFavorite(item.secid) },
                        onCompareToggle = { viewModel.toggleCompare(item.secid) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteItemCard(
    secid: String,
    shortName: String,
    bond: Bond?,
    isCompared: Boolean,
    canSelectMore: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onCompareToggle: () -> Unit
) {
    val curYield = bond?.let { calculateCurrentYield(it.couponPercent, it.price) }

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
                Text(text = bond?.shortName ?: shortName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "${stringResource(Res.string.label_yield_short)}${formatPercent(bond?.yieldToMaturity)}", fontSize = 13.sp)
                    Text(text = "${stringResource(Res.string.label_current_yield_short)}${formatPercent(curYield)}", fontSize = 13.sp)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete), tint = Color.Gray)
            }
        }
    }
}
