from decimal import Decimal
from django.db import models
from django.contrib.auth.models import AbstractUser
from django.core.validators import MinValueValidator
from django.core.exceptions import ValidationError
from app.recipes.models import IngredientNutrition
from app.recipes.utils import convert_amount


class Allergy(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'allergy'
        verbose_name = 'Аллергия'
        verbose_name_plural = 'Аллергии'
        ordering = ['-id']

    def __str__(self):
        return self.name


class DietType(models.Model):
    name = models.CharField(max_length=255, unique=True)

    class Meta:
        db_table = 'diet_type'
        verbose_name = 'Тип питания'
        verbose_name_plural = 'Типы питания'
        ordering = ['-id']

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
    birth_date = models.DateField(null=True, blank=True, verbose_name="Дата рождения")
    gender = models.CharField(
        max_length=10,
        choices=[('male', 'Мужской'), ('female', 'Женский')],
        null=True,
        blank=True,
        verbose_name="Пол"
    )
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


class UserStock(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE, related_name='ingredients_in_stock')
    ingredient = models.ForeignKey('recipes.Ingredient', on_delete=models.CASCADE, related_name='in_users_stock')
    amount = models.DecimalField(max_digits=8, decimal_places=2, validators=[MinValueValidator(Decimal('0.01'))])
    unit = models.ForeignKey('recipes.Unit', on_delete=models.CASCADE)
    
    class Meta:
        db_table = 'user_stock'
        verbose_name = 'Ингредиент в наличии у пользователя'
        verbose_name_plural = 'Ингредиенты в наличии у пользователей'
        ordering = ['user', 'ingredient', 'amount']
        constraints = [
            models.UniqueConstraint(fields=['user', 'ingredient'], name='unique_user_ingredient_in_stock')
        ]
    
    def clean(self):
        super().clean()
        if not self.ingredient_id or not self.unit_id:
            return
        
        try:
            self.amount_in_base_units
        except (ValueError, ValidationError) as e:
            raise ValidationError({'unit': str(e)})
    
    def save(self, *args, **kwargs):
        self.full_clean()
        super().save(*args, **kwargs)
    
    @property
    def amount_in_base_units(self):
        if hasattr(self, '_in_base_units_cache'):
            return self._in_base_units_cache

        try:
            nutrition = self.ingredient.ingredient_nutrition
            base_unit = nutrition.base_unit
        except IngredientNutrition.DoesNotExist:
            raise ValidationError({
                'ingredient': f'Для ингредиента "{self.ingredient.name}" не указана пищевая ценность'
            })

        if self.unit_id == base_unit.pk:
            self._in_base_units_cache = self.amount
            return self._in_base_units_cache

        result = convert_amount(self.ingredient, self.amount, self.unit, base_unit)
        if result is None:
            raise ValueError(
                f'Невозможно конвертировать "{self.unit}" в "{base_unit}" '
                f'для ингредиента "{self.ingredient}"'
            )
        self._in_base_units_cache = result
        return self._in_base_units_cache
        
    def __str__(self):
        return f'Пользователь ID {self.user_id} имеет {self.amount} {self.unit.name} {self.ingredient.name}'
