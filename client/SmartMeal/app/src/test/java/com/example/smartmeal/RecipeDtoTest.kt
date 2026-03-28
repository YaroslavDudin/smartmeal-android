package com.example.smartmeal

import com.example.smartmeal.feature.home.data.menu.RecipeDetailDto
import com.example.smartmeal.feature.home.data.menu.RecipeShortDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecipeDtoTest {
    private val gson = Gson()

    @Test
    fun parseRecipeShortList_fromApiSample() {
        val json = """
            [
              {
                "id": 1,
                "title": "Борщ",
                "cook_time": 60,
                "servings": 4,
                "total_calories": 1200.0,
                "total_proteins": 45.0,
                "total_fats": 30.0,
                "total_carbs": 150.0
              }
            ]
        """.trimIndent()

        val type = object : TypeToken<List<RecipeShortDto>>() {}.type
        val result: List<RecipeShortDto> = gson.fromJson(json, type)

        assertEquals(1, result.size)
        assertEquals(1, result.first().id)
        assertEquals("Борщ", result.first().title)
        assertEquals(60, result.first().cook_time)
        assertEquals(4, result.first().servings)
        assertEquals(1200.0, result.first().total_calories, 0.001)
    }

    @Test
    fun parseRecipeDetail_fromApiSample() {
        val json = """
            {
              "id": 1,
              "title": "Борщ",
              "cook_time": 60,
              "servings": 4,
              "total_calories": 1200.0,
              "total_proteins": 45.0,
              "total_fats": 30.0,
              "total_carbs": 150.0,
              "total_weight_g": 1600.0,
              "per_serving_calories": 300.0,
              "per_serving_proteins": 11.25,
              "per_serving_fats": 7.5,
              "per_serving_carbs": 37.5,
              "ingredients": [
                {
                  "ingredient_name": "Свёкла",
                  "amount": 300.0,
                  "unit_name": "г"
                }
              ],
              "steps": [
                { "step_number": 1, "description": "Нарезать свёклу соломкой", "timer": 1 }
              ]
            }
        """.trimIndent()

        val result: RecipeDetailDto = gson.fromJson(json, RecipeDetailDto::class.java)

        assertEquals(1, result.id)
        assertEquals("Борщ", result.title)
        assertEquals(60, result.cook_time)
        assertEquals(4, result.servings)
        assertEquals(1600.0, result.total_weight_g, 0.001)
        assertEquals(300.0, result.per_serving_calories, 0.001)
        assertNotNull(result.ingredients.firstOrNull())
        assertEquals("Свёкла", result.ingredients.first().ingredient_name)
        assertEquals(1, result.steps.first().step_number)
        assertEquals(1, result.steps.first().timer)
    }
}
