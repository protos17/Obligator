package ru.protos.obligator.presentation.card

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_add_to_portfolio
import obligator.shared.generated.resources.action_back
import obligator.shared.generated.resources.action_cancel
import obligator.shared.generated.resources.action_copy_isin
import obligator.shared.generated.resources.action_help
import obligator.shared.generated.resources.action_save
import obligator.shared.generated.resources.bond_not_found_on_exchange
import obligator.shared.generated.resources.card_features
import obligator.shared.generated.resources.card_main_info
import obligator.shared.generated.resources.coupon_calendar_empty
import obligator.shared.generated.resources.coupon_calendar_title
import obligator.shared.generated.resources.dialog_portfolio_quantity_label
import obligator.shared.generated.resources.dialog_portfolio_quantity_title
import obligator.shared.generated.resources.disclaimer_text
import obligator.shared.generated.resources.error_offline_payments
import obligator.shared.generated.resources.feature_all_investors
import obligator.shared.generated.resources.feature_amortization
import obligator.shared.generated.resources.feature_floating_coupon
import obligator.shared.generated.resources.feature_has_offer
import obligator.shared.generated.resources.feature_qualified
import obligator.shared.generated.resources.filter_listing_level
import obligator.shared.generated.resources.filter_mat_date
import obligator.shared.generated.resources.label_accrued_int
import obligator.shared.generated.resources.label_coupon_percent
import obligator.shared.generated.resources.label_coupon_period
import obligator.shared.generated.resources.label_coupon_value
import obligator.shared.generated.resources.label_current_yield
import obligator.shared.generated.resources.label_duration
import obligator.shared.generated.resources.label_face_value
import obligator.shared.generated.resources.label_isin
import obligator.shared.generated.resources.label_next_coupon
import obligator.shared.generated.resources.label_offer
import obligator.shared.generated.resources.label_price
import obligator.shared.generated.resources.label_sec_type
import obligator.shared.generated.resources.label_secid
import obligator.shared.generated.resources.label_ytm
import obligator.shared.generated.resources.payment_coupon
import obligator.shared.generated.resources.payment_repayment
import obligator.shared.generated.resources.tab_favorites
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.calc.calculateCurrentYield
import ru.protos.obligator.analytics.calc.getIssuerGroupRu
import ru.protos.obligator.analytics.model.PaymentKind
import ru.protos.obligator.presentation.components.TermDialog
import ru.protos.obligator.presentation.components.TermExplanations
import ru.protos.obligator.presentation.components.TermInfo
import ru.protos.obligator.presentation.util.formatDate
import ru.protos.obligator.presentation.util.formatDuration
import ru.protos.obligator.presentation.util.formatPercent
import ru.protos.obligator.presentation.util.formatRubles
import ru.protos.obligator.presentation.util.formatString

