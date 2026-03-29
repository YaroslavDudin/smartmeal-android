from decimal import Decimal
from django.test import TestCase
from app.recipes.models import Ingredient, IngredientCategory, Recipe, RecipeIngredient, Unit
from app.recipes.serializers import RecipeSerializer



class RecipesModelTests(TestCase):
    def test_create_recipe(self):
        recipe = Recipe.objects.create(
            title="Жареное мясо",
            cook_time=10,
            servings=1
        )
        self.assertEqual(recipe.title, "Жареное мясо")
        self.assertEqual(recipe.cook_time, 10)
        self.assertEqual(str(recipe), "Жареное мясо")

    def test_recipe_serializer_scales_ingredient_amount_for_target_servings(self):
        category = IngredientCategory.objects.create(name="Овощи")
        unit = Unit.objects.create(name="г", is_base=True)
        ingredient = Ingredient.objects.create(name="Картофель", category=category)
        recipe = Recipe.objects.create(
            title="Картофельное пюре",
            cook_time=30,
            servings=2
        )
        RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=ingredient,
            amount=Decimal("100.00"),
            unit=unit
        )

        serializer = RecipeSerializer(recipe, context={"target_servings": 4})
        ingredient_data = serializer.data["ingredients"][0]

        self.assertEqual(Decimal(str(ingredient_data["amount"])), Decimal("200.00"))
        self.assertEqual(ingredient_data["base_unit_name"], "г")
