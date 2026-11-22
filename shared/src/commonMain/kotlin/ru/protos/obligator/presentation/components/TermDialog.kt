package ru.protos.obligator.presentation.components

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import obligator.shared.generated.resources.Res
import obligator.shared.generated.resources.action_understand
import org.jetbrains.compose.resources.stringResource

data class TermInfo(
    val title: String,
    val description: String
)

object TermExplanations {
    val YIELD_TO_MATURITY = TermInfo(
        "Доходность к погашению",
        "Ожидаемая годовая доходность облигации при условии ее удержания до даты погашения и реинвестирования купонов."
    )
    val CURRENT_YIELD = TermInfo(
        "Текущая доходность",
        "Отношение годовой суммы купона к текущей рыночной цене облигации."
    )
    val ACCRUED_INT = TermInfo(
        "НКД (Накопленный купонный доход)",
        "Накопленный купонный доход, выплачиваемый покупателем продавцу при покупке облигации."
    )
    val DURATION = TermInfo(
        "Дюрация",
        "Средневзвешенный срок до получения всех платежей по облигации (в днях). Чем выше дюрация, тем чувствительнее цена к изменению ставок."
    )
    val LIST_LEVEL = TermInfo(
        "Уровень листинга",
        "Уровень надежности и требований биржи к эмитенту (1 — высший котировальный список)."
    )
    val OFFER = TermInfo(
        "Оферта",
        "Право или обязанность эмитента досрочно выкупить облигацию по заранее определенной цене в установленную дату."
    )
    val AMORTIZATION = TermInfo(
        "Амортизация",
        "Постепенное погашение номинала облигации частями вместе с купонными выплатами."
    )
    val FLOATING_COUPON = TermInfo(
        "Плавающий купон",
        "Купонная ставка, привязанная к рыночному индикатору (например, ключевой ставке ЦБ) и регулярно пересчитываемая."
    )
}

@Composable
fun TermDialog(
    termInfo: TermInfo?,
    onDismiss: () -> Unit
) {
    if (termInfo != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = termInfo.title) },
            text = { Text(text = termInfo.description) },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.action_understand))
                }
            }
        )
    }
}
