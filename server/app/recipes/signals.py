from django.db.models.signals import post_save, post_delete
from django.dispatch import receiver
from .models import RecipeIngredient, IngredientNutrition, UnitConversion, Recipe

@receiver(post_save, sender=RecipeIngredient)
@receiver(post_delete, sender=RecipeIngredient)
def update_recipe_totals_on_ingredient_change(sender, instance, **kwargs):
    # 1. Защита от падения при loaddata
    if kwargs.get('raw', False):
        return
        
    # 2. Защита от краша при каскадном удалении
    # Если мы удаляем сам рецепт, не нужно пытаться пересчитать его тоталы
    if not Recipe.objects.filter(pk=instance.recipe_id).exists():
        return

    instance.recipe.update_totals()

@receiver(post_save, sender=IngredientNutrition)
def update_recipe_totals_on_nutrition_change(sender, instance, **kwargs):
    # Защита от падения при loaddata
    if kwargs.get('raw', False):
        return
        
    # Обновляем все рецепты, где используется этот ингредиент
    recipes = Recipe.objects.filter(recipe_ingredients__ingredient=instance.ingredient).distinct()
    for recipe in recipes:
        recipe.update_totals()

@receiver(post_save, sender=UnitConversion)
def update_recipe_totals_on_conversion_change(sender, instance, **kwargs):
    # Защита от падения при loaddata
    if kwargs.get('raw', False):
        return
        
    # Обновляем все рецепты, где используется этот ингредиент с этой единицей измерения
    recipes = Recipe.objects.filter(
        recipe_ingredients__ingredient=instance.ingredient,
        recipe_ingredients__unit=instance.unit
    ).distinct()
    for recipe in recipes:
        recipe.update_totals()