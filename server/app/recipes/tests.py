from django.core.exceptions import ValidationError
from decimal import Decimal
from django.test import TestCase, RequestFactory
from django.contrib.auth.models import AnonymousUser
from app.recipes.models import Ingredient, IngredientCategory, Recipe, RecipeIngredient, Unit, IngredientNutrition
from app.recipes.serializers import RecipeSerializer
from app.recipes.models import (
    Ingredient, IngredientNutrition, IngredientCategory,
    Recipe, RecipeIngredient, Unit, UnitConversion,
)


class RecipesModelTests(TestCase):
    def setUp(self):
        self.unit_kg = Unit.objects.create(name='кг')
        self.base_unit_g = Unit.objects.create(name='г', is_base=True)
        self.unit_cup = Unit.objects.create(name='стакан', is_base=False)
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель',
            category=self.category,
            can_be_added_to_cart=True,
        )
        self.nutrition = IngredientNutrition.objects.create(
            ingredient=self.ingredient,
            base_unit=self.base_unit_g,
            base_weight=100,
            protein=Decimal('2.0'),
            fat=Decimal('0.1'),
            carbs=Decimal('16.0'),
        )
        self.conversion_kg_to_g = UnitConversion.objects.create(
            ingredient=self.ingredient,
            from_unit=self.unit_kg,
            to_unit=self.base_unit_g,
            amount_per_unit=Decimal('1000'),
        )
        self.conversion_cup_to_g = UnitConversion.objects.create(
            ingredient=self.ingredient,
            from_unit=self.unit_cup,
            to_unit=self.base_unit_g,
            amount_per_unit=Decimal('150'),
        )
        self.recipe = Recipe.objects.create(
            title='Картофельное пюре',
            cook_time=30,
            servings=2
        )

    def test_create_recipe(self):
        recipe = Recipe.objects.create(
            title='Жареное мясо',
            cook_time=10,
            servings=1
        )
        self.assertEqual(recipe.title, 'Жареное мясо')
        self.assertEqual(recipe.cook_time, 10)
        self.assertEqual(str(recipe), 'Жареное мясо')

    def test_recipe_serializer_scales_ingredient_amount_for_target_servings(self):
        RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount=Decimal('100.00'),
            unit=self.unit_kg
        )
        serializer = RecipeSerializer(self.recipe, context={'target_servings': 4})
        ingredient_data = serializer.data['ingredients'][0]

        self.assertEqual(Decimal(ingredient_data['amount']), Decimal('200.00'))
        self.assertEqual(ingredient_data['base_unit_name'], 'г')

    def test_recipe_ingredient_fails_if_no_nutrition(self):
        ingredient = Ingredient.objects.create(
            name='Морковь',
            category=self.category,
        )
        with self.assertRaises(ValidationError) as cm:
            RecipeIngredient.objects.create(
                recipe=self.recipe,
                ingredient=ingredient,
                amount=Decimal('100.0'),
                unit=self.base_unit_g
            )
            self.assertIn('пищевая ценность', str(cm.exception).lower())

    def test_recipe_ingredient_fails_if_no_conversion(self):
        recipe = Recipe.objects.create(
            title='Картофельное пюре',
            cook_time=30,
            servings=2
        )
        new_unit = Unit.objects.create(name='ложка', is_base=False)
        with self.assertRaises(ValidationError) as cm:
            RecipeIngredient.objects.create(
                recipe=recipe,
                ingredient=self.ingredient,
                amount=Decimal('100.00'),
                unit=new_unit
            )
        self.assertIn('нет конвертации', str(cm.exception).lower())

    def test_recipe_ingredient_with_conversion(self):
        ri = RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )

        self.assertEqual(ri.amount_in_base_units, Decimal('1000'))
        protein, fat, carbs = ri.get_macros()
        scale = ri.amount_in_base_units / self.nutrition.base_weight
        self.assertEqual(protein, (self.nutrition.protein * scale))
        self.assertEqual(fat, (self.nutrition.fat * scale))
        self.assertEqual(carbs, (self.nutrition.carbs * scale))
        self.assertEqual(ri.calories, (self.nutrition.calories * scale))

    def test_recipe_totals(self):
        recipe = Recipe.objects.create(
            title='Картошка тушеная с морковкой',
            cook_time=20,
            servings=2,
        )
        potato = RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount=Decimal('2.5'),
            unit=self.unit_kg
        )
        ingredient2 = Ingredient.objects.create(
            name='Морковь',
            category=self.category,
        )
        IngredientNutrition.objects.create(
            ingredient=ingredient2,
            base_unit=self.base_unit_g,
            base_weight=100,
            protein=Decimal('1.0'),
            fat=Decimal('0.2'),
            carbs=Decimal('8.0'),
        )
        UnitConversion.objects.create(
            ingredient=ingredient2,
            from_unit=self.unit_kg,
            to_unit=self.base_unit_g,
            amount_per_unit=Decimal('1000'),
        )
        carrot = RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=ingredient2,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        
        potato_macros = potato.get_macros()
        carrot_macros = carrot.get_macros()
        total_protein = potato_macros[0] + carrot_macros[0]
        total_fat = potato_macros[1] + carrot_macros[1]
        total_carbs = potato_macros[2] + carrot_macros[2]
        total_calories = potato.calories + carrot.calories

        self.assertEqual(recipe.total_proteins, total_protein)
        self.assertEqual(recipe.total_fats, total_fat)
        self.assertEqual(recipe.total_carbs, total_carbs)
        self.assertEqual(recipe.total_calories, total_calories)
        self.assertEqual(recipe.per_serving_proteins, Decimal(total_protein / recipe.servings))
        self.assertEqual(recipe.per_serving_fats, Decimal(total_fat / recipe.servings))
        self.assertEqual(recipe.per_serving_carbs, Decimal(total_carbs / recipe.servings))
        self.assertEqual(recipe.per_serving_calories, Decimal(total_calories / recipe.servings))

    def test_recipe_ingredient_conversion_reverse(self):
        recipe = Recipe.objects.create(
            title='Тест',
            cook_time=10,
            servings=1
        )
        ri = RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount=Decimal('3'),
            unit=self.unit_cup
        )

        self.assertEqual(ri.amount_in_base_units, Decimal('450'))
        amount_in_cups = ri.get_amount_in_target_units(self.unit_cup)
        self.assertEqual(amount_in_cups, Decimal('3'))

    def test_ingredient_nutrition_calories_property(self):
        expected = (self.nutrition.protein * Decimal('4.0') +
                    self.nutrition.carbs * Decimal('4.0') +
                    self.nutrition.fat * Decimal('9.0'))
        self.assertEqual(self.nutrition.calories, expected)

    def test_ingredient_nutrition_clean_fails_non_base_unit(self):
        non_base_unit = Unit.objects.create(name='шт', is_base=False)
        with self.assertRaises(ValidationError) as cm:
            IngredientNutrition.objects.create(
                ingredient=self.ingredient,
                base_unit=non_base_unit,
                base_weight=100,
                protein=Decimal('2.0'),
                fat=Decimal('0.1'),
                carbs=Decimal('16.0'),
            )
        self.assertIn('не является базовой', str(cm.exception).lower())

    def test_recipe_ingredient_unique_constraint(self):
        recipe = Recipe.objects.create(title='Двойная запись', cook_time=10, servings=1)
        RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        with self.assertRaises(ValidationError) as cm:
            RecipeIngredient.objects.create(
                recipe=recipe,
                ingredient=self.ingredient,
                amount=Decimal('2.0'),
                unit=self.unit_cup 
            )
        self.assertIn('already exists', str(cm.exception).lower())

    def test_recipe_step_auto_number(self):
        from app.recipes.models import RecipeStep
        recipe = Recipe.objects.create(title='Шаги', cook_time=10, servings=1)
        step1 = RecipeStep.objects.create(recipe=recipe, description='Первый шаг')
        self.assertEqual(step1.step_number, 1)
        step2 = RecipeStep.objects.create(recipe=recipe, description='Второй шаг')
        self.assertEqual(step2.step_number, 2)
        step3 = RecipeStep.objects.create(recipe=recipe, description='Третий шаг')
        self.assertEqual(step3.step_number, 3)
        step2.delete()
        step3.refresh_from_db()
        self.assertEqual(step3.step_number, 2)
        step4 = RecipeStep.objects.create(recipe=recipe, description='Четвертый шаг')
        self.assertEqual(step4.step_number, 3)
    
    def test_recipe_ingredient_macros_caching(self):
        ri = RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )

        macros1 = ri.get_macros()
        macros2 = ri.get_macros()
        self.assertEqual(macros1, macros2)

        ri.amount = Decimal('2.0')
        macros3 = ri.get_macros()
        self.assertNotEqual(macros1, macros3)

        ri.unit = self.unit_cup
        macros4 = ri.get_macros()
        self.assertNotEqual(macros3, macros4)

    def test_recipe_ingredient_nutrition_caching(self):
        ri = RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        nutr1 = ri.nutrition
        nutr2 = ri.nutrition
        self.assertEqual(nutr1, nutr2)

        new_ingredient = Ingredient.objects.create(
            name='Морковь',
            category=self.category,
            can_be_added_to_cart=True,
        )
        IngredientNutrition.objects.create(
            ingredient=new_ingredient,
            base_unit=self.base_unit_g,
            base_weight=100,
            protein=Decimal('1.0'),
            fat=Decimal('0.2'),
            carbs=Decimal('8.0'),
        )

        ri.ingredient = new_ingredient
        nutr3 = ri.nutrition
        self.assertNotEqual(nutr1, nutr3)

    def test_recipe_ingredient_cache_after_save(self):
        recipe = Recipe.objects.create(title='Тест рецепта', cook_time=10, servings=2)
        ri = RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        # Вычисляем белок, кэш сохраняется
        total1 = recipe.total_proteins
        ri.amount = Decimal('2.0')
        ri.save()
        total2 = recipe.total_proteins

        self.assertNotEqual(total1, total2)

    def test_recipe_ingredient_cache_on_refresh_from_db(self):
        ri = RecipeIngredient.objects.create(
            recipe=self.recipe,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        macros1 = ri.get_macros()

        # Изменяем запись напрямую в БД
        RecipeIngredient.objects.filter(pk=ri.pk).update(amount=Decimal('2.0'))
        ri.refresh_from_db()

        macros2 = ri.get_macros()
        self.assertNotEqual(macros1, macros2)
