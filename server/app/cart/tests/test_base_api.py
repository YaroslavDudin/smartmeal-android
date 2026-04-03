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
            is_checked=False,
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
        self.assertEqual(item_data['is_checked'], cart_item.is_checked)

    def test_get_cart_items_filter_checked(self):
        CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1'),
            unit=self.unit_kg,
            is_checked=True,
        )
        CartItem.objects.create(
            user=self.user,
            ingredient=self.carrot,
            total_amount=Decimal('2'),
            unit=self.unit_kg,
            is_checked=False,
        )
        response = self.auth_client.get(self.url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        all_items = [item for group in response.data.values() for item in group]
        item_ids = [item['id'] for item in all_items]
        potato_item = CartItem.objects.get(ingredient=self.potato)
        carrot_item = CartItem.objects.get(ingredient=self.carrot)
        self.assertNotIn(potato_item.pk, item_ids)
        self.assertIn(carrot_item.pk, item_ids)

    def test_get_cart_items_show_checked_true(self):
        CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1'),
            unit=self.unit_kg,
            is_checked=True,
        )
        CartItem.objects.create(
            user=self.user,
            ingredient=self.carrot,
            total_amount=Decimal('2'),
            unit=self.unit_kg,
            is_checked=False,
        )
        response = self.auth_client.get(f'{self.url}?show_checked=true')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('Овощи', response.data)
        self.assertEqual(len(response.data['Овощи']), 2)

    def test_get_cart_items_unauthenticated(self):
        response = self.api_client.get(self.url)
        self.assertEqual(response.status_code, 401)

    def test_create_cart_item_success(self):
        data = {
            'ingredient': self.potato.id,
            'total_amount': '1500',
            'unit': self.unit_g.pk,
            'is_checked': False,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(CartItem.objects.count(), 1)
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertEqual(cart_item.user, self.user)
        self.assertEqual(cart_item.ingredient, self.potato)
        self.assertEqual(cart_item.total_amount, Decimal('1500'))
        self.assertEqual(cart_item.unit, self.unit_g)
        self.assertFalse(cart_item.is_checked)

    def test_create_cart_item_default_is_checked(self):
        data = {
            'ingredient': self.potato.id,
            'total_amount': '1.5',
            'unit': self.unit_kg.id,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        cart_item = CartItem.objects.get(pk=response.data['id'])
        self.assertFalse(cart_item.is_checked)

    def test_create_cart_item_with_ingredient_not_addable_to_cart(self):
        self.potato.can_be_added_to_cart = False
        self.potato.save()
        data = {
            'ingredient': self.potato.id,
            'total_amount': '1.5',
            'unit': self.unit_g.id,
            'is_checked': False,
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
            is_checked=True,
        )
        data = {
            'ingredient': self.potato.id,
            'total_amount': 'status.HTTP_200_OK0',
            'unit': self.unit_g.id,
            'is_checked': True,
        }
        response = self.auth_client.post(self.url, data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_patch_cart_item_amount(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
            is_checked=True,
        )
        response = self.auth_client.patch(
            f'{self.url}{cart_item.pk}/',
            {'total_amount': '6000'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        cart_item.refresh_from_db()
        self.assertEqual(cart_item.total_amount, Decimal('6000'))

    def test_patch_cart_item_is_checked(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
            is_checked=False,
        )
        response = self.auth_client.patch(
            f'{self.url}{cart_item.pk}/',
            {'is_checked': True},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        cart_item.refresh_from_db()
        self.assertTrue(cart_item.is_checked)

    def test_patch_cart_item_of_another_user(self):
        cart_item = CartItem.objects.create(
            user=self.other_user,
            ingredient=self.potato,
            total_amount=Decimal('1'),
            unit=self.unit_kg,
            is_checked=False,
        )
        response = self.auth_client.patch(
            f'{self.url}{cart_item.pk}/',
            {'is_checked': True},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_delete_cart_item(self):
        cart_item = CartItem.objects.create(
            user=self.user,
            ingredient=self.potato,
            total_amount=Decimal('1000'),
            unit=self.unit_g,
            is_checked=True,
        )
        self.assertEqual(CartItem.objects.count(), 1)
        response = self.auth_client.delete(f'{self.url}{cart_item.pk}/')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(CartItem.objects.filter(pk=cart_item.pk).exists())
