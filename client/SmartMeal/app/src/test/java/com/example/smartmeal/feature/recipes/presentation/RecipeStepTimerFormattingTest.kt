package com.example.smartmeal.feature.recipes.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeStepTimerFormattingTest {

    @Test
    fun nullTimer_returnsNull() {
        assertNull(formatStepTimerLabel(null))
    }

    @Test
    fun zeroTimer_returnsNull() {
        assertNull(formatStepTimerLabel(0))
    }

    @Test
    fun positiveTimer_formatsMinutes() {
        assertEquals("1 мин", formatStepTimerLabel(1))
        assertEquals("3 мин", formatStepTimerLabel(3))
    }
}
