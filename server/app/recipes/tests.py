from django.test import TestCase
from app.recipes.models import Recipe
from rest_framework.test import APITestCase
from rest_framework import status
from django.contrib.auth import get_user_model
from app.accounts.models import UserFavorite

User = get_user_model()

class RecipesModelTests(TestCase):
    def test_create_recipe(self):
        recipe = Recipe.objects.create(
            title="Жареное мясо злокрыса",
            cook_time=10,
            servings=1
        )
        self.assertEqual(recipe.title, "Жареное мясо злокрыса")
        self.assertEqual(recipe.cook_time, 10)
        self.assertEqual(str(recipe), "Жареное мясо злокрыса")
# Create your tests here.

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

        self.url = f'/api/recipes/{self.recipe.id}/favorite/'

    def test_toggle_favorite_add(self):
        self.client.force_authenticate(user=self.user)

        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(UserFavorite.objects.count(), 1)
        
        favorite = UserFavorite.objects.first()
        self.assertEqual(favorite.user, self.user)
        self.assertEqual(favorite.recipe, self.recipe)

    def test_toggle_favorite_remove(self):
        self.client.force_authenticate(user=self.user)

        UserFavorite.objects.create(user=self.user, recipe=self.recipe)
        self.assertEqual(UserFavorite.objects.count(), 1)

        response = self.client.post(self.url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertEqual(UserFavorite.objects.count(), 0)

    def test_toggle_favorite_unauthenticated(self):
        response = self.client.post(self.url)

        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertEqual(UserFavorite.objects.count(), 0)

    def test_toggle_favorite_not_found(self):
        self.client.force_authenticate(user=self.user)

        fake_url = '/api/recipes/9999/favorite/'
        response = self.client.post(fake_url)
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