@Composable
fun BondCardScreen(
    secid: String,
    viewModel: BondCardViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showPortfolioDialog by remember { mutableStateOf(false) }
    var selectedTermInfo by remember { mutableStateOf<TermInfo?>(null) }

    LaunchedEffect(secid) {
        viewModel.loadBond(secid)
    }

    val bond = state.bond
    val displayTitle = bond?.shortName ?: state.fallbackShortName ?: secid

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.action_back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { viewModel.toggleFavorite() }) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(Res.string.tab_favorites),
                    tint = if (state.isFavorite) Color.Red else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading && bond == null && !state.isMissingFromExchange) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.isMissingFromExchange) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = stringResource(Res.string.bond_not_found_on_exchange),
                            color = Color(0xFFE65100),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                if (state.errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = stringResource(Res.string.error_offline_payments),
                            color = Color(0xFFC62828),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = stringResource(Res.string.card_main_info), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        MetricRow(stringResource(Res.string.label_secid), bond?.secid ?: state.secid)
                        val isin = bond?.isin
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(Res.string.label_isin), color = Color.Gray, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatString(isin), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                if (!isin.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(isin))
                                        },
                                        modifier = Modifier.height(24.dp).width(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(Res.string.action_copy_isin),
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                        MetricRow(
                            stringResource(Res.string.label_sec_type),
                            if (bond != null) getIssuerGroupRu(bond.secType) else formatString(null)
                        )
                        MetricRow(stringResource(Res.string.label_face_value), formatRubles(bond?.faceValue))
                        MetricRow(stringResource(Res.string.label_price), formatPercent(bond?.price))

                        MetricRowWithHelp(
                            label = stringResource(Res.string.label_accrued_int),
                            value = formatRubles(bond?.accruedInt),
                            onHelp = { selectedTermInfo = TermExplanations.ACCRUED_INT }
                        )
                        val ytmValue = if (bond?.yieldToMaturity == null) {
                            formatPercent(null)
                        } else {
                            "${formatPercent(bond.yieldToMaturity)} ${bond.yieldKind ?: ""}".trim()
                        }
                        MetricRowWithHelp(
                            label = stringResource(Res.string.label_ytm),
                            value = ytmValue,
                            onHelp = { selectedTermInfo = TermExplanations.YIELD_TO_MATURITY }
                        )
                        MetricRowWithHelp(
                            label = stringResource(Res.string.label_current_yield),
                            value = formatPercent(calculateCurrentYield(bond?.couponPercent, bond?.price)),
                            onHelp = { selectedTermInfo = TermExplanations.CURRENT_YIELD }
                        )
                        MetricRowWithHelp(
                            label = stringResource(Res.string.label_duration),
                            value = formatDuration(bond?.duration),
                            onHelp = { selectedTermInfo = TermExplanations.DURATION }
                        )
                        MetricRowWithHelp(
                            label = stringResource(Res.string.filter_listing_level),
                            value = formatString(bond?.listLevel?.toString()),
                            onHelp = { selectedTermInfo = TermExplanations.LIST_LEVEL }
                        )

                        MetricRow(stringResource(Res.string.label_coupon_percent), formatPercent(bond?.couponPercent))
                        MetricRow(stringResource(Res.string.label_coupon_value), formatRubles(bond?.couponValue))
                        MetricRow(stringResource(Res.string.label_coupon_period), formatDuration(bond?.couponPeriod))
                        MetricRow(stringResource(Res.string.label_next_coupon), formatDate(bond?.nextCoupon))
                        MetricRow(stringResource(Res.string.filter_mat_date), formatDate(bond?.matDate))

                        MetricRowWithHelp(
                            label = stringResource(Res.string.label_offer),
                            value = formatDate(bond?.offerDate),
                            onHelp = { selectedTermInfo = TermExplanations.OFFER }
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth(), elevation = 2.dp) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = stringResource(Res.string.card_features), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (bond?.bondType?.contains("Амортизируем", ignoreCase = true) == true) {
                            BadgeRow(stringResource(Res.string.feature_amortization), TermExplanations.AMORTIZATION) { selectedTermInfo = it }
                        }
                        if (bond?.bondType?.contains("плавающ", ignoreCase = true) == true) {
                            BadgeRow(stringResource(Res.string.feature_floating_coupon), TermExplanations.FLOATING_COUPON) { selectedTermInfo = it }
                        }
                        if (bond?.offerDate != null) {
                            BadgeRow(stringResource(Res.string.feature_has_offer), TermExplanations.OFFER) { selectedTermInfo = it }
                        }
                        if (state.isQualified == true) {
                            Text(stringResource(Res.string.feature_qualified), color = Color.Red, fontWeight = FontWeight.Medium)
                        } else if (state.isQualified == false) {
                            Text(stringResource(Res.string.feature_all_investors), color = Color(0xFF00796B))
                        }
                    }
                }

                if (bond != null || state.portfolioQuantity > 0 || state.fallbackShortName != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { showPortfolioDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (state.portfolioQuantity > 0) "В портфеле: ${state.portfolioQuantity} шт. (изменить)"
                                else stringResource(Res.string.action_add_to_portfolio)
                            )
                        }
                    }
                }

                Text(text = stringResource(Res.string.coupon_calendar_title), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (state.payments.isEmpty()) {
                    Text(stringResource(Res.string.coupon_calendar_empty), color = Color.Gray, fontSize = 14.sp)
                } else {
                    state.payments.forEach { payment ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDate(payment.date), fontSize = 14.sp)
                            Text(
                                if (payment.kind == PaymentKind.REPAYMENT) stringResource(Res.string.payment_repayment) else stringResource(Res.string.payment_coupon),
                                fontSize = 14.sp,
                                color = if (payment.kind == PaymentKind.REPAYMENT) Color(0xFF1565C0) else Color.DarkGray
                            )
                            Text(formatRubles(payment.amount), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Divider()
                    }
                }
            }
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

    if (showPortfolioDialog) {
        PortfolioQuantityDialog(
            currentQuantity = state.portfolioQuantity,
            onSave = { qty ->
                viewModel.addToPortfolio(qty)
                showPortfolioDialog = false
            },
            onDismiss = { showPortfolioDialog = false }
        )
    }

    TermDialog(termInfo = selectedTermInfo, onDismiss = { selectedTermInfo = null })
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun MetricRowWithHelp(label: String, value: String, onHelp: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Color.Gray, fontSize = 14.sp)
            IconButton(onClick = onHelp, modifier = Modifier.height(24.dp).width(24.dp)) {
                Icon(Icons.Default.HelpOutline, contentDescription = stringResource(Res.string.action_help), tint = Color.Gray)
            }
        }
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun BadgeRow(label: String, termInfo: TermInfo, onHelp: (TermInfo) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("• $label", fontWeight = FontWeight.Medium)
        IconButton(onClick = { onHelp(termInfo) }, modifier = Modifier.height(24.dp).width(24.dp)) {
            Icon(Icons.Default.HelpOutline, contentDescription = stringResource(Res.string.action_help), tint = Color.Gray)
        }
    }
}

@Composable
fun PortfolioQuantityDialog(
    currentQuantity: Int,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(if (currentQuantity > 0) currentQuantity.toString() else "1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_portfolio_quantity_title)) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(Res.string.dialog_portfolio_quantity_label)) },
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
