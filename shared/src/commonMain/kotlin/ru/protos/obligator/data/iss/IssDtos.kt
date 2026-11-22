package ru.protos.obligator.data.iss

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class IssTableDto(
    val columns: List<String> = emptyList(),
    val data: List<List<JsonElement>> = emptyList()
)

@Serializable
data class IssSecuritiesResponseDto(
    val securities: IssTableDto? = null,
    val marketdata: IssTableDto? = null,
    @SerialName("marketdata_yields")
    val marketdataYields: IssTableDto? = null
)

@Serializable
data class IssBondizationResponseDto(
    val coupons: IssTableDto? = null,
    val amortizations: IssTableDto? = null,
    val offers: IssTableDto? = null,
    @SerialName("coupons.cursor")
    val couponsCursor: IssTableDto? = null,
    @SerialName("amortizations.cursor")
    val amortizationsCursor: IssTableDto? = null
)

@Serializable
data class IssDescriptionResponseDto(
    val description: IssTableDto? = null
)

fun IssTableDto.getColumnIndex(name: String): Int = columns.indexOf(name)

/** Извлекает непустую строку по индексу, игнорируя нулевую дату. */
fun List<JsonElement>.getString(index: Int): String? {
    if (index < 0 || index >= size) return null
    val elem = get(index)
    if (elem is JsonPrimitive && elem.isString) {
        val str = elem.content
        return if (str.isBlank() || str == "0000-00-00") null else str
    }
    return null
}

/** Извлекает число Double по индексу с опциональной заменой нуля на null. */
fun List<JsonElement>.getDouble(index: Int, zeroAsNull: Boolean = false): Double? {
    if (index < 0 || index >= size) return null
    val elem = get(index)
    if (elem is JsonPrimitive) {
        val d = elem.doubleOrNull ?: return null
        if (zeroAsNull && d == 0.0) return null
        return d
    }
    return null
}

fun List<JsonElement>.getInt(index: Int): Int? {
    if (index < 0 || index >= size) return null
    val elem = get(index)
    if (elem is JsonPrimitive) {
        return elem.intOrNull
    }
    return null
}
