from decimal import Decimal
from datetime import date, timedelta
from rest_framework import status
from rest_framework.test import APITestCase

from app.cart.models import CartItem
from app.menus.models import MenuItem
from app.recipes.models import RecipeIngredient
from app.accounts.models import UserStock
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

    def create_user_stock(self, user, ingredient, amount, unit):
        return UserStock.objects.create(
            user=user,
            ingredient=ingredient,
            amount=amount,
            unit=unit,
        )

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

    def test_ingredient_not_added_to_cart_when_flag_false(self):
        self.potato.can_be_added_to_cart = False
        self.potato.save()
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())

    def test_existing_cart_items_are_deleted(self):
        CartItem.objects.create(
            user=self.user, ingredient=self.potato,
            total_amount=Decimal('100'), unit=self.unit_g
        )
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_menu_item(self.menu_day, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_day.pk}, format='json')
        items = CartItem.objects.filter(user=self.user, ingredient=self.potato)
        self.assertEqual(items.count(), 1)
        self.assertEqual(items.first().total_amount, Decimal('200'))

    def test_unit_conversion_uses_base_unit(self):
        CartItem.objects.create(
            user=self.user, ingredient=self.potato,
            total_amount=Decimal('500'), unit=self.unit_g
        )
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_kg, 1)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        self.assertEqual(item.total_amount, Decimal('1000'))
        self.assertEqual(item.unit, self.unit_g)

    def test_recalculate_uses_user_stock_to_reduce_amount(self):
        # Создаём запас: 200 г картофеля
        self.create_user_stock(self.user, self.potato, Decimal('200'), self.unit_g)
        # В рецепте нужно 500 г
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 500)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        # Ожидаем: 500 - 200 = 300 г
        self.assertEqual(item.total_amount, Decimal('300'))

    def test_recalculate_stock_with_different_unit(self):
        # Запас: 1 кг картофеля (единица кг, нужно конвертировать в граммы)
        self.create_user_stock(self.user, self.potato, Decimal('1'), self.unit_kg)
        # В рецепте нужно 1500 г
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 1500)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        # 1 кг = 1000 г, остаётся 1500 - 1000 = 500 г
        self.assertEqual(item.total_amount, Decimal('500'))

    def test_recalculate_stock_fully_covers_need(self):
        # Запас: 800 г картофеля
        self.create_user_stock(self.user, self.potato, Decimal('800'), self.unit_g)
        # Нужно 600 г
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 600)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        # Корзина не должна содержать картофель
        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())

    def test_recalculate_stock_partial_coverage_accumulated(self):
        # Запас: 100 г картофеля
        self.create_user_stock(self.user, self.potato, Decimal('100'), self.unit_g)
        # Два рецепта: один требует 200 г, другой 300 г
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 200)
        self.add_recipe_ingredient(self.salad_recipe, self.potato, self.unit_g, 300)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.add_menu_item(self.menu_week, self.salad_recipe, day_offset=1, meal_type=self.dinner_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.potato)
        # Всего нужно 500 г, вычитаем 100 г = 400 г
        self.assertEqual(item.total_amount, Decimal('400'))

    def test_recalculate_stock_with_different_ingredient_not_affected(self):
        # Запас для картофеля, но рецепт использует морковь – не должно влиять
        self.create_user_stock(self.user, self.potato, Decimal('500'), self.unit_g)
        self.add_recipe_ingredient(self.soup_recipe, self.carrot, self.unit_g, 300)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        item = self.get_cart_item(self.user, self.carrot)
        self.assertEqual(item.total_amount, Decimal('300'))
        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())

    def test_recalculate_stock_with_zero_amount_after_conversion(self):
        # Запас: 0.5 кг картофеля (500 г)
        self.create_user_stock(self.user, self.potato, Decimal('0.5'), self.unit_kg)
        # Нужно 500 г
        self.add_recipe_ingredient(self.soup_recipe, self.potato, self.unit_g, 500)
        self.add_menu_item(self.menu_week, self.soup_recipe, day_offset=0, meal_type=self.lunch_type)
        self.auth_client.post(self.url, {'menu_id': self.menu_week.pk}, format='json')
        self.assertFalse(CartItem.objects.filter(user=self.user, ingredient=self.potato).exists())
