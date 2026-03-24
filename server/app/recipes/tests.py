from django.test import TestCase
from app.recipes.models import Recipe


class RecipesModelTests(TestCase):
    def test_create_recipe(self):
        recipe = Recipe.objects.create(
            title="Жареное мясо злокрыса",
            cook_time=10,
            servings=1
        )
        self.assertEqual(recipe.title, "Жареное мясо злокрыса")
        self.assertEqual(recipe.cook_time, 10)
        self.assertEqual(str(recipe), "Жареное мясо злокрыса")
