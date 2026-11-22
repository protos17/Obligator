package ru.protos.obligator.presentation

import ru.protos.obligator.presentation.compare.CompareSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompareSelectionTest {

    @Test
    fun testCompareSelectionLimitThree() {
        val selection = CompareSelection()
        selection.toggle("B1")
        selection.toggle("B2")
        selection.toggle("B3")

        assertEquals(3, selection.selectedSecids.value.size)

        selection.toggle("B4")
        assertEquals(3, selection.selectedSecids.value.size)
        assertTrue(selection.isSelected("B1"))
        assertTrue(selection.isSelected("B2"))
        assertTrue(selection.isSelected("B3"))

        selection.toggle("B2")
        assertEquals(2, selection.selectedSecids.value.size)
    }

    @Test
    fun testCompareSelectionClear() {
        val selection = CompareSelection()
        selection.toggle("B1")
        selection.toggle("B2")
        assertEquals(2, selection.selectedSecids.value.size)

        selection.clear()
        assertEquals(0, selection.selectedSecids.value.size)
    }
}
