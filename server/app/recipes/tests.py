from django.test import TestCase
from app.recipes.models import Recipe

class RecipesModelTests(TestCase):
    def test_create_recipe(self):
        recipe = Recipe.objects.create(
            title="Жареное мясо злокрыса",
            cook_time=10,
            servings=1,
            calories=150,
            protein=10.5,
            fat=12.0,
            carbs=2.0
        )
        self.assertEqual(recipe.title, "Жареное мясо злокрыса")
        self.assertEqual(recipe.cook_time, 10)
        self.assertEqual(str(recipe), "Жареное мясо злокрыса")
# Create your tests here.
