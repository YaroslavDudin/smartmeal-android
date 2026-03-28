from app.recipes.models import Recipe
from rest_framework.test import APITestCase
from rest_framework import status
from django.contrib.auth import get_user_model
from app.accounts.models import UserFavorite


User = get_user_model()


class RecipeFavoriteAPITestCase(APITestCase):
    def setUp(self):
        self.user = User.objects.create_user(
            username='regular_dude',
            email='regular@tester.com',
            password='password123'
        )

        self.recipe = Recipe.objects.create(
            title='Сладкий рулет',
            cook_time=45,
            servings=2
        )

        self.client.force_authenticate(user=self.user)
        self.base_url = '/api/accounts/favorites/'

    def test_get_all_favorites(self):
        response_empty = self.client.get(self.base_url)
        self.assertEqual(response_empty.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response_empty.data), 0)
        
        favorite = UserFavorite.objects.create(user=self.user, recipe=self.recipe)

        response = self.client.get(self.base_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data), 1)
        self.assertEqual(response.data[0]['id'], favorite.pk)
        self.assertEqual(response.data[0]['recipe_title'], self.recipe.title)
        self.assertEqual(response.data[0]['recipe_image_url'], None)

    def test_favorite_add(self):
        response = self.client.post(self.base_url, {'recipe': self.recipe.pk}, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(UserFavorite.objects.count(), 1)
        
        favorite = UserFavorite.objects.get(pk=response.data['id'])
        self.assertEqual(favorite.user, self.user)
        self.assertEqual(favorite.recipe, self.recipe)

    def test_favorite_remove(self):
        favorite = UserFavorite.objects.create(user=self.user, recipe=self.recipe)
        self.assertEqual(UserFavorite.objects.count(), 1)

        favorite_url = f'{self.base_url}{favorite.pk}/'
        response = self.client.delete(favorite_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(UserFavorite.objects.filter(pk=favorite.pk).exists())

    def test_favorite_add_unauthenticated(self):
        self.client.force_authenticate(user=None)
        response = self.client.post(self.base_url, {'recipe_id': self.recipe.pk}, format='json')

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertEqual(UserFavorite.objects.count(), 0)

    def test_favorite_not_found(self):
        fake_url = f'{self.base_url}9999/'
        response = self.client.delete(fake_url)
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
