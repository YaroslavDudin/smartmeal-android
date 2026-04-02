from datetime import date

from django.contrib.auth import get_user_model
from django.core.exceptions import ValidationError
from django.test import TestCase
from rest_framework.test import APITestCase
from rest_framework import status

from app.menus.models import MealType, Menu, MenuItem, Period
from app.menus.serializers import GenerateMenuSerializer
from app.recipes.models import Recipe

User = get_user_model()


class MenusModelTests(TestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="diet_user",
            email="diet@test.com",
            password="password123",
        )
        self.recipe = Recipe.objects.create(
            title="Oatmeal with berries",
            cook_time=15,
            servings=1,
        )
        self.menu = Menu.objects.create(
            user=self.user,
            period=Period.WEEK,
            start_date=date.today(),
        )
        self.meal_type = MealType.objects.create(name="Breakfast", order=1)

    def test_create_menu(self):
        self.assertEqual(self.menu.period, Period.WEEK)
        self.assertEqual(self.menu.user.email, "diet@test.com")

    def test_create_menu_item(self):
        menu_item = MenuItem.objects.create(
            menu=self.menu,
            recipe=self.recipe,
            day_offset=0,
            meal_type=self.meal_type,
        )

        self.assertEqual(menu_item.meal_type.name, "Breakfast")
        self.assertEqual(menu_item.recipe.title, "Oatmeal with berries")
        self.assertEqual(menu_item.menu.period, Period.WEEK)

    def test_menu_item_allows_day_offset_255(self):
        menu_item = MenuItem(
            menu=self.menu,
            recipe=self.recipe,
            day_offset=255,
            meal_type=self.meal_type,
        )

        menu_item.full_clean()

    def test_menu_item_rejects_day_offset_256(self):
        menu_item = MenuItem(
            menu=self.menu,
            recipe=self.recipe,
            day_offset=256,
            meal_type=self.meal_type,
        )

        with self.assertRaises(ValidationError):
            menu_item.full_clean()


class GenerateMenuSerializerTests(TestCase):
    def test_generate_menu_serializer_allows_256_days(self):
        serializer = GenerateMenuSerializer(
            data={
                "period": "custom",
                "days": 256,
                "start_date": date.today(),
            }
        )

        self.assertTrue(serializer.is_valid(), serializer.errors)

    def test_generate_menu_serializer_rejects_257_days(self):
        serializer = GenerateMenuSerializer(
            data={
                "period": "custom",
                "days": 257,
                "start_date": date.today(),
            }
        )

        self.assertFalse(serializer.is_valid())
        self.assertIn("days", serializer.errors)

    def test_generate_menu_serializer_allows_cook_times_dict(self):
        serializer = GenerateMenuSerializer(
            data={
                "period": "custom",
                "days": 1,
                "start_date": date.today(),
                "cook_times": {
                    "breakfast": "short",
                    "lunch": "medium",
                    "dinner": "long"
                }
            }
        )
        self.assertTrue(serializer.is_valid(), serializer.errors) 
        self.assertEqual(serializer.validated_data["cook_times"]["breakfast"], "short") 


class MenuGenerationAPITests(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username="testuser",
            email="test@test.com",
            password="password123"
        )
        self.client.force_authenticate(user=self.user)

        self.mt_breakfast = MealType.objects.create(name="breakfast", order=1)
        self.mt_lunch = MealType.objects.create(name="lunch", order=2)
        self.mt_dinner = MealType.objects.create(name="dinner", order=3)

        self.recipe_short = Recipe.objects.create(title="Short Recipe", cook_time=15, servings=1)
        self.recipe_medium = Recipe.objects.create(title="Medium Recipe", cook_time=45, servings=1)
        self.recipe_long = Recipe.objects.create(title="Long Recipe", cook_time=90, servings=1)

        for recipe in [self.recipe_short, self.recipe_medium, self.recipe_long]:
            recipe.meal_types.add(self.mt_breakfast, self.mt_lunch, self.mt_dinner)

        self.url = '/api/menus/generate/'

    def test_generate_menu_with_detailed_cook_times(self):
        data = {
            "period": "custom",
            "days": 1,
            "start_date": date.today(),
            "cook_times": {
                "breakfast": "short",
                "lunch": "medium",
                "dinner": "long"
            }
        }
        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        menu_id = response.data['id']
        items = MenuItem.objects.filter(menu_id=menu_id)

        breakfast_item = items.get(meal_type=self.mt_breakfast)
        self.assertEqual(breakfast_item.recipe, self.recipe_short)
        self.assertEqual(breakfast_item.requested_cook_time, "short")

        lunch_item = items.get(meal_type=self.mt_lunch)
        self.assertEqual(lunch_item.recipe, self.recipe_medium)
        self.assertEqual(lunch_item.requested_cook_time, "medium")

        dinner_item = items.get(meal_type=self.mt_dinner)
        self.assertEqual(dinner_item.recipe, self.recipe_long)
        self.assertEqual(dinner_item.requested_cook_time, "long")

    def test_generate_weekly_menu_with_detailed_cook_times(self):
        data = {
            "period": "week",
            "start_date": date.today(),
            "cook_times": {
                "breakfast": "short",
                "lunch": "medium",
                "dinner": "long"
            }
        }
        
        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        menu_id = response.data['id']
        items = MenuItem.objects.filter(menu_id=menu_id)
        
        self.assertEqual(items.count(), 21)

        for item in items:
            if item.meal_type == self.mt_breakfast:
                self.assertEqual(item.requested_cook_time, "short")
                self.assertTrue(item.recipe.cook_time <= 30, f"Завтрак слишком долгий: {item.recipe.cook_time} мин")
            
            elif item.meal_type == self.mt_lunch:
                self.assertEqual(item.requested_cook_time, "medium")
                self.assertTrue(30 < item.recipe.cook_time < 60, f"Обед не вписывается в medium: {item.recipe.cook_time} мин")
            
            elif item.meal_type == self.mt_dinner:
                self.assertEqual(item.requested_cook_time, "long")
                self.assertTrue(item.recipe.cook_time >= 60, f"Ужин слишком быстрый: {item.recipe.cook_time} мин")

    def test_generate_menu_fallback_to_global_cook_time(self):
        data = {
            "period": "custom",
            "days": 1,
            "start_date": date.today(),
            "cook_time_range": "short"
        }
        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        menu_id = response.data['id']
        items = MenuItem.objects.filter(menu_id=menu_id)
        
        for item in items:
            self.assertEqual(item.recipe, self.recipe_short)

    def test_adjust_existing_menu_cook_times(self):
        menu = Menu.objects.create(user=self.user, period="custom", start_date=date.today())
        
        item_breakfast = MenuItem.objects.create(
            menu=menu, recipe=self.recipe_long, meal_type=self.mt_breakfast, day_offset=0, requested_cook_time="any"
        )
        item_lunch = MenuItem.objects.create(
            menu=menu, recipe=self.recipe_short, meal_type=self.mt_lunch, day_offset=1, requested_cook_time="any"
        )

        data = {
            "cook_times": {
                "breakfast": "short",
                "lunch": "short"
            }
        }
        
        url = f'/api/menus/{menu.id}/adjust/'
        response = self.client.post(url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)


        item_breakfast.refresh_from_db()
        item_lunch.refresh_from_db()

        self.assertEqual(item_breakfast.requested_cook_time, "short")
        self.assertEqual(item_breakfast.recipe, self.recipe_short)

        self.assertEqual(item_lunch.requested_cook_time, "short")
        self.assertEqual(item_lunch.recipe, self.recipe_short)