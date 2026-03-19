from django.db import models
from django.contrib.auth.models import AbstractUser


class Allergy(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'allergy'
        verbose_name = 'Аллергия'
        verbose_name_plural = 'Аллергии'
        ordering = ['name']

    def __str__(self):
        return self.name


class DietType(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'diet_type'
        verbose_name = 'Тип питания'
        verbose_name_plural = 'Типы питания'
        ordering = ['name']

    def __str__(self):
        return self.name


class CookTimeRange(models.TextChoices):
    SHORT = 'short', 'До 30 минут'
    MEDIUM = 'medium', 'От 30 до 60 минут'
    LONG = 'long', '60 минут и более'
    ANY = 'any', 'Любое время'


class User(AbstractUser):
    email = models.EmailField(unique=True, max_length=255)
    portion_size = models.IntegerField(default=1)
    created_at = models.DateTimeField(auto_now_add=True)
    
    allergies = models.ManyToManyField(Allergy, blank=True, related_name='users')
    diet_type = models.ForeignKey('DietType', null=True, blank=True, on_delete=models.SET_NULL, related_name='users')
    preferred_cook_time = models.CharField(
        max_length=10,
        choices=CookTimeRange.choices,
        default=CookTimeRange.ANY,
        verbose_name="Предпочитаемое время готовки"
    )

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['username']

    class Meta:
        db_table = 'user'
        verbose_name = 'Пользователь'
        verbose_name_plural = 'Пользователи'
        ordering = ['-created_at']

    def __str__(self):
        return f'Имя пользователя: {self.username}, email: {self.email}'


class UserFavorite(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='favorites')
    recipe = models.ForeignKey('recipes.Recipe', on_delete=models.CASCADE, related_name='favorited_by')
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'user_favorite'
        verbose_name = 'Избранный рецепт пользователя'
        verbose_name_plural = 'Избранные рецепты пользователей'
        ordering = ['-created_at']
        indexes = [
            models.Index(fields=['user', 'created_at']),
        ]
        constraints = [
            models.UniqueConstraint(fields=['user', 'recipe'], name='unique_user_favorite_recipe')
        ]

    def __str__(self):
        return f'User ID: {self.user_id} - Recipe ID: {self.recipe_id}'
