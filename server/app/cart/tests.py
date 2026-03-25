from decimal import Decimal
from django.test import TestCase
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework import status
from app.cart.models import CartItem
from app.recipes.models import Ingredient, Unit, IngredientCategory, Recipe, RecipeIngredient
from app.menus.models import Menu, MenuItem, MealType

User = get_user_model()

class CartItemModelTests(TestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123'
        )
        
        self.unit = Unit.objects.create(name='кг')
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель', 
            category=self.category
        )

    def test_create_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=2.50,
            unit=self.unit,
            is_checked=False
        )

        self.assertEqual(cart_item.total_amount, 2.50)
        self.assertFalse(cart_item.is_checked)
        self.assertEqual(cart_item.user.email, 'buyer@test.com')
        self.assertEqual(cart_item.ingredient.name, 'Картофель')
        
        expected_str = f'Ingredient ID {self.ingredient.id} (2.5 Unit ID {self.unit.id}) for User ID {self.user.id}'
        self.assertEqual(str(cart_item), expected_str)


class CartItemCreateAPITest(APITestCase):

    def setUp(self):
        self.user = User.objects.create_user(
            username='test_buyer',
            email='buyer@test.com',
            password='password123',
        )
        
        self.unit = Unit.objects.create(name='кг')
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель', 
            category=self.category,
        )

        self.client.force_authenticate(user=self.user)
        self.base_url = '/api/cart/'

    def test_get_cart_items_grouped(self):
        response_empty = self.client.get(self.base_url)
        self.assertEqual(response_empty.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response_empty.data), 0)
        
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1.0,
            unit=self.unit,
            is_checked=True,
        )
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        item_data = response.data['Овощи'][0]
        self.assertEqual(item_data['id'], cart_item.pk)
        self.assertEqual(item_data['ingredient_name'], cart_item.ingredient.name)
        self.assertEqual(Decimal(item_data['total_amount']), cart_item.total_amount)
        self.assertEqual(item_data['unit_name'], cart_item.unit.name)
        self.assertEqual(item_data['is_checked'], cart_item.is_checked)

    def test_get_cart_items_filter_checked(self):
        ingredient2 = Ingredient.objects.create(name='Морковь', category=self.category)
        CartItem.objects.create(user=self.user, ingredient=self.ingredient, total_amount=1.0, unit=self.unit, is_checked=True)
        unchecked_item = CartItem.objects.create(user=self.user, ingredient=ingredient2, total_amount=2.0, unit=self.unit, is_checked=False)

        response = self.client.get(f'{self.base_url}?show_checked=false')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        self.assertEqual(response.data['Овощи'][0]['id'], unchecked_item.pk)

    def test_get_cart_items_unauthenticated(self):
        self.client.force_authenticate(user=None)

        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_cart_item_success(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
            'is_checked': False,
        }

        response = self.client.post(self.base_url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)

        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertEqual(cart_item.user, self.user)
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, 1.5)
        self.assertEqual(cart_item.unit, self.unit)
        self.assertFalse(cart_item.is_checked)
    
    def test_create_cart_item_default_is_checked(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
        }
        
        response = self.client.post(self.base_url, data, format='json')
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertFalse(cart_item.is_checked)

    def test_create_cart_item_missing_ingredient(self):
        data = {
            'total_amount': 1.0,
            'unit': self.unit.id,
            'is_checked': True,
        }
        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('ingredient', response.data)

    def test_create_cart_item_duplicate(self):
        CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1.0,
            unit=self.unit,
            is_checked=True,
        )
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 2.0,
            'unit': self.unit.id,
            'is_checked': True,
        }

        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
    
    def test_patch_cart_item_amount(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1.0,
            unit=self.unit,
            is_checked=True,
        )

        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.patch(cart_item_url, {'total_amount': 6.0}, format='json')
        cart_item.refresh_from_db()

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(cart_item.total_amount, 6.0)
        
    def test_patch_cart_item_is_checked(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1.0,
            unit=self.unit,
            is_checked=False,
        )

        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.patch(cart_item_url, {'is_checked': True}, format='json')
        cart_item.refresh_from_db()
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(cart_item.is_checked)
        
    def test_delete_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            total_amount=1.0,
            unit=self.unit,
            is_checked=True,
        )
        self.assertEqual(CartItem.objects.count(), 1)
        
        cart_item_url = f'{self.base_url}{cart_item.pk}/'
        response = self.client.delete(cart_item_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(CartItem.objects.filter(pk=cart_item.pk).exists())

    def test_cart_recalculate_creates_grouped_list(self):
        recipe = Recipe.objects.create(title='Суп', cook_time=30, servings=2)

        RecipeIngredient.objects.create(
            recipe=recipe,
            ingredient=self.ingredient,
            amount = 2.0,
            unit=self.unit
        )
        menu = Menu.objects.create(
            user=self.user, 
            period='week', 
            start_date='2026-03-25'
        )
        lunch_type, created = MealType.objects.get_or_create(name='lunch', defaults={'order': 1})

        MenuItem.objects.create(
            menu=menu, 
            recipe=recipe, 
            day_offset=0, 
            meal_type=lunch_type
        )
        CartItem.objects.all().delete()

        recalculate_url = f'{self.base_url}recalculate/'
        response = self.client.post(recalculate_url)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.first()
        self.assertEqual(cart_item.ingredient, self.ingredient)
        self.assertEqual(cart_item.total_amount, 2.0)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        self.assertEqual(response.data['Овощи'][0]['ingredient_name'], 'Картофель')
        
