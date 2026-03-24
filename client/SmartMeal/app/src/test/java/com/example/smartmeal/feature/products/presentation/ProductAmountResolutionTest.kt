package com.example.smartmeal.feature.products.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductAmountResolutionTest {

    @Test
    fun resolveAmountInGrams_usesBackendValueWhenItIsPositive() {
        val result = resolveAmountInGrams(
            amountInGrams = 42.0,
            fallbackAmount = 2.0,
            fallbackUnit = "tablespoon"
        )

        assertEquals(42.0, result, 0.0)
    }

    @Test
    fun resolveAmountInGrams_fallsBackForZeroGramTablespoon() {
        val result = resolveAmountInGrams(
            amountInGrams = 0.0,
            fallbackAmount = 2.0,
            fallbackUnit = "tablespoon"
        )

        assertEquals(30.0, result, 0.0)
    }

    @Test
    fun resolveAmountInGrams_fallsBackForZeroGramTeaspoon() {
        val result = resolveAmountInGrams(
            amountInGrams = 0.0,
            fallbackAmount = 3.0,
            fallbackUnit = "teaspoon"
        )

        assertEquals(15.0, result, 0.0)
    }
}
