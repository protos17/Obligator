package ru.protos.obligator.presentation.compare

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CompareSelection {
    private val _selectedSecids = MutableStateFlow<Set<String>>(emptySet())
    val selectedSecids: StateFlow<Set<String>> = _selectedSecids.asStateFlow()

    /** Добавляет или убирает облигацию из списка сравнения (до трёх бумаг). */
    fun toggle(secid: String) {
        val current = _selectedSecids.value
        if (current.contains(secid)) {
            _selectedSecids.value = current - secid
        } else {
            if (current.size < 3) {
                _selectedSecids.value = current + secid
            }
        }
    }

    fun remove(secid: String) {
        _selectedSecids.value = _selectedSecids.value - secid
    }

    fun clear() {
        _selectedSecids.value = emptySet()
    }

    fun isSelected(secid: String): Boolean = _selectedSecids.value.contains(secid)

    fun canSelectMore(): Boolean = _selectedSecids.value.size < 3
}
