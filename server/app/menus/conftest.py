import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient

from app.menus.models import MealType
from app.recipes.models import Recipe


User = get_user_model()


@pytest.fixture
def auth_client(db):
    user = User.objects.create_user(
        username='menu-user',
        email='menu-user@example.com',
        password='password123',
    )
    client = APIClient()
    client.force_authenticate(user=user)
    client.user = user
    return client


@pytest.fixture
def meal_type_factory(db):
    def create_meal_type(**kwargs):
        defaults = {
            'name': 'breakfast',
            'order': 0,
        }
        defaults.update(kwargs)
        return MealType.objects.create(**defaults)

    return create_meal_type


@pytest.fixture
def recipe_factory(db):
    def create_recipe(**kwargs):
        meal_type = kwargs.pop('meal_type', None)
        defaults = {
            'title': 'Test recipe',
            'cook_time': 30,
            'servings': 1,
            'per_serving_calories': 500,
            'per_serving_proteins': 25,
            'per_serving_fats': 15,
            'per_serving_carbs': 55,
        }
        defaults.update(kwargs)
        recipe = Recipe.objects.create(**defaults)
        if meal_type is not None:
            recipe.meal_types.add(meal_type)
        return recipe

    return create_recipe
