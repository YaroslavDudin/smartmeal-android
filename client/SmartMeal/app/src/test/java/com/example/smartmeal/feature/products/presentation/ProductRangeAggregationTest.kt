package com.example.smartmeal.feature.products.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductRangeAggregationTest {

    @Test
    fun aggregateProductsForDisplay_sumsSameIngredientWithinSelectedRange() {
        val filteredProducts = filterProductsByDayRange(
            products = listOf(
                product(
                    id = "monday",
                    name = "Куриное филе",
                    amount = "500 г",
                    dayOffset = 0,
                    actualDate = "2026-03-23"
                ),
                product(
                    id = "tuesday",
                    name = "Куриное филе",
                    amount = "400 г",
                    dayOffset = 1,
                    actualDate = "2026-03-24"
                ),
                product(
                    id = "wednesday",
                    name = "Куриное филе",
                    amount = "300 г",
                    dayOffset = 2,
                    actualDate = "2026-03-25"
                )
            ),
            startIndex = 0,
            endIndex = 1
        )

        val aggregatedProducts = aggregateProductsForDisplay(filteredProducts)

        assertEquals(1, aggregatedProducts.size)
        assertEquals("900 г", aggregatedProducts.first().amount)
        assertEquals(setOf("monday", "tuesday"), aggregatedProducts.first().sourceIds)
    }

    @Test
    fun aggregateProductsForDisplay_keepsCheckedAndUncheckedAmountsSeparate() {
        val aggregatedProducts = aggregateProductsForDisplay(
            listOf(
                product(
                    id = "monday",
                    name = "Куриное филе",
                    amount = "500 г",
                    dayOffset = 0,
                    actualDate = "2026-03-23",
                    checked = true
                ),
                product(
                    id = "tuesday",
                    name = "Куриное филе",
                    amount = "400 г",
                    dayOffset = 1,
                    actualDate = "2026-03-24",
                    checked = false
                )
            )
        )

        val purchases = aggregatedProducts.first { it.checked }
        val regular = aggregatedProducts.first { !it.checked }

        assertEquals("Покупки", purchases.categoryName)
        assertEquals("500 г", purchases.amount)
        assertEquals("Мясо и рыба", regular.categoryName)
        assertEquals("400 г", regular.amount)
    }

    @Test
    fun isExcludedIngredient_excludesWaterOnly() {
        assertTrue(isExcludedIngredient("Вода"))
        assertTrue(isExcludedIngredient("water"))
        assertEquals(false, isExcludedIngredient("Куриное филе"))
    }

    private fun product(
        id: String,
        name: String,
        amount: String,
        dayOffset: Int,
        actualDate: String,
        checked: Boolean = false
    ) = ProductUiModel(
        id = id,
        name = name,
        amount = amount,
        category = "Мясо и рыба",
        icon = "",
        categoryName = "Мясо и рыба",
        categoryIcon = "",
        checked = checked,
        dayOffsets = setOf(dayOffset),
        actualDates = setOf(actualDate),
        sourceIds = setOf(id)
    )
}
