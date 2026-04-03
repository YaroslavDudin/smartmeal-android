# tests/test_recalculate.py
from decimal import Decimal
from datetime import date, timedelta
from rest_framework import status
from rest_framework.test import APITestCase

from app.cart.models import CartItem
from app.menus.models import MenuItem
from app.recipes.models import RecipeIngredient
from .setup import Setup


class TestRecalculate(APITestCase, Setup):
    url = '/api/cart/recalculate/'

    def add_recipe_ingredient(self, recipe, ingredient, unit, amount):
        return RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=ingredient,
            unit=unit,
            amount=amount,
        )

    def add_menu_item(self, menu, recipe, day_offset=0, meal_type=None):
        return MenuItem.objects.create(
            menu=menu,
            recipe=recipe,
            day_offset=day_offset,
            meal_type=meal_type,
        )

    def get_cart_item(self, user, ingredient):
        return CartItem.objects.get(user=user, ingredient=ingredient)

    # Тесты
    def test_recalculate_requires_auth(self):
        response = self.api_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_no_active_menu_returns_404(self):
        response = self.auth_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn('нет активного меню', response.data['detail'])

    def test_future_menu_not_active(self):
        self.menu_week.start_date = date.today() + timedelta(days=1)
        self.menu_week.save()

        response = self.auth_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_expired_menu_not_active(self):
        self.menu_week.start_date = date.today() - timedelta(days=10)
        self.menu_week.save()

        response = self.auth_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_active_menu_detected_automatically(self):
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 300)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        response = self.auth_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('300'))

    def test_explicit_menu_id_works(self):
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 500)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        response = self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(self.get_cart_item(self.user, self.potato).total_amount, Decimal('500'))

    def test_wrong_menu_id_returns_404(self):
        response = self.auth_client.post(self.url, {'menu_id': 99999}, format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_day_offset_filters_ingredients(self):
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_recipe_ingredient(self.salad_recipe, self.potato, self.unit_g, 300)

        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.add_menu_item(self.menu_week, self.salad_recipe, day_offset=1, meal_type=self.lunch_type)

        response = self.auth_client.post(
            self.url, {'menu_id': self.menu_week.pk, 'day_offset': 0}, format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(self.get_cart_item(self.user, self.potato).total_amount, Decimal('200'))

    def test_invalid_day_offset_returns_404(self):
        response = self.auth_client.post(
            self.url, {'menu_id': self.menu_week.pk, 'day_offset': 99}, format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_new_cart_item_created(self):
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 400)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('400'))
        self.assertEqual(item.unit, self.unit_g)

    def test_same_ingredient_in_two_recipes_accumulates(self):
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 300)
        self.add_recipe_ingredient(self.salad_recipe, self.potato, self.unit_g, 200)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.dinner_type)
        self.add_menu_item(self.menu_week, self.salad_recipe, day_offset=0, meal_type=self.lunch_type)

        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('500'))

    def test_existing_cart_item_amount_added(self):
        CartItem.objects.create(
            user=self.user, ingredient=self.potato,
            total_amount=Decimal('100'), unit=self.unit_g, is_checked=False
        )
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('300'))

    def test_checked_cart_item_reset_on_recalculate(self):
        CartItem.objects.create(
            user=self.user, ingredient=self.potato,
            total_amount=Decimal('50'), unit=self.unit_g, is_checked=True
        )
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 300)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertFalse(item.is_checked)
        self.assertEqual(item.total_amount, Decimal('300'))

    def test_ingredient_not_added_to_cart_when_flag_false(self):
        self.potato.can_be_added_to_cart = False
        self.potato.save()
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())

    def test_unit_conversion_applied_when_units_differ(self):
        CartItem.objects.create(
            user=self.user, ingredient=self.potato,
            total_amount=Decimal('500'), unit=self.unit_g, is_checked=False
        )
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_kg, 1)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)

        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('1500'))
