from decimal import Decimal
from rest_framework.test import APITestCase
from rest_framework import status

from app.cart.models import CartItem
from .setup import Setup


class TestExport(APITestCase, Setup):
    url = '/api/cart/export/'

    def make_cart_item(self, user, ingredient, unit, amount):
        return CartItem.objects.create(
            user=user,
            ingredient=ingredient,
            unit=unit,
            total_amount=Decimal(str(amount)),
        )

    def test_export_requires_auth(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        response = self.api_client.post(f'{self.url}?all=true', {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_export_without_all_or_ids_returns_400(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        response = self.auth_client.post(self.url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('cart_items_ids', response.data['detail'])
        self.assertIn('all', response.data['detail'])

    def test_export_with_empty_ids_and_no_all_returns_400(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        response = self.auth_client.post(self.url, {'cart_items_ids': []}, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_export_all_returns_plain_text(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 500)
        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('text/plain', response.get('Content-Type', ''))

    def test_export_all_contains_ingredient_and_category(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 500)
        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        text = response.content.decode()
        self.assertIn(self.cat_vegetables.name, text)
        self.assertIn(self.potato.name, text)
        self.assertIn('500', text)
        self.assertIn(self.unit_g.name, text)

    def test_export_by_ids_only_exports_selected(self):
        potato_item = self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        self.make_cart_item(self.user, self.carrot, self.unit_g, 150)

        response = self.auth_client.post(self.url, {'cart_items_ids': [potato_item.pk]}, format='json')
        text = response.content.decode()
        self.assertIn(self.potato.name, text)
        self.assertNotIn(self.carrot.name, text)

    def test_export_by_ids_with_missing_id_returns_400(self):
        item = self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        non_existent_id = item.pk + 9999

        response = self.auth_client.post(
            self.url, {'cart_items_ids': [item.pk, non_existent_id]}, format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn(str(non_existent_id), response.data['detail'])

    def test_export_cannot_access_other_users_cart_items(self):
        other_item = self.make_cart_item(self.other_user, self.potato, self.unit_g, 200)

        response = self.auth_client.post(
            self.url, {'cart_items_ids': [other_item.pk]}, format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_export_groups_by_category(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        self.make_cart_item(self.user, self.carrot, self.unit_g, 150)

        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        text = response.content.decode()
        self.assertEqual(text.count(f'{self.cat_vegetables.name}:'), 1)
        self.assertIn(self.potato.name, text)
        self.assertIn(self.carrot.name, text)

    def test_export_multiple_categories(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 300)
        self.make_cart_item(self.user, self.milk, self.unit_g, 500)

        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        text = response.content.decode()
        self.assertIn(self.cat_vegetables.name, text)
        self.assertIn(self.cat_dairy.name, text)

    def test_export_has_content_disposition_header(self):
        self.make_cart_item(self.user, self.potato, self.unit_g, 200)
        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        self.assertIn('Content-Disposition', response)
        self.assertIn('shopping_list.txt', response['Content-Disposition'])

    def test_export_empty_cart_returns_empty_text(self):
        response = self.auth_client.post(f'{self.url}?all=true', {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.content.decode().strip(), '')
