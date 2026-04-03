from decimal import Decimal
from rest_framework import status
from rest_framework.test import APITestCase

from app.cart.models import CartItem
from .setup import Setup


class TestCartApi(APITestCase, Setup):
    url = '/api/cart/'

    def test_get_cart_items_empty(self):
        response = self.auth_client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data, {})

    def test_get_cart_items_grouped(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
        )
        response = self.auth_client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 1)
        item_data = response.data['Овощи'][0]
        self.assertEqual(item_data['id'], cart_item.pk)
        self.assertEqual(item_data['ingredient_name'], self.potato.name)
        self.assertEqual(Decimal(item_data['total_amount']), cart_item.total_amount)
        self.assertEqual(item_data['unit_name'], self.unit_g.name)

    def test_get_cart_items_unauthenticated(self):
        response = self.api_client.get(self.url)
        self.assertEqual(response.status_code, 401)

    def test_create_cart_item_success(self):
        data = {
            'ingredient': self.potato.id,
            'total_amount': '1500',
            'unit': self.unit_g.pk,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertEqual(cart_item.user, self.user)
        self.assertEqual(cart_item.ingredient, self.potato)
        self.assertEqual(cart_item.total_amount, Decimal('1500'))
        self.assertEqual(cart_item.unit, self.unit_g)

    def test_create_cart_item_with_ingredient_not_addable_to_cart(self):
        self.potato.can_be_added_to_cart = False
        self.potato.save()
        data = {
            'ingredient': self.potato.id,
            'total_amount': '1.5',
            'unit': self.unit_g.id,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('can_be_added_to_cart', response.data)
        self.assertEqual(CartItem.objects.count(), 0)

    def test_create_cart_item_duplicate(self):
        CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
        )
        data = {
            'ingredient': self.potato.id,
            'total_amount': '200',
            'unit': self.unit_g.id,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_patch_cart_item_amount(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
        )
        response = self.auth_client.patch(
            f'{self.url}{cart_item.pk}/',
            {'total_amount': '6000'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        cart_item.refresh_from_db()
        self.assertEqual(cart_item.total_amount, Decimal('6000'))

    def test_patch_cart_item_of_another_user(self):
        cart_item = CartItem.objects.create(
            user=self.other_user,
            ingredient=self.potato,
            total_amount=Decimal('1'),
            unit=self.unit_kg,
        )
        response = self.auth_client.patch(
            f'{self.url}{cart_item.pk}/',
            {'total_amount': '2'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_delete_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
        )
        self.assertEqual(CartItem.objects.count(), 1)
        response = self.auth_client.delete(f'{self.url}{cart_item.pk}/')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(CartItem.objects.filter(pk=cart_item.pk).exists())
