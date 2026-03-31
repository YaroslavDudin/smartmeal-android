package com.example.smartmeal.feature.recipes.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeNutritionFormattingTest {

    @Test
    fun calculatePer100_returnsScaledValue() {
        assertEquals(75.0, calculatePer100(totalValue = 1200.0, totalWeightG = 1600.0), 0.001)
    }

    @Test
    fun calculatePer100_returnsZeroWhenWeightIsInvalid() {
        assertEquals(0.0, calculatePer100(totalValue = 1200.0, totalWeightG = 0.0), 0.001)
    }

    @Test
    fun formatNutritionValue_trimsTrailingZero() {
        assertEquals("7.5", formatNutritionValue(7.5))
        assertEquals("12", formatNutritionValue(12.0))
    }

    @Test
    fun formatWeightLabel_usesGrams() {
        assertEquals("1600 г", formatWeightLabel(1600.0))
    }
}
