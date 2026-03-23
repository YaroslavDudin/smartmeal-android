package com.example.smartmeal.feature.home.data.menu

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecipeDtoParsingTest {
    private val gson = Gson()

    @Test
    fun parseRecipeShortList_fromApiResponse() {
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
        val recipe = result.first()
        assertEquals(1, recipe.id)
        assertEquals("Борщ", recipe.title)
        assertEquals(60, recipe.cook_time)
        assertEquals(4, recipe.servings)
        assertEquals(1200.0, recipe.total_calories, 0.001)
    }

    @Test
    fun parseRecipeDetail_fromApiResponse() {
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
                { "step_number": 1, "description": "Нарезать свёклу соломкой" }
              ]
            }
        """.trimIndent()

        val recipe: RecipeDetailDto = gson.fromJson(json, RecipeDetailDto::class.java)

        assertEquals(1, recipe.id)
        assertEquals("Борщ", recipe.title)
        assertEquals(60, recipe.cook_time)
        assertEquals(4, recipe.servings)
        assertEquals(300.0, recipe.per_serving_calories, 0.001)
        assertNotNull(recipe.ingredients)
        assertEquals(1, recipe.ingredients.size)
        assertEquals("Свёкла", recipe.ingredients.first().ingredient_name)
        assertEquals("г", recipe.ingredients.first().unit_name)
        assertEquals(1, recipe.steps.size)
        assertEquals(1, recipe.steps.first().step_number)
    }
}
