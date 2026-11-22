package ru.protos.obligator.presentation.screener

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_apply
import obligator.shared.generated.resources.action_cancel
import obligator.shared.generated.resources.action_filters
import obligator.shared.generated.resources.filter_coupon_all
import obligator.shared.generated.resources.filter_coupon_fixed_short
import obligator.shared.generated.resources.filter_coupon_floating_short
import obligator.shared.generated.resources.filter_coupon_type
import obligator.shared.generated.resources.filter_issuer_cbr
import obligator.shared.generated.resources.filter_issuer_corp
import obligator.shared.generated.resources.filter_issuer_muni
import obligator.shared.generated.resources.filter_issuer_ofz
import obligator.shared.generated.resources.filter_listing_level
import obligator.shared.generated.resources.filter_mat_date
import obligator.shared.generated.resources.filter_ytm
import obligator.shared.generated.resources.from
import obligator.shared.generated.resources.label_issuer_group
import obligator.shared.generated.resources.to
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.model.BondFilter
import ru.protos.obligator.analytics.model.CouponType

@Composable
fun FilterBottomSheet(
    currentFilter: BondFilter,
    onApply: (BondFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var issuerGroups by remember { mutableStateOf(currentFilter.issuerGroups ?: emptySet()) }
    var minYield by remember { mutableStateOf(currentFilter.minYieldToMat?.toString() ?: "") }
    var maxYield by remember { mutableStateOf(currentFilter.maxYieldToMat?.toString() ?: "") }
    var minYears by remember { mutableStateOf(currentFilter.minYearsToMat?.toString() ?: "") }
    var maxYears by remember { mutableStateOf(currentFilter.maxYearsToMat?.toString() ?: "") }
    var couponType by remember { mutableStateOf(currentFilter.couponType) }
    var listLevels by remember { mutableStateOf(currentFilter.listLevels ?: emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.action_filters), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(Res.string.label_issuer_group), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterCheckbox(stringResource(Res.string.filter_issuer_ofz), issuerGroups.contains("GOVT")) { checked ->
                        issuerGroups = if (checked) issuerGroups + "GOVT" else issuerGroups - "GOVT"
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterCheckbox(stringResource(Res.string.filter_issuer_muni), issuerGroups.contains("MUNI")) { checked ->
                        issuerGroups = if (checked) issuerGroups + "MUNI" else issuerGroups - "MUNI"
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterCheckbox(stringResource(Res.string.filter_issuer_corp), issuerGroups.contains("CORP")) { checked ->
                        issuerGroups = if (checked) issuerGroups + "CORP" else issuerGroups - "CORP"
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterCheckbox(stringResource(Res.string.filter_issuer_cbr), issuerGroups.contains("CBR")) { checked ->
                        issuerGroups = if (checked) issuerGroups + "CBR" else issuerGroups - "CBR"
                    }
                }

                Text(stringResource(Res.string.filter_ytm), fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minYield,
                        onValueChange = { minYield = it },
                        label = { Text(stringResource(Res.string.from)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxYield,
                        onValueChange = { maxYield = it },
                        label = { Text(stringResource(Res.string.to)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text(stringResource(Res.string.filter_mat_date), fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minYears,
                        onValueChange = { minYears = it },
                        label = { Text(stringResource(Res.string.from)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxYears,
                        onValueChange = { maxYears = it },
                        label = { Text(stringResource(Res.string.to)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text(stringResource(Res.string.filter_coupon_type), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterRadioOption(
                        label = stringResource(Res.string.filter_coupon_all),
                        selected = couponType == null,
                        onClick = { couponType = null }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterRadioOption(
                        label = stringResource(Res.string.filter_coupon_fixed_short),
                        selected = couponType == CouponType.FIXED,
                        onClick = { couponType = CouponType.FIXED }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterRadioOption(
                        label = stringResource(Res.string.filter_coupon_floating_short),
                        selected = couponType == CouponType.FLOATING,
                        onClick = { couponType = CouponType.FLOATING }
                    )
                }

                Text(stringResource(Res.string.filter_listing_level), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterCheckbox("1", listLevels.contains(1)) { checked ->
                        listLevels = if (checked) listLevels + 1 else listLevels - 1
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterCheckbox("2", listLevels.contains(2)) { checked ->
                        listLevels = if (checked) listLevels + 2 else listLevels - 2
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilterCheckbox("3", listLevels.contains(3)) { checked ->
                        listLevels = if (checked) listLevels + 3 else listLevels - 3
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val filter = BondFilter(
                        issuerGroups = issuerGroups.ifEmpty { null },
                        minYieldToMat = minYield.toDoubleOrNull(),
                        maxYieldToMat = maxYield.toDoubleOrNull(),
                        minYearsToMat = minYears.toDoubleOrNull(),
                        maxYearsToMat = maxYears.toDoubleOrNull(),
                        couponType = couponType,
                        listLevels = listLevels.ifEmpty { null }
                    )
                    onApply(filter)
                }
            ) {
                Text(stringResource(Res.string.action_apply))
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
fun FilterCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
    }
}

@Composable
private fun FilterRadioOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, fontSize = 14.sp, modifier = Modifier.padding(end = 4.dp))
    }
}
