from django.test import TestCase
from django.contrib.auth import get_user_model
from rest_framework.test import APITestCase
from rest_framework import status
from app.cart.models import CartItem
from app.recipes.models import Ingredient, Unit, IngredientCategory

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
        self.url = '/api/cart/'

    def test_create_cart_item_success(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
            'is_checked': False,
        }

        response = self.client.post(self.url, data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)

        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertTrue(isinstance(cart_item, CartItem))
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
        
        response = self.client.post(self.url, data, format='json')
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertTrue(cart_item.is_checked)

    def test_create_cart_item_unauthenticated(self):
        data = {
            'ingredient': self.ingredient.id,
            'total_amount': 1.5,
            'unit': self.unit.id,
            'is_checked': False,
        }
        self.client.force_authenticate(user=None)

        response = self.client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_create_cart_item_invalid_data(self):
        data = {
            'total_amount': 1.0,
            'unit': self.unit.id,
            'is_checked': True,
        }
        response = self.client.post(self.url, data, format='json')
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

        response = self.client.post(self.url, data, format='json')
        self.assertIn(response.status_code, [status.HTTP_200_OK, status.HTTP_201_CREATED])
        self.assertEqual(CartItem.objects.count(), 1)
        
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertEqual(cart_item.total_amount, 3.0)
