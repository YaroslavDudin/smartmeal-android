from django.db import models
from django.contrib.auth.models import AbstractUser


class Allergy(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'allergy'
        ordering = ['name']

    def __str__(self):
        return self.name


class DietType(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'diet_type'
        ordering = ['name']

    def __str__(self):
        return self.name


class User(AbstractUser):
    email = models.EmailField(unique=True, max_length=255)
    portion_size = models.IntegerField(default=1)
    created_at = models.DateTimeField(auto_now_add=True)
    
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    diet_type = models.ForeignKey('DietType', null=True, blank=True, on_delete=models.SET_NULL, related_name='users')

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    class Meta:
        db_table = 'user'
        ordering = ['-created_at']

    def __str__(self):
        return f'Имя пользователя: {self.username}, email: {self.email}'


class UserFavorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='favorited_by')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'user_favorite'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'created_at']),
        ]
        constraints = [
            models.UniqueConstraint(fields=['user', 'recipe'], name='unique_user_favorite_recipe')
        ]

    def __str__(self):
        return f'User ID: {self.user_id} - Recipe ID: {self.recipe_id}'
