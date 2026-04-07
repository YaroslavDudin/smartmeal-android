from datetime import date

from django.contrib.auth import get_user_model
from django.test import TestCase
from rest_framework.test import APIClient

from app.recipes.models import (
    Unit, IngredientCategory, Ingredient, IngredientNutrition,
    UnitConversion, Recipe
)
from app.menus.models import Period, MealType, Menu

User = get_user_model()


class Setup(TestCase):
    """
    Базовый класс для всех тестов меню.
    В setUp создаются все необходимые объекты, доступные через self.
    """

    def setUp(self):
        # Клиенты API
        self.api_client = APIClient()

        # Пользователи (уникальные имена и email)
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123'
        )
        self.other_user = User.objects.create_user(
            username='other_buyer',
            email='other@test.com',
            password='password123'
        )

        # Аутентифицированные клиенты
        self.auth_client = APIClient()
        self.auth_client.force_authenticate(user=self.user)
        self.other_auth_client = APIClient()
        self.other_auth_client.force_authenticate(user=self.other_user)

        # Единицы измерения
        self.unit_g = Unit.objects.create(name='г', is_base=True)
        self.unit_ml = Unit.objects.create(name='мл', is_base=True)
        self.unit_kg = Unit.objects.create(name='кг')
        self.unit_item = Unit.objects.create(name='шт')

        # Категории ингредиентов
        self.cat_vegetables = IngredientCategory.objects.create(name='Овощи')
        self.cat_dairy = IngredientCategory.objects.create(name='Молочные продукты')

        # Ингредиенты
        self.potato = self._create_potato()
        self.carrot = self._create_carrot()
        self.milk = self._create_milk()

        # Тип приёма пищи
        self.lunch_type = MealType.objects.create(name='lunch', order=1)
        self.dinner_type = MealType.objects.create(name='dinner', order=2)

        # Рецепты
        self.soup_recipe = Recipe.objects.create(title='Суп', cook_time=30, servings=2)
        self.salad_recipe = Recipe.objects.create(title='Салат', cook_time=20, servings=4)

        # Меню
        today = date.today()
        self.menu_week = Menu.objects.create(
            user=self.user, period=Period.WEEK, start_date=today
        )
        self.menu_day = Menu.objects.create(
            user=self.user, period=Period.DAY, start_date=today
        )
        self.menu_custom = Menu.objects.create(
            user=self.user, period=Period.CUSTOM, start_date=today
        )

    # Вспомогательные методы для создания ингредиентов (сохраняют зависимости)
    def _create_potato(self):
        ingredient = Ingredient.objects.create(
            name='Картофель',
            category=self.cat_vegetables,
            can_be_added_to_cart=True
        )
        IngredientNutrition.objects.create(
            ingredient=ingredient,
            base_unit=self.unit_g,
            base_weight=100,
            protein=2,
            fat=0.1,
            carbs=16
        )
        UnitConversion.objects.create(
            ingredient=ingredient,
            from_unit=self.unit_kg,
            to_unit=self.unit_g,
            amount_per_unit=1000
        )
        return ingredient

    def _create_carrot(self):
        ingredient = Ingredient.objects.create(
            name='Морковь',
            category=self.cat_vegetables,
            can_be_added_to_cart=True
        )
        IngredientNutrition.objects.create(
            ingredient=ingredient,
            base_unit=self.unit_g,
            base_weight=100,
            protein=0.9,
            fat=0.2,
            carbs=6.8
        )
        UnitConversion.objects.create(
            ingredient=ingredient,
            from_unit=self.unit_item,
            to_unit=self.unit_g,
            amount_per_unit=150
        )
        return ingredient

    def _create_milk(self):
        ingredient = Ingredient.objects.create(
            name='Молоко',
            category=self.cat_dairy,
            can_be_added_to_cart=True
        )
        IngredientNutrition.objects.create(
            ingredient=ingredient,
            base_unit=self.unit_ml,
            base_weight=100,
            protein=3.5,
            fat=3.2,
            carbs=4.4
        )
        UnitConversion.objects.create(
            ingredient=ingredient,
            from_unit=self.unit_ml,
            to_unit=self.unit_g,
            amount_per_unit=1
        )
        return ingredient
