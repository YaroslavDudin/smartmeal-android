from decimal import Decimal
from rest_framework.test import APITestCase
from rest_framework import status
from django.contrib.auth import get_user_model
from app.recipes.models import Ingredient, Unit, IngredientCategory, IngredientNutrition, UnitConversion
from app.accounts.models import UserStock


User = get_user_model()


class UserStockAPITestCase(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username='stock_user',
            email='stock@test.com',
            password='testpass123'
        )
        self.other_user = User.objects.create_user(
            username='other_user',
            email='other@test.com',
            password='testpass123'
        )
        self.unit_g = Unit.objects.create(name='г', is_base=True)
        self.unit_kg = Unit.objects.create(name='кг')
        self.category = IngredientCategory.objects.create(name='Овощи')
        self.ingredient = Ingredient.objects.create(
            name='Картофель',
            category=self.category,
            can_be_added_to_cart=True
        )
        IngredientNutrition.objects.create(
            ingredient=self.ingredient,
            base_unit=self.unit_g,
            base_weight=100,
            protein=Decimal('2.0'),
            fat=Decimal('0.1'),
            carbs=Decimal('16.0')
        )
        UnitConversion.objects.create(
            ingredient=self.ingredient,
            from_unit=self.unit_kg,
            to_unit=self.unit_g,
            amount_per_unit=1000
        )

        self.client.force_authenticate(user=self.user)
        self.base_url = '/api/accounts/stock/'

    def test_get_all_stock_empty(self):
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data, [])

    def test_get_all_stock_with_items(self):
        stock_item = UserStock.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            amount=Decimal('2.5'),
            unit=self.unit_kg
        )
        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['id'], stock_item.id)
        self.assertEqual(response.data[0]['ingredient'], self.ingredient.id)
        self.assertEqual(response.data[0]['ingredient_name'], self.ingredient.name)
        self.assertEqual(Decimal(response.data[0]['amount']), stock_item.amount)
        self.assertEqual(response.data[0]['unit'], self.unit_kg.id)
        self.assertEqual(response.data[0]['unit_name'], self.unit_kg.name)

    def test_create_stock_item(self):
        data = {
            'ingredient': self.ingredient.id,
            'amount': '1.5',
            'unit': self.unit_kg.id
        }
        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(UserStock.objects.count(), 1)
        stock = UserStock.objects.get(pk=response.data['id'])
        self.assertEqual(stock.user, self.user)
        self.assertEqual(stock.ingredient, self.ingredient)
        self.assertEqual(stock.amount, Decimal('1.5'))
        self.assertEqual(stock.unit, self.unit_kg)

    def test_delete_stock_item(self):
        stock = UserStock.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            amount=Decimal('1'),
            unit=self.unit_kg
        )
        self.assertEqual(UserStock.objects.count(), 1)
        url = f'{self.base_url}{stock.id}/'
        response = self.client.delete(url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(UserStock.objects.filter(id=stock.id).exists())

    def test_create_stock_unauthenticated(self):
        self.client.force_authenticate(user=None)
        data = {
            'ingredient': self.ingredient.id,
            'amount': '1.5',
            'unit': self.unit_kg.id
        }
        response = self.client.post(self.base_url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertEqual(UserStock.objects.count(), 0)

    def test_delete_stock_not_found(self):
        url = f'{self.base_url}9999/'
        response = self.client.delete(url)
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_update_stock_item(self):
        stock = UserStock.objects.create(
            user=self.user,
            ingredient=self.ingredient,
            amount=Decimal('1.0'),
            unit=self.unit_kg
        )
        url = f'{self.base_url}{stock.id}/'
        data = {'amount': '2.0'}
        response = self.client.patch(url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        stock.refresh_from_db()
        self.assertEqual(stock.amount, Decimal('2.0'))

    def test_cannot_access_other_user_stock(self):
        other_stock = UserStock.objects.create(
            user=self.other_user,
            ingredient=self.ingredient,
            amount=Decimal('5'),
            unit=self.unit_kg
        )
        url = f'{self.base_url}{other_stock.id}/'
        response = self.client.get(url)
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_delete = self.client.delete(url)
        self.assertEqual(response_delete.status_code, status.HTTP_404_NOT_FOUND)
