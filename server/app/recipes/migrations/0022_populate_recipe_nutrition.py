from django.db import migrations
from decimal import Decimal

def populate_nutrition(apps, schema_editor):
    Recipe = apps.get_model('recipes', 'Recipe')
    RecipeIngredient = apps.get_model('recipes', 'RecipeIngredient')
    IngredientNutrition = apps.get_model('recipes', 'IngredientNutrition')
    UnitConversion = apps.get_model('recipes', 'UnitConversion')

    # Константы для расчета
    CALORIES_PER_GRAM = {
        'protein': Decimal('4.0'),
        'carbs': Decimal('4.0'),
        'fat': Decimal('9.0'),
    }

    for recipe in Recipe.objects.all():
        protein = fat = carbs = Decimal(0)
        
        for ri in RecipeIngredient.objects.filter(recipe=recipe):
            try:
                # Получаем КБЖУ ингредиента
                nutrition = IngredientNutrition.objects.get(ingredient=ri.ingredient)
                
                # Конвертация количества в базовые единицы
                amount = ri.amount
                if ri.unit.id != nutrition.base_unit.id:
                    # Ищем конвертацию
                    try:
                        conv = UnitConversion.objects.get(
                            ingredient=ri.ingredient,
                            from_unit=ri.unit,
                            to_unit=nutrition.base_unit
                        )
                        amount = ri.amount * conv.amount_per_unit
                    except UnitConversion.DoesNotExist:
                        # Пробуем обратную конвертацию
                        try:
                            conv = UnitConversion.objects.get(
                                ingredient=ri.ingredient,
                                from_unit=nutrition.base_unit,
                                to_unit=ri.unit
                            )
                            amount = ri.amount / conv.amount_per_unit
                        except UnitConversion.DoesNotExist:
                            continue # Нет конвертации

                scale = amount / Decimal(nutrition.base_weight)
                protein += scale * nutrition.protein
                fat += scale * nutrition.fat
                carbs += scale * nutrition.carbs
            except IngredientNutrition.DoesNotExist:
                continue
        
        if recipe.servings > 0:
            recipe.per_serving_proteins = protein / recipe.servings
            recipe.per_serving_fats = fat / recipe.servings
            recipe.per_serving_carbs = carbs / recipe.servings
            recipe.per_serving_calories = (
                recipe.per_serving_proteins * CALORIES_PER_GRAM['protein']
                + recipe.per_serving_carbs * CALORIES_PER_GRAM['carbs']
                + recipe.per_serving_fats * CALORIES_PER_GRAM['fat']
            )
        else:
            recipe.per_serving_proteins = recipe.per_serving_fats = \
            recipe.per_serving_carbs = recipe.per_serving_calories = Decimal(0)

        recipe.save(update_fields=[
            'per_serving_proteins', 'per_serving_fats', 
            'per_serving_carbs', 'per_serving_calories'
        ])

class Migration(migrations.Migration):
    dependencies = [
        ('recipes', '0021_recipe_per_serving_calories_recipe_per_serving_carbs_and_more'),
    ]

    operations = [
        migrations.RunPython(populate_nutrition, reverse_code=migrations.RunPython.noop),
    ]
