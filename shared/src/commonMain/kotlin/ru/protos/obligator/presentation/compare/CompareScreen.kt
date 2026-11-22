package ru.protos.obligator.presentation.compare

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_back
import obligator.shared.generated.resources.action_delete
import obligator.shared.generated.resources.compare_different_yield_kinds
import obligator.shared.generated.resources.compare_empty
import obligator.shared.generated.resources.compare_legend
import obligator.shared.generated.resources.compare_title_prefix
import org.jetbrains.compose.resources.stringResource
import ru.protos.obligator.analytics.model.CompareRowModel
import ru.protos.obligator.presentation.theme.AppTheme

@Composable
fun CompareScreen(
    viewModel: CompareViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(Res.string.action_back))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "${stringResource(Res.string.compare_title_prefix)}${state.bonds.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.bonds.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(Res.string.compare_empty), color = Color.Gray)
            }
        } else {
            val bonds = state.bonds
            val compareResult = state.result
            val horizontalScrollState = rememberScrollState()

            Column(
                modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                compareResult.rows.forEach { rowModel ->
                    CompareRow(
                        rowModel = rowModel,
                        horizontalScrollState = horizontalScrollState,
                        onItemClick = if (rowModel.id == "shortName") { index ->
                            val isinToCopy = bonds.getOrNull(index)?.isin ?: bonds.getOrNull(index)?.secid ?: ""
                            if (isinToCopy.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(isinToCopy))
                            }
                        } else null,
                        action = if (rowModel.id == "shortName") { index ->
                            IconButton(
                                onClick = { viewModel.removeSecid(bonds[index].secid) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.action_delete),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (compareResult.hasDifferentYieldKinds) {
                    Text(
                        text = stringResource(Res.string.compare_different_yield_kinds),
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = stringResource(Res.string.compare_legend),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CompareRow(
    rowModel: CompareRowModel,
    horizontalScrollState: ScrollState,
    onItemClick: ((Int) -> Unit)? = null,
    action: (@Composable (Int) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rowModel.label,
            fontSize = 12.sp,
            fontWeight = if (rowModel.isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (rowModel.isHeader) Color.Unspecified else Color.Gray,
            modifier = Modifier
                .width(108.dp)
                .padding(end = 6.dp)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(horizontalScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rowModel.values.forEachIndexed { index, value ->
                val isHighlighted = rowModel.highlighted.contains(index)
                val cellBg = if (isHighlighted) AppTheme.customColors.highlightBackground else Color.Transparent
                val cellTextColor = if (isHighlighted) AppTheme.customColors.highlightText else Color.Unspecified
                val cellFontWeight = if (rowModel.isHeader || isHighlighted) FontWeight.Bold else FontWeight.Medium
                val displayText = if (isHighlighted && value != "нет данных") "▲ $value" else value
                val isIsin = rowModel.id == "isin"

                Box(
                    modifier = Modifier
                        .width(122.dp)
                        .fillMaxHeight()
                        .background(cellBg)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .then(if (onItemClick != null) Modifier.clickable { onItemClick(index) } else Modifier),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayText,
                            fontWeight = cellFontWeight,
                            color = cellTextColor,
                            fontSize = if (isIsin) 11.5.sp else 12.sp,
                            maxLines = if (isIsin) 1 else 2,
                            softWrap = !isIsin,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        action?.invoke(index)
                    }
                }
            }
        }
    }
    Divider()
}
