from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from app.recipes.models import RecipeIngredient, IngredientNutrition, UnitConversion

@receiver([post_save, post_delete], sender=RecipeIngredient)
def update_recipe_nutrition_on_ingredient_change(sender, instance, **kwargs):
    '''Обновляет КБЖУ рецепта при изменении его ингредиентов.'''
    instance.recipe.update_nutrition_cache()

@receiver(post_save, sender=IngredientNutrition)
def update_recipes_on_nutrition_change(sender, instance, **kwargs):
    '''Обновляет все рецепты, использующие данный ингредиент, при изменении его пищевой ценности.'''
    recipes = instance.ingredient.used_in_recipes.all().values_list('recipe_id', flat=True).distinct()
    for recipe_id in recipes:
        from app.recipes.models import Recipe
        recipe = Recipe.objects.get(pk=recipe_id)
        recipe.update_nutrition_cache()

@receiver([post_save, post_delete], sender=UnitConversion)
def update_recipes_on_conversion_change(sender, instance, **kwargs):
    '''Обновляет все рецепты, использующие данный ингредиент, при изменении коэффициентов конвертации.'''
    recipes = instance.ingredient.used_in_recipes.all().values_list('recipe_id', flat=True).distinct()
    for recipe_id in recipes:
        from app.recipes.models import Recipe
        recipe = Recipe.objects.get(pk=recipe_id)
        recipe.update_nutrition_cache()
