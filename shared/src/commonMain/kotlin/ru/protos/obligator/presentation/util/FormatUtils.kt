package ru.protos.obligator.presentation.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

/** Форматирует число до двух знаков после запятой или возвращает «нет данных». */
fun formatDouble(value: Double?): String {
    if (value == null) return "нет данных"
    val rounded = (round(value * 100.0) / 100.0)
    val isNegative = rounded < 0
    val absVal = if (isNegative) -rounded else rounded
    val intPart = absVal.toLong()
    val fracPart = round((absVal - intPart) * 100).toLong()
    val fracStr = if (fracPart < 10) "0$fracPart" else "$fracPart"
    val signStr = if (isNegative) "-" else ""
    return "$signStr$intPart,$fracStr"
}

fun formatPercent(value: Double?): String {
    if (value == null) return "нет данных"
    return "${formatDouble(value)}%"
}

fun formatRubles(value: Double?): String {
    if (value == null) return "нет данных"
    return "${formatDouble(value)} ₽"
}

fun formatDuration(value: Int?): String {
    if (value == null) return "нет данных"
    return "$value дн."
}

fun formatString(value: String?): String {
    return value ?: "нет данных"
}

/** Форматирует дату из «YYYY-MM-DD» в «DD.MM.YYYY». */
fun formatDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank() || dateStr == "0000-00-00") return "нет данных"
    val parts = dateStr.split("-")
    if (parts.size == 3 && parts[0].length == 4 && parts[1].length == 2 && parts[2].length == 2) {
        return "${parts[2]}.${parts[1]}.${parts[0]}"
    }
    return dateStr
}

/** Форматирует ключ месяца из «YYYY-MM» в «MM.YYYY». */
fun formatMonthKey(monthKey: String): String {
    val parts = monthKey.split("-")
    if (parts.size == 2 && parts[0].length == 4 && parts[1].length == 2) {
        return "${parts[1]}.${parts[0]}"
    }
    return monthKey
}

/** Форматирует метку времени в локальные дату и время. */
fun formatDateTime(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis == 0L) return "нет данных"
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val dayStr = if (dt.dayOfMonth < 10) "0${dt.dayOfMonth}" else "${dt.dayOfMonth}"
        val monthStr = if (dt.monthNumber < 10) "0${dt.monthNumber}" else "${dt.monthNumber}"
        val yearStr = "${dt.year}"
        val hourStr = if (dt.hour < 10) "0${dt.hour}" else "${dt.hour}"
        val minStr = if (dt.minute < 10) "0${dt.minute}" else "${dt.minute}"
        "$dayStr.$monthStr.$yearStr $hourStr:$minStr"
    } catch (_: Exception) {
        "нет данных"
    }
}
