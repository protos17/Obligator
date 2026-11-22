package ru.protos.obligator.presentation.portfolio

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_add_to_portfolio
import obligator.shared.generated.resources.action_cancel
import obligator.shared.generated.resources.action_delete
import obligator.shared.generated.resources.action_save
import obligator.shared.generated.resources.calendar_coupons_prefix
import obligator.shared.generated.resources.calendar_month_prefix
import obligator.shared.generated.resources.calendar_repayments_prefix
import obligator.shared.generated.resources.coupon_calendar_title
import obligator.shared.generated.resources.dialog_add_position_title
import obligator.shared.generated.resources.dialog_change_selection
import obligator.shared.generated.resources.dialog_edit_quantity_label
import obligator.shared.generated.resources.dialog_edit_quantity_title
import obligator.shared.generated.resources.dialog_portfolio_quantity_label
import obligator.shared.generated.resources.dialog_search_placeholder
import obligator.shared.generated.resources.dialog_selected_bond
import obligator.shared.generated.resources.portfolio_calendar_empty
import obligator.shared.generated.resources.portfolio_positions_empty
import obligator.shared.generated.resources.portfolio_positions_title
import obligator.shared.generated.resources.portfolio_total_value
import obligator.shared.generated.resources.tab_portfolio
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.calc.calculatePositionValue
import ru.protos.obligator.analytics.model.Bond
import ru.protos.obligator.analytics.model.CalendarMonthGroup
import ru.protos.obligator.analytics.model.PortfolioPosition
import ru.protos.obligator.presentation.util.formatDate
import ru.protos.obligator.presentation.util.formatMonthKey
import ru.protos.obligator.presentation.util.formatRubles

@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    onBondClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editPosition by remember { mutableStateOf<PortfolioPosition?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(Res.string.tab_portfolio), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.action_add_to_portfolio))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.action_add_to_portfolio))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = stringResource(Res.string.portfolio_total_value), color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatRubles(state.totalValue),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(text = "${stringResource(Res.string.portfolio_positions_title)} (${state.positions.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                if (state.positions.isEmpty()) {
                    Text(stringResource(Res.string.portfolio_positions_empty), color = Color.Gray, fontSize = 14.sp)
                } else {
                    state.positions.forEach { pos ->
                        val bond = state.bondsMap[pos.secid]
                        val posVal = bond?.let { calculatePositionValue(it.price, it.faceValue, it.accruedInt, pos.quantity) }

                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onBondClick(pos.secid) },
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = bond?.shortName ?: pos.shortName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "${pos.quantity} шт. • ${formatRubles(posVal)}", fontSize = 14.sp)
                                }
                                Row {
                                    IconButton(onClick = { editPosition = pos }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.dialog_edit_quantity_title), tint = Color.Gray)
                                    }
                                    IconButton(onClick = { viewModel.removePosition(pos.secid) }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_delete), tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(Res.string.coupon_calendar_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)

                if (state.calendarGroups.isEmpty()) {
                    Text(stringResource(Res.string.portfolio_calendar_empty), color = Color.Gray, fontSize = 14.sp)
                } else {
                    state.calendarGroups.forEach { monthGroup ->
                        CalendarMonthCard(group = monthGroup)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPositionDialog(
            searchQuery = state.searchQuery,
            searchResults = state.searchResults,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            onSelectBond = { bond, qty ->
                viewModel.addOrUpdatePosition(bond.secid, bond.shortName, qty)
                viewModel.onSearchQueryChanged("")
                showAddDialog = false
            },
            onDismiss = {
                viewModel.onSearchQueryChanged("")
                showAddDialog = false
            }
        )
    }

    if (editPosition != null) {
        val pos = editPosition!!
        EditQuantityDialog(
            currentQuantity = pos.quantity,
            onSave = { qty ->
                viewModel.addOrUpdatePosition(pos.secid, pos.shortName, qty)
                editPosition = null
            },
            onDismiss = { editPosition = null }
        )
    }
}

@Composable
fun CalendarMonthCard(group: CalendarMonthGroup) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "${stringResource(Res.string.calendar_month_prefix)}${formatMonthKey(group.monthKey)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            if (group.couponTotal > 0) {
                Text("${stringResource(Res.string.calendar_coupons_prefix)}${formatRubles(group.couponTotal)}", fontSize = 13.sp, color = Color(0xFF2E7D32))
            }
            if (group.repaymentTotal > 0) {
                Text("${stringResource(Res.string.calendar_repayments_prefix)}${formatRubles(group.repaymentTotal)}", fontSize = 13.sp, color = Color(0xFF1565C0))
            }
            Divider()
            group.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatDate(item.date)} • ${item.shortName}", fontSize = 13.sp)
                    Text(
                        text = if (item.totalAmount != null) formatRubles(item.totalAmount) else "нет данных",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun AddPositionDialog(
    searchQuery: String,
    searchResults: List<Bond>,
    onQueryChange: (String) -> Unit,
    onSelectBond: (Bond, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var queryText by remember(searchQuery) { mutableStateOf(searchQuery) }
    var selectedBond by remember { mutableStateOf<Bond?>(null) }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_add_position_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedBond == null) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { text ->
                            queryText = text
                            onQueryChange(text)
                        },
                        label = { Text(stringResource(Res.string.dialog_search_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(searchResults, key = { it.secid }) { bond ->
                            Text(
                                text = "${bond.shortName} (${bond.secid})",
                                modifier = Modifier.fillMaxWidth().clickable { selectedBond = bond }.padding(8.dp)
                            )
                            Divider()
                        }
                    }
                } else {
                    Text("${stringResource(Res.string.dialog_selected_bond)}${selectedBond?.shortName}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(Res.string.dialog_portfolio_quantity_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { selectedBond = null }) {
                        Text(stringResource(Res.string.dialog_change_selection))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bond = selectedBond
                    val qty = quantityText.toIntOrNull() ?: 0
                    if (bond != null && qty > 0) {
                        onSelectBond(bond, qty)
                    }
                },
                enabled = selectedBond != null && (quantityText.toIntOrNull() ?: 0) > 0
            ) {
                Text(stringResource(Res.string.action_add_to_portfolio))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

@Composable
fun EditQuantityDialog(
    currentQuantity: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_edit_quantity_title)) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(Res.string.dialog_edit_quantity_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(textValue.toIntOrNull() ?: 0) }) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
