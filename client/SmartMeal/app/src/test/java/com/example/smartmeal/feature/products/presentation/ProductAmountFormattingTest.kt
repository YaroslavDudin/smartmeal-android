package com.example.smartmeal.feature.products.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductAmountFormattingTest {

    @Test
    fun formatWeightDisplay_keepsGramsBelowOneKilogram() {
        assertEquals("250 г", formatWeightDisplay(250.0))
    }

    @Test
    fun formatWeightDisplay_switchesToKilogramsAtOneThousandGrams() {
        assertEquals("1.5 кг", formatWeightDisplay(1500.0))
    }

    @Test
    fun formatWeightDisplay_dropsDecimalForWholeKilograms() {
        assertEquals("2 кг", formatWeightDisplay(2000.0))
    }
}
